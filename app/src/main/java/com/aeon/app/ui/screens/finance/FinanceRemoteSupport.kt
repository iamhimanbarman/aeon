package com.aeon.app.ui.screens.finance

import com.aeon.app.BuildConfig
import com.aeon.app.data.local.database.entities.FinanceCategoryScopeStorage
import com.aeon.app.data.local.database.entities.FinanceCounterpartyEmailPreferenceStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

internal data class FinanceRemoteCounterpartyInput(
    val id: String? = null,
    val name: String,
    val email: String,
    val emailSharePreference: String = FinanceCounterpartyEmailPreferenceStorage.All
)

internal data class FinanceRemoteCounterpartyShareInput(
    val id: String? = null,
    val counterpartyId: String? = null,
    val counterpartyName: String,
    val counterpartyEmail: String,
    val direction: String,
    val purpose: String,
    val amount: String,
    val currency: String,
    val note: String? = null,
    val emailSharePreference: String = FinanceCounterpartyEmailPreferenceStorage.All,
    val occurredAt: String
)

internal data class FinanceRemoteCounterpartyManualEmailInput(
    val counterpartyId: String,
    val recordIds: List<String>,
    val message: String? = null
)

internal data class FinanceRemoteCounterpartyRecordStatusInput(
    val counterpartyId: String,
    val recordIds: List<String>,
    val status: String,
    val message: String? = null
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

    suspend fun shareCounterpartyRecord(
        accessToken: String,
        input: FinanceRemoteCounterpartyShareInput
    ): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("counterpartyName", input.counterpartyName)
            .put("counterpartyEmail", input.counterpartyEmail)
            .put("direction", input.direction)
            .put("purpose", input.purpose)
            .put("amount", input.amount)
            .put("currency", input.currency)
            .put("emailSharePreference", input.emailSharePreference)
            .put("occurredAt", input.occurredAt)

        input.note?.takeIf(String::isNotBlank)?.let { note ->
            payload.put("note", note)
        }

        executeJsonObject(
            Request.Builder()
                .url(buildUrl("/v1/finance/counterparty-share", emptyMap()))
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .post(
                    payload.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .build()
        )
    }

    suspend fun syncCounterparty(
        accessToken: String,
        input: FinanceRemoteCounterpartyInput
    ): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("name", input.name)
            .put("email", input.email)
            .put("emailSharePreference", input.emailSharePreference)

        input.id?.takeIf(String::isNotBlank)?.let { id ->
            payload.put("id", id)
        }

        executeJsonObject(
            Request.Builder()
                .url(buildUrl("/v1/finance/counterparties", emptyMap()))
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .post(
                    payload.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .build()
        )
    }

    suspend fun syncCounterpartyRecord(
        accessToken: String,
        input: FinanceRemoteCounterpartyShareInput
    ): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("counterpartyName", input.counterpartyName)
            .put("counterpartyEmail", input.counterpartyEmail)
            .put("direction", input.direction)
            .put("purpose", input.purpose)
            .put("amount", input.amount)
            .put("currency", input.currency)
            .put("emailSharePreference", input.emailSharePreference)
            .put("occurredAt", input.occurredAt)

        input.id?.takeIf(String::isNotBlank)?.let { id ->
            payload.put("id", id)
        }
        input.counterpartyId?.takeIf(String::isNotBlank)?.let { counterpartyId ->
            payload.put("counterpartyId", counterpartyId)
        }
        input.note?.takeIf(String::isNotBlank)?.let { note ->
            payload.put("note", note)
        }

        executeJsonObject(
            Request.Builder()
                .url(buildUrl("/v1/finance/counterparty-records", emptyMap()))
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .post(
                    payload.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .build()
        )
    }

    suspend fun sendCounterpartyRecordsEmail(
        accessToken: String,
        input: FinanceRemoteCounterpartyManualEmailInput
    ): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("counterpartyId", input.counterpartyId)
            .put("recordIds", JSONArray(input.recordIds))

        input.message?.takeIf(String::isNotBlank)?.let { message ->
            payload.put("message", message)
        }

        executeJsonObject(
            Request.Builder()
                .url(buildUrl("/v1/finance/counterparty-record-emails", emptyMap()))
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .post(
                    payload.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .build()
        )
    }

    suspend fun updateCounterpartyRecordStatus(
        accessToken: String,
        input: FinanceRemoteCounterpartyRecordStatusInput
    ): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("counterpartyId", input.counterpartyId)
            .put("recordIds", JSONArray(input.recordIds))
            .put("status", input.status)

        input.message?.takeIf(String::isNotBlank)?.let { message ->
            payload.put("message", message)
        }

        executeJsonObject(
            Request.Builder()
                .url(buildUrl("/v1/finance/counterparty-record-status", emptyMap()))
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .post(
                    payload.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .build()
        )
    }

    suspend fun deleteCounterpartyRecord(
        accessToken: String,
        recordId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        executeJsonObject(
            Request.Builder()
                .url(buildUrl("/v1/finance/counterparty-records/$recordId", emptyMap()))
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .delete()
                .build()
        )
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
            return parseJsonObject(response.body?.string().orEmpty(), response.isSuccessful, response.code)
        }
    }

    private fun executeJsonObject(
        request: Request
    ): JSONObject {
        client.newCall(request).execute().use { response ->
            return parseJsonObject(response.body?.string().orEmpty(), response.isSuccessful, response.code)
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

    private fun parseJsonObject(
        body: String,
        isSuccessful: Boolean,
        code: Int
    ): JSONObject {
        if (!isSuccessful) {
            error(parseErrorMessage(body, code))
        }

        return if (body.isBlank()) JSONObject() else JSONObject(body)
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
