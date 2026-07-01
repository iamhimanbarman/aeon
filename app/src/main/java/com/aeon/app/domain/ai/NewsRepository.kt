package com.aeon.app.domain.ai

import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class NewsFetchResult(
    val articles: List<NewsArticle>,
    val groundingStatus: GroundingStatus,
    val fetchedAt: Instant?,
    val errorMessage: String? = null,
    val isOffline: Boolean = false
)

data class NewsBriefResult(
    val brief: NewsBrief?,
    val fetch: NewsFetchResult,
    val errorMessage: String? = null
)

interface NewsRepository {
    fun observeArticles(category: NewsCategory): Flow<List<NewsArticle>>
    fun observeLatestBrief(category: NewsCategory): Flow<NewsBrief?>
    suspend fun fetchLatest(category: NewsCategory, force: Boolean = false): NewsFetchResult
    suspend fun generateBrief(category: NewsCategory, force: Boolean = false): NewsBriefResult
    suspend fun cached(category: NewsCategory): NewsFetchResult
    suspend fun saveBrief(
        category: NewsCategory,
        summary: String,
        articles: List<NewsArticle>,
        modelId: String,
        groundingStatus: GroundingStatus
    ): NewsBrief
    suspend fun latestBrief(category: NewsCategory): NewsBrief?
    suspend fun toggleSaved(articleId: String)
    suspend fun markRead(articleId: String)
    suspend fun clearCache()
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
    fun selectedCategories(): Set<NewsCategory>
    fun setSelectedCategories(categories: Set<NewsCategory>)
    fun customSources(): List<String>
    fun setCustomSources(urls: List<String>)
}

interface NewsSourceClient {
    suspend fun fetch(category: NewsCategory, customSources: List<String> = emptyList()): Result<List<NewsArticle>>
}
