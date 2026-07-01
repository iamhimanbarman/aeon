package com.aeon.app.data.repository

import com.aeon.app.data.ai.AiNetworkStatusProvider
import com.aeon.app.data.ai.AiSettingsPreferences
import com.aeon.app.data.local.database.dao.NewsDao
import com.aeon.app.data.local.database.entities.NewsSummaryEntity
import com.aeon.app.data.news.toDomain
import com.aeon.app.data.news.toEntity
import com.aeon.app.domain.ai.GroundingStatus
import com.aeon.app.domain.ai.NewsArticle
import com.aeon.app.domain.ai.NewsBrief
import com.aeon.app.domain.ai.NewsCategory
import com.aeon.app.domain.ai.NewsFetchResult
import com.aeon.app.domain.ai.NewsRepository
import com.aeon.app.domain.ai.NewsBriefResult
import com.aeon.app.domain.ai.NewsSourceClient
import com.aeon.app.domain.ai.VerifyNewsFreshnessUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class NewsRepositoryImpl(
    private val dao: NewsDao,
    private val sourceClient: NewsSourceClient,
    private val network: AiNetworkStatusProvider,
    private val preferences: AiSettingsPreferences
) : NewsRepository {
    override fun observeArticles(category: NewsCategory): Flow<List<NewsArticle>> =
        dao.observeArticles(category.storageValue).map { list -> list.map { it.toDomain() } }

    override fun observeLatestBrief(category: NewsCategory): Flow<NewsBrief?> =
        dao.observeLatestSummary(category.storageValue).map { it?.toDomain() }

    override suspend fun fetchLatest(category: NewsCategory, force: Boolean): NewsFetchResult {
        if (!isEnabled()) return cached(category).copy(errorMessage = "News fetching is turned off.")
        if (!network.isOnline()) return cached(category).copy(isOffline = true)
        if (!force) {
            val recent = dao.getFreshArticles(category.storageValue, Instant.now().minusSeconds(30 * 60))
            if (recent.isNotEmpty()) return recent.toFetchResult()
        }
        val fetched = sourceClient.fetch(category, customSources())
        if (fetched.isFailure || fetched.getOrNull().isNullOrEmpty()) {
            return cached(category).copy(errorMessage = "Live news could not be fetched.")
        }
        val existing = dao.getArticles(category.storageValue, 100).associateBy { it.id }
        val entities = fetched.getOrThrow().map { article -> article.toEntity(existing[article.id]) }
        dao.upsertArticles(entities)
        return entities.toFetchResult()
    }

    override suspend fun generateBrief(category: NewsCategory, force: Boolean): NewsBriefResult {
        val fetched = fetchLatest(category, force)
        return NewsBriefResult(
            brief = latestBrief(category),
            fetch = fetched,
            errorMessage = fetched.errorMessage ?: "AI summaries are unavailable in this build."
        )
    }

    override suspend fun cached(category: NewsCategory): NewsFetchResult =
        dao.getArticles(category.storageValue, 30).toFetchResult()

    override suspend fun saveBrief(
        category: NewsCategory,
        summary: String,
        articles: List<NewsArticle>,
        modelId: String,
        groundingStatus: GroundingStatus
    ): NewsBrief {
        val generatedAt = Instant.now()
        val sourceTime = articles.maxOfOrNull(NewsArticle::fetchedAt) ?: generatedAt
        val entity = NewsSummaryEntity(
            id = "news_brief_${UUID.randomUUID().toString().replace("-", "")}",
            category = category.storageValue,
            title = "${category.label} Daily Brief",
            summary = summary,
            sourceArticleIds = articles.map(NewsArticle::id),
            generatedByModel = modelId,
            generatedAt = generatedAt,
            freshnessLabel = when (groundingStatus) {
                GroundingStatus.CachedGrounded -> "Cached · ${VerifyNewsFreshnessUseCase.label(sourceTime)}"
                else -> VerifyNewsFreshnessUseCase.label(sourceTime)
            }
        )
        dao.upsertSummary(entity)
        return entity.toDomain()
    }

    override suspend fun latestBrief(category: NewsCategory): NewsBrief? = dao.getLatestSummary(category.storageValue)?.toDomain()
    override suspend fun toggleSaved(articleId: String) = dao.toggleSaved(articleId)
    override suspend fun markRead(articleId: String) = dao.markRead(articleId)

    override suspend fun clearCache() {
        dao.clearSummaries()
        dao.clearUnsavedArticles()
    }

    override fun isEnabled(): Boolean = preferences.newsEnabled
    override fun setEnabled(enabled: Boolean) { preferences.newsEnabled = enabled }
    override fun selectedCategories(): Set<NewsCategory> = preferences.newsCategories.map(NewsCategory::fromStorage).toSet()
    override fun setSelectedCategories(categories: Set<NewsCategory>) { preferences.newsCategories = categories.map(NewsCategory::storageValue).toSet() }
    override fun customSources(): List<String> = preferences.customNewsSources
    override fun setCustomSources(urls: List<String>) { preferences.customNewsSources = urls }

    private fun List<com.aeon.app.data.local.database.entities.NewsArticleEntity>.toFetchResult(): NewsFetchResult {
        val articles = map { it.toDomain() }
        val fetchedAt = maxOfOrNull { it.fetchedAt }
        val grounding = fetchedAt?.let(VerifyNewsFreshnessUseCase::grounding) ?: GroundingStatus.Unavailable
        return NewsFetchResult(articles, grounding, fetchedAt)
    }
}
