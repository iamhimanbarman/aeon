package com.aeon.app.data.news

import com.aeon.app.data.local.database.entities.NewsArticleEntity
import com.aeon.app.data.local.database.entities.NewsSummaryEntity
import com.aeon.app.domain.ai.NewsArticle
import com.aeon.app.domain.ai.NewsBrief
import com.aeon.app.domain.ai.NewsCategory

fun NewsArticleEntity.toDomain() = NewsArticle(
    id = id,
    title = title,
    description = description,
    url = url,
    sourceName = sourceName,
    category = NewsCategory.fromStorage(category),
    publishedAt = publishedAt,
    fetchedAt = fetchedAt,
    imageUrl = imageUrl,
    isRead = isRead,
    isSaved = isSaved
)

fun NewsArticle.toEntity(existing: NewsArticleEntity? = null) = NewsArticleEntity(
    id = id,
    title = title,
    description = description,
    url = url,
    sourceName = sourceName,
    category = category.storageValue,
    publishedAt = publishedAt,
    fetchedAt = fetchedAt,
    imageUrl = imageUrl,
    contentSnippet = description,
    language = "en",
    country = "IN",
    isRead = existing?.isRead ?: isRead,
    isSaved = existing?.isSaved ?: isSaved
)

fun NewsSummaryEntity.toDomain() = NewsBrief(
    id = id,
    category = NewsCategory.fromStorage(category),
    title = title,
    summary = summary,
    articleIds = sourceArticleIds,
    modelId = generatedByModel,
    generatedAt = generatedAt,
    freshnessLabel = freshnessLabel
)
