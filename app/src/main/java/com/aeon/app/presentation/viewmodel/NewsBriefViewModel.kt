package com.aeon.app.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeon.app.domain.ai.NewsArticle
import com.aeon.app.domain.ai.NewsBrief
import com.aeon.app.domain.ai.NewsCategory
import com.aeon.app.domain.ai.NewsRepository
import com.aeon.app.domain.ai.VerifyNewsFreshnessUseCase
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class NewsBriefUiState(
    val selectedCategory: NewsCategory = NewsCategory.Top,
    val categories: List<NewsCategory> = NewsCategory.entries,
    val brief: NewsBriefUi? = null,
    val articles: List<NewsArticleUi> = emptyList(),
    val freshnessLabel: String = "No news fetched yet",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class NewsBriefUi(
    val title: String,
    val summary: String,
    val freshnessLabel: String,
    val sourceCount: Int
)

@Immutable
data class NewsArticleUi(
    val id: String,
    val title: String,
    val description: String,
    val sourceName: String,
    val url: String?,
    val timeLabel: String,
    val isSaved: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class NewsBriefViewModel(private val repository: NewsRepository) : ViewModel() {
    private val availableCategories = NewsCategory.entries.filter { category ->
        category == NewsCategory.Custom || category in repository.selectedCategories()
    }.ifEmpty { listOf(NewsCategory.Top, NewsCategory.Custom) }
    private val selectedCategory = MutableStateFlow(
        NewsCategory.Top.takeIf(availableCategories::contains) ?: availableCategories.first()
    )
    private var refreshJob: Job? = null
    val uiState = MutableStateFlow(
        NewsBriefUiState(
            selectedCategory = selectedCategory.value,
            categories = availableCategories
        )
    )

    init {
        viewModelScope.launch {
            selectedCategory.flatMapLatest { category ->
                combine(
                    repository.observeArticles(category),
                    repository.observeLatestBrief(category)
                ) { articles, brief -> Triple(category, articles, brief) }
            }.collect { (category, articles, brief) ->
                uiState.update {
                    it.copy(
                        selectedCategory = category,
                        brief = brief?.toUi(),
                        articles = articles.map(NewsArticle::toUi),
                        freshnessLabel = brief?.freshnessLabel
                            ?: articles.maxOfOrNull(NewsArticle::fetchedAt)?.let(VerifyNewsFreshnessUseCase::label)
                            ?: "No news fetched yet"
                    )
                }
            }
        }
        refresh(force = false)
    }

    fun selectCategory(category: NewsCategory) {
        selectedCategory.value = category
        uiState.update { it.copy(selectedCategory = category, errorMessage = null) }
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        refreshJob?.cancel()
        uiState.update { it.copy(isLoading = true, errorMessage = null) }
        refreshJob = viewModelScope.launch {
            val result = repository.generateBrief(selectedCategory.value, force)
            val baseFreshness = result.brief?.freshnessLabel
                ?: result.fetch.fetchedAt?.let(VerifyNewsFreshnessUseCase::label)
                ?: if (result.fetch.articles.isEmpty()) "No news fetched yet" else uiState.value.freshnessLabel
            uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage,
                    freshnessLabel = if (result.fetch.isOffline) "Offline mode | $baseFreshness" else baseFreshness
                )
            }
        }
    }

    fun toggleSaved(articleId: String) {
        viewModelScope.launch { repository.toggleSaved(articleId) }
    }

    fun markRead(articleId: String) {
        viewModelScope.launch { repository.markRead(articleId) }
    }
}

private fun NewsBrief.toUi() = NewsBriefUi(
    title = title,
    summary = summary,
    freshnessLabel = freshnessLabel,
    sourceCount = articleIds.size.coerceAtMost(15)
)

private fun NewsArticle.toUi() = NewsArticleUi(
    id = id,
    title = title,
    description = description.orEmpty(),
    sourceName = sourceName,
    url = url,
    timeLabel = (publishedAt ?: fetchedAt).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM | h:mm a", Locale.getDefault())),
    isSaved = isSaved
)
