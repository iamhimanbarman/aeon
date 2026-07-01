package com.aeon.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aeon.app.data.local.database.entities.NewsArticleEntity
import com.aeon.app.data.local.database.entities.NewsSummaryEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface NewsDao {
    @Upsert suspend fun upsertArticles(articles: List<NewsArticleEntity>)
    @Upsert suspend fun upsertSummary(summary: NewsSummaryEntity)

    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY COALESCE(published_at, fetched_at) DESC LIMIT :limit")
    fun observeArticles(category: String, limit: Int = 50): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY COALESCE(published_at, fetched_at) DESC LIMIT :limit")
    suspend fun getArticles(category: String, limit: Int = 30): List<NewsArticleEntity>

    @Query("SELECT * FROM news_articles WHERE category = :category AND fetched_at >= :since ORDER BY COALESCE(published_at, fetched_at) DESC LIMIT :limit")
    suspend fun getFreshArticles(category: String, since: Instant, limit: Int = 30): List<NewsArticleEntity>

    @Query("SELECT * FROM news_summaries WHERE category = :category ORDER BY generated_at DESC LIMIT 1")
    fun observeLatestSummary(category: String): Flow<NewsSummaryEntity?>

    @Query("SELECT * FROM news_summaries WHERE category = :category ORDER BY generated_at DESC LIMIT 1")
    suspend fun getLatestSummary(category: String): NewsSummaryEntity?

    @Query("UPDATE news_articles SET is_saved = CASE WHEN is_saved = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleSaved(id: String)

    @Query("UPDATE news_articles SET is_read = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("DELETE FROM news_summaries")
    suspend fun clearSummaries()

    @Query("DELETE FROM news_articles WHERE is_saved = 0")
    suspend fun clearUnsavedArticles()
}
