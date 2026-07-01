package com.aeon.app.domain.ai

import com.aeon.app.data.local.database.entities.AiModelModeStorage
import java.time.Instant

enum class AiModelMode(val storageValue: String, val label: String) {
    Auto(AiModelModeStorage.Auto, "Auto");

    companion object {
        fun fromStorage(value: String?): AiModelMode = Auto
    }
}

enum class AiPromptRole { User, Assistant, System }

enum class GroundingStatus {
    Ungrounded,
    LocalDataGrounded,
    NewsSourceGrounded,
    CachedGrounded,
    Unavailable
}

enum class NewsCategory(val storageValue: String, val label: String) {
    Top("top", "Top"),
    India("india", "India"),
    World("world", "World"),
    Technology("technology", "Tech"),
    Business("business", "Business"),
    Science("science", "Science"),
    Health("health", "Health"),
    Sports("sports", "Sports"),
    Entertainment("entertainment", "Entertainment"),
    Local("local", "Local"),
    Custom("custom", "Custom");

    companion object {
        fun fromStorage(value: String?): NewsCategory =
            entries.firstOrNull { it.storageValue == value } ?: Top
    }
}

enum class AiModelApi { OpenAiChat, BedrockConverse }

data class AiModel(
    val key: String,
    val displayName: String,
    val provider: String,
    val modelId: String,
    val role: String,
    val api: AiModelApi
)

data class AiPromptMessage(
    val role: AiPromptRole,
    val content: String
)

data class NewsArticle(
    val id: String,
    val title: String,
    val description: String?,
    val url: String?,
    val sourceName: String,
    val category: NewsCategory,
    val publishedAt: Instant?,
    val fetchedAt: Instant,
    val imageUrl: String?,
    val isRead: Boolean,
    val isSaved: Boolean
)

data class NewsBrief(
    val id: String,
    val category: NewsCategory,
    val title: String,
    val summary: String,
    val articleIds: List<String>,
    val modelId: String,
    val generatedAt: Instant,
    val freshnessLabel: String
)

data class AiRequestOptions(
    val systemPrompt: String,
    val maxTokens: Int = 1_200,
    val temperature: Double = 0.35
)

enum class AiRemoteError {
    NoNetwork,
    MissingKey,
    Unauthorized,
    ModelUnavailable,
    Timeout,
    ProviderUnavailable,
    InvalidResponse,
    CloudDisabled
}

sealed interface AiRemoteResult {
    data class Success(
        val content: String,
        val model: AiModel,
        val inputTokens: Int? = null,
        val outputTokens: Int? = null
    ) : AiRemoteResult

    data class Failure(val error: AiRemoteError) : AiRemoteResult
}

object BuildGroundedPromptUseCase {
    private const val NEWS_SYSTEM_PROMPT =
        "You are Aeon's news summarizer. Only use the provided articles. Do not invent facts. " +
            "If articles disagree, mention uncertainty. Include source names and publish times when available."

    fun news(
        userPrompt: String,
        articles: List<NewsArticle>,
        fetchedAt: Instant
    ): Pair<String, AiRequestOptions> {
        require(articles.isNotEmpty()) { "Grounded news requires at least one article." }
        val sourceText = articles.take(15).mapIndexed { index, article ->
            "${index + 1}. ${article.title}\nSource: ${article.sourceName}\n" +
                "Published: ${article.publishedAt ?: "unknown"}\n" +
                "Description: ${article.description.orEmpty()}"
        }.joinToString("\n\n")
        val prompt = """
            User request: $userPrompt
            Freshness: articles fetched at $fetchedAt.

            ARTICLES:
            $sourceText

            Output exactly these sections: Top headlines, Key points, Why it matters, Source list, Freshness note.
            Use at most 5 top headlines and 450 words total.
        """.trimIndent()
        return prompt to AiRequestOptions(
            systemPrompt = NEWS_SYSTEM_PROMPT,
            maxTokens = 700,
            temperature = 0.1
        )
    }
}

object VerifyNewsFreshnessUseCase {
    private const val FRESH_HOURS = 6L
    private const val CACHE_HOURS = 72L

    fun grounding(fetchedAt: Instant, now: Instant = Instant.now()): GroundingStatus = when {
        fetchedAt.isAfter(now.minusSeconds(FRESH_HOURS * 3600)) -> GroundingStatus.NewsSourceGrounded
        fetchedAt.isAfter(now.minusSeconds(CACHE_HOURS * 3600)) -> GroundingStatus.CachedGrounded
        else -> GroundingStatus.Unavailable
    }

    fun label(fetchedAt: Instant, now: Instant = Instant.now()): String {
        val minutes = java.time.Duration.between(fetchedAt, now).toMinutes().coerceAtLeast(0)
        return when {
            minutes < 2 -> "Updated just now"
            minutes < 60 -> "Updated $minutes minutes ago"
            minutes < FRESH_HOURS * 60 -> "Updated ${minutes / 60} hours ago"
            minutes < CACHE_HOURS * 60 -> "Cached ${minutes / 60} hours ago"
            else -> "Cache is out of date"
        }
    }
}
