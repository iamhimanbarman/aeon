package com.aeon.app.data.news

import android.text.Html
import android.util.Xml
import com.aeon.app.domain.ai.NewsArticle
import com.aeon.app.domain.ai.NewsCategory
import com.aeon.app.domain.ai.NewsSourceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RssNewsSourceClient : NewsSourceClient {
    override suspend fun fetch(category: NewsCategory, customSources: List<String>): Result<List<NewsArticle>> = runCatching {
        val fetchedAt = Instant.now()
        val feeds = if (category == NewsCategory.Custom) customSources else feedsFor(category) + customSources.take(2)
        require(feeds.isNotEmpty()) { "No news sources configured." }
        coroutineScope {
            feeds.distinct().take(4).map { feedUrl ->
                async(Dispatchers.IO) {
                    runCatching { fetchFeed(feedUrl, category, fetchedAt) }.getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
        }
            .distinctBy { normalizeTitle(it.title) }
            .sortedWith(
                compareByDescending<NewsArticle> { sourceReliability(it.sourceName) }
                    .thenByDescending { it.publishedAt ?: it.fetchedAt }
            )
            .take(30)
    }

    private suspend fun fetchFeed(url: String, category: NewsCategory, fetchedAt: Instant): List<NewsArticle> =
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "AeonPersonal/1.0 RSS Reader")
            try {
                if (connection.responseCode !in 200..299) return@withContext emptyList()
                connection.inputStream.buffered().use { input -> parse(input, category, fetchedAt) }
            } finally {
                connection.disconnect()
            }
        }

    private fun parse(input: java.io.InputStream, category: NewsCategory, fetchedAt: Instant): List<NewsArticle> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }
        val articles = mutableListOf<NewsArticle>()
        var inItem = false
        var title: String? = null
        var description: String? = null
        var link: String? = null
        var source: String? = null
        var published: Instant? = null
        var image: String? = null
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "item", "entry" -> {
                        inItem = true; title = null; description = null; link = null; source = null; published = null; image = null
                    }
                    "title" -> if (inItem) title = parser.nextText().cleanHtml()
                    "description", "summary" -> if (inItem) description = parser.nextText().cleanHtml().take(700)
                    "link" -> if (inItem) {
                        link = parser.getAttributeValue(null, "href") ?: runCatching { parser.nextText() }.getOrNull()
                    }
                    "source" -> if (inItem) source = runCatching { parser.nextText().cleanHtml() }.getOrNull()
                    "pubdate", "published", "updated" -> if (inItem) published = parseDate(runCatching { parser.nextText() }.getOrNull())
                    "media:content", "media:thumbnail", "content", "thumbnail", "enclosure" -> if (inItem) {
                        image = parser.getAttributeValue(null, "url") ?: image
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name.equals("item", true) || parser.name.equals("entry", true)) {
                    val cleanTitle = title.orEmpty().trim()
                    if (cleanTitle.isNotBlank()) {
                        val resolvedSource = source?.takeIf(String::isNotBlank)
                            ?: cleanTitle.substringAfterLast(" - ", "News source")
                        val displayTitle = if (cleanTitle.endsWith(" - $resolvedSource")) {
                            cleanTitle.removeSuffix(" - $resolvedSource").trim()
                        } else cleanTitle
                        articles += NewsArticle(
                            id = stableId(link ?: "$resolvedSource|$displayTitle"),
                            title = displayTitle,
                            description = description?.takeIf(String::isNotBlank),
                            url = link?.trim()?.takeIf(String::isNotBlank),
                            sourceName = resolvedSource,
                            category = category,
                            publishedAt = published,
                            fetchedAt = fetchedAt,
                            imageUrl = image,
                            isRead = false,
                            isSaved = false
                        )
                    }
                    inItem = false
                }
            }
            parser.next()
        }
        return articles
    }

    private fun feedsFor(category: NewsCategory): List<String> = when (category) {
        NewsCategory.Top -> listOf(googleTop(), "https://feeds.bbci.co.uk/news/rss.xml")
        NewsCategory.India -> listOf(googleSearch("India when:1d"))
        NewsCategory.World -> listOf(googleTopic("WORLD"), "https://feeds.bbci.co.uk/news/world/rss.xml")
        NewsCategory.Technology -> listOf(googleTopic("TECHNOLOGY"), "https://feeds.bbci.co.uk/news/technology/rss.xml")
        NewsCategory.Business -> listOf(googleTopic("BUSINESS"), "https://feeds.bbci.co.uk/news/business/rss.xml")
        NewsCategory.Science -> listOf(googleTopic("SCIENCE"), "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml")
        NewsCategory.Health -> listOf(googleTopic("HEALTH"), "https://feeds.bbci.co.uk/news/health/rss.xml")
        NewsCategory.Sports -> listOf(googleTopic("SPORTS"), "https://feeds.bbci.co.uk/sport/rss.xml")
        NewsCategory.Entertainment -> listOf(googleTopic("ENTERTAINMENT"), "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml")
        NewsCategory.Local -> listOf(googleSearch("India local news when:1d"))
        NewsCategory.Custom -> emptyList()
    }

    private fun googleTop() = "https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en"
    private fun googleTopic(topic: String) = "https://news.google.com/rss/headlines/section/topic/$topic?hl=en-IN&gl=IN&ceid=IN:en"
    private fun googleSearch(query: String) = "https://news.google.com/rss/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&hl=en-IN&gl=IN&ceid=IN:en"

    private fun String.cleanHtml(): String = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
        .replace(Regex("\\s+"), " ").trim()

    private fun parseDate(value: String?): Instant? = value?.let {
        runCatching { ZonedDateTime.parse(it.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
    }

    private fun stableId(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return "news_${digest.take(12).joinToString("") { byte -> "%02x".format(byte) }}"
    }

    private fun normalizeTitle(value: String) = value.lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ").trim()

    private fun sourceReliability(source: String): Int = when {
        source.contains("Reuters", true) -> 100
        source.contains("Associated Press", true) || source.equals("AP", true) -> 98
        source.contains("BBC", true) -> 96
        source.contains("The Hindu", true) || source.contains("Indian Express", true) -> 94
        source.contains("Press Trust", true) -> 94
        else -> 80
    }
}
