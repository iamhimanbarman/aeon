package com.aeon.app.domain.ai

import com.aeon.app.data.ai.AiNetworkStatusProvider
import com.aeon.app.data.ai.AiSettingsPreferences
import com.aeon.app.data.local.database.dao.NewsDao
import com.aeon.app.data.local.database.entities.NewsArticleEntity
import com.aeon.app.data.local.database.entities.NewsSummaryEntity
import com.aeon.app.data.repository.NewsRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NewsRepositoryTest {
    @Test
    fun `successful news fetch is persisted and grounded`() = runBlocking {
        val dao = TestNewsDao()
        val now = Instant.now()
        val article = newsArticle("verified", now)
        val repository = repository(dao, TestNewsSource(Result.success(listOf(article))))

        val result = repository.fetchLatest(NewsCategory.Top, force = true)

        assertEquals(listOf("verified"), result.articles.map(NewsArticle::id))
        assertEquals(GroundingStatus.NewsSourceGrounded, result.groundingStatus)
        assertEquals(1, dao.articles.size)
    }

    @Test
    fun `news fetch failure returns unavailable without fabricated articles`() = runBlocking {
        val repository = repository(TestNewsDao(), TestNewsSource(Result.failure(IllegalStateException("feed down"))))

        val result = repository.fetchLatest(NewsCategory.Top, force = true)

        assertTrue(result.articles.isEmpty())
        assertEquals(GroundingStatus.Unavailable, result.groundingStatus)
        assertEquals("Live news could not be fetched.", result.errorMessage)
    }

    @Test
    fun `news cache persists and is returned offline`() = runBlocking {
        val dao = TestNewsDao()
        val network = TestNewsNetwork(true)
        val now = Instant.now()
        val repository = repository(dao, TestNewsSource(Result.success(listOf(newsArticle("cached", now)))), network)
        repository.fetchLatest(NewsCategory.Top, force = true)

        network.online = false
        val cached = repository.fetchLatest(NewsCategory.Top, force = true)

        assertEquals(listOf("cached"), cached.articles.map(NewsArticle::id))
        assertEquals(GroundingStatus.NewsSourceGrounded, cached.groundingStatus)
    }

    private fun repository(
        dao: TestNewsDao,
        source: NewsSourceClient,
        network: TestNewsNetwork = TestNewsNetwork(true)
    ) = NewsRepositoryImpl(
        dao = dao,
        sourceClient = source,
        network = network,
        preferences = TestNewsPreferences()
    )

    private fun newsArticle(id: String, fetchedAt: Instant) = NewsArticle(
        id = id,
        title = "Verified headline $id",
        description = "Source-provided description",
        url = "https://example.com/$id",
        sourceName = "Example Wire",
        category = NewsCategory.Top,
        publishedAt = fetchedAt.minusSeconds(300),
        fetchedAt = fetchedAt,
        imageUrl = null,
        isRead = false,
        isSaved = false
    )
}

private class TestNewsSource(private val result: Result<List<NewsArticle>>) : NewsSourceClient {
    override suspend fun fetch(category: NewsCategory, customSources: List<String>): Result<List<NewsArticle>> = result
}

private class TestNewsNetwork(var online: Boolean) : AiNetworkStatusProvider {
    override fun isOnline(): Boolean = online
    override fun observeOnline(): Flow<Boolean> = flowOf(online)
}

private class TestNewsPreferences : AiSettingsPreferences {
    override var cloudEnabled = true
    override var defaultMode = AiModelMode.Auto.storageValue
    override var newsEnabled = true
    override var newsCategories = setOf(NewsCategory.Top.storageValue)
    override var customNewsSources = emptyList<String>()
}

private class TestNewsDao : NewsDao {
    val articles = mutableListOf<NewsArticleEntity>()
    private val summaries = mutableListOf<NewsSummaryEntity>()

    override suspend fun upsertArticles(articles: List<NewsArticleEntity>) {
        articles.forEach { article ->
            this.articles.removeAll { it.id == article.id }
            this.articles += article
        }
    }

    override suspend fun upsertSummary(summary: NewsSummaryEntity) {
        summaries.removeAll { it.id == summary.id }
        summaries += summary
    }

    override fun observeArticles(category: String, limit: Int): Flow<List<NewsArticleEntity>> =
        flowOf(filteredArticles(category, limit))

    override suspend fun getArticles(category: String, limit: Int): List<NewsArticleEntity> =
        filteredArticles(category, limit)

    override suspend fun getFreshArticles(category: String, since: Instant, limit: Int): List<NewsArticleEntity> =
        filteredArticles(category, limit).filter { it.fetchedAt >= since }

    override fun observeLatestSummary(category: String): Flow<NewsSummaryEntity?> =
        flowOf(summaries.filter { it.category == category }.maxByOrNull(NewsSummaryEntity::generatedAt))

    override suspend fun getLatestSummary(category: String): NewsSummaryEntity? =
        summaries.filter { it.category == category }.maxByOrNull(NewsSummaryEntity::generatedAt)

    override suspend fun toggleSaved(id: String) {
        replaceArticle(id) { it.copy(isSaved = !it.isSaved) }
    }

    override suspend fun markRead(id: String) {
        replaceArticle(id) { it.copy(isRead = true) }
    }

    override suspend fun clearSummaries() = summaries.clear()
    override suspend fun clearUnsavedArticles() { articles.removeAll { !it.isSaved } }

    private fun filteredArticles(category: String, limit: Int) = articles
        .filter { it.category == category }
        .sortedByDescending { it.publishedAt ?: it.fetchedAt }
        .take(limit)

    private fun replaceArticle(id: String, transform: (NewsArticleEntity) -> NewsArticleEntity) {
        val index = articles.indexOfFirst { it.id == id }
        if (index >= 0) articles[index] = transform(articles[index])
    }
}
