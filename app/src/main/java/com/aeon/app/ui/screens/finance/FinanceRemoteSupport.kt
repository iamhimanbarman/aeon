package com.aeon.app.ui.screens.finance

import com.aeon.app.BuildConfig
import com.aeon.app.data.local.database.entities.FinanceCategoryScopeStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class FinanceRemoteTransactionQuery(
    val transactionType: String,
    val category: String? = null,
    val day: String? = null,
    val month: String? = null,
    val from: String? = null,
    val to: String? = null,
    val limit: Int = 300
)

internal class FinanceRemoteClient(
    private val baseUrl: String = BuildConfig.AUTH_BASE_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = baseUrl.isNotBlank()

    suspend fun fetchExpenseCategories(
        accessToken: String
    ): List<FinanceCategoryOption> = withContext(Dispatchers.IO) {
        executeJsonArray(
            path = "/v1/finance/categories",
            accessToken = accessToken
        ).toFinanceCategoryOptions()
            .filter { option -> option.scope == FinanceCategoryScopeStorage.Expense }
    }

    suspend fun fetchExpenseTransactionMonths(
        accessToken: String,
        category: String? = null
    ): Set<YearMonth> = withContext(Dispatchers.IO) {
        val response = executeJsonObject(
            path = "/v1/finance/transaction-months",
            accessToken = accessToken,
            query = buildMap {
                put("transactionType", "expense")
                category?.takeIf(String::isNotBlank)?.let { put("category", it) }
            }
        )
        val months = response.optJSONArray("months") ?: JSONArray()
        buildSet {
            for (index in 0 until months.length()) {
                months.optString(index)
                    .takeIf { value -> value.isNotBlank() }
                    ?.let { value ->
                        runCatching { YearMonth.parse(value) }
                            .getOrNull()
                            ?.let(::add)
                    }
            }
        }
    }

    suspend fun fetchExpenseTransactions(
        accessToken: String,
        query: FinanceRemoteTransactionQuery
    ): List<FinanceTransactionEntity> = withContext(Dispatchers.IO) {
        executeJsonArray(
            path = "/v1/finance/transactions",
            accessToken = accessToken,
            query = buildMap {
                put("transactionType", query.transactionType)
                put("limit", query.limit.toString())
                query.category?.takeIf(String::isNotBlank)?.let { put("category", it) }
                query.day?.takeIf(String::isNotBlank)?.let { put("day", it) }
                query.month?.takeIf(String::isNotBlank)?.let { put("month", it) }
                query.from?.takeIf(String::isNotBlank)?.let { put("from", it) }
                query.to?.takeIf(String::isNotBlank)?.let { put("to", it) }
            }
        ).toFinanceTransactions()
    }

    private fun executeJsonObject(
        path: String,
        accessToken: String,
        query: Map<String, String> = emptyMap()
    ): JSONObject {
        client.newCall(
            Request.Builder()
                .url(buildUrl(path, query))
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .get()
                .build()
        ).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error(parseErrorMessage(body, response.code))
            }
            return if (body.isBlank()) JSONObject() else JSONObject(body)
        }
    }

    private fun executeJsonArray(
        path: String,
        accessToken: String,
        query: Map<String, String> = emptyMap()
    ): JSONArray {
        client.newCall(
            Request.Builder()
                .url(buildUrl(path, query))
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .get()
                .build()
        ).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error(parseErrorMessage(body, response.code))
            }
            return if (body.isBlank()) JSONArray() else JSONArray(body)
        }
    }

    private fun buildUrl(
        path: String,
        query: Map<String, String>
    ): String {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        require(normalizedBase.isNotBlank()) { "Finance backend URL is missing." }
        val parsedBase = "$normalizedBase/".toHttpUrlOrNull()
            ?: error("Invalid finance backend URL.")

        return parsedBase.newBuilder()
            .addPathSegments(path.trimStart('/'))
            .apply {
                query.forEach { (key, value) ->
                    addQueryParameter(key, value)
                }
            }
            .build()
            .toString()
    }

    private fun parseErrorMessage(
        body: String,
        code: Int
    ): String {
        return runCatching {
            val json = JSONObject(body)
            json.optJSONObject("error")
                ?.optString("message")
                ?.takeIf { message -> message.isNotBlank() }
        }.getOrNull() ?: when (code) {
            401 -> "Finance cloud session expired."
            else -> "Unable to load finance cloud data right now."
        }
    }
}

private fun JSONArray.toFinanceCategoryOptions(): List<FinanceCategoryOption> {
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                FinanceCategoryOption(
                    key = item.optString("id"),
                    label = item.optString("label"),
                    iconKey = item.optString("iconKey").ifBlank { "category" },
                    familyKey = item.optString("familyKey").ifBlank { "core" },
                    scope = item.optString("scope").ifBlank { FinanceCategoryScopeStorage.Expense },
                    isDefault = item.optBoolean("isDefault", false)
                )
            )
        }
    }.filter { option ->
        option.key.isNotBlank() && option.label.isNotBlank()
    }
}

private fun JSONArray.toFinanceTransactions(): List<FinanceTransactionEntity> {
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            item.toFinanceTransactionEntityOrNull()?.let(::add)
        }
    }
}

private fun JSONObject.toFinanceTransactionEntityOrNull(): FinanceTransactionEntity? {
    val id = optString("id").takeIf { value -> value.isNotBlank() } ?: return null
    val occurredAt = optString("occurredAt")
        .takeIf { value -> value.isNotBlank() }
        ?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
        ?: return null
    val createdAt = optString("createdAt")
        .takeIf { value -> value.isNotBlank() }
        ?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
        ?: occurredAt
    val updatedAt = optString("updatedAt")
        .takeIf { value -> value.isNotBlank() }
        ?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
        ?: createdAt

    return FinanceTransactionEntity(
        id = id,
        accountId = optString("accountId").takeIf { value -> value.isNotBlank() },
        transactionType = optString("transactionType").ifBlank { "expense" },
        title = optString("title").ifBlank { "Expense entry" },
        merchant = optString("merchant").takeIf { value -> value.isNotBlank() },
        category = optString("category").ifBlank { "general" },
        amount = optString("amount")
            .takeIf { value -> value.isNotBlank() }
            ?.toBigDecimalOrNull()
            ?: BigDecimal.ZERO,
        currency = optString("currency").ifBlank { "INR" },
        paymentMethod = optString("paymentMethod").takeIf { value -> value.isNotBlank() },
        note = optString("note").takeIf { value -> value.isNotBlank() },
        tags = optJSONArray("tags").toStringList(),
        receiptUri = optString("receiptUri").takeIf { value -> value.isNotBlank() },
        occurredAt = occurredAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = optString("deletedAt")
            .takeIf { value -> value.isNotBlank() }
            ?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
    )
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()

    return buildList {
        for (index in 0 until length()) {
            optString(index)
                .takeIf { value -> value.isNotBlank() }
                ?.let(::add)
        }
    }
}
