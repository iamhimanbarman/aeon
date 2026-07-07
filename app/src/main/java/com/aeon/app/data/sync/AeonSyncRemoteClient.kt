package com.aeon.app.data.sync

import android.net.Uri
import com.aeon.app.BuildConfig
import com.aeon.app.data.local.database.entities.AeonSyncOutboxEntity
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class AeonSyncPushResult(
    val acknowledgements: List<AeonSyncAcknowledgement>
)

data class AeonSyncPullResult(
    val cursor: Long,
    val hasMore: Boolean,
    val changes: List<AeonSyncPulledChange>
)

data class AeonSyncResolveResult(
    val serverRevision: Long?,
    val serverPayloadJson: String?,
    val serverDeletedAt: String?
)

data class AeonSyncAcknowledgement(
    val idempotencyKey: String,
    val entityType: String,
    val entityId: String,
    val status: String,
    val serverRevision: Long?,
    val serverPayloadJson: String?,
    val serverDeletedAt: String?
)

data class AeonSyncPulledChange(
    val revision: Long,
    val clientId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val deletedAt: String?
)

class AeonSyncRemoteException(
    message: String,
    val retryable: Boolean,
    val authFailure: Boolean = false,
    cause: Throwable? = null
) : Exception(message, cause)

class AeonSyncRemoteClient(
    private val baseUrl: String = BuildConfig.AUTH_BASE_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = baseUrl.isNotBlank()

    fun pushOutbox(
        accessToken: String,
        clientId: String,
        entries: List<AeonSyncOutboxEntity>
    ): AeonSyncPushResult {
        if (!isConfigured()) {
            throw AeonSyncRemoteException("Sync backend URL is missing.", retryable = false)
        }

        val request = Request.Builder()
            .url(url("/v1/sync/push"))
            .post(pushBody(clientId, entries).toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()

        return executePush(request)
    }

    fun pullChanges(
        accessToken: String,
        cursor: Long,
        limit: Int,
        entityTypes: List<String>
    ): AeonSyncPullResult {
        if (!isConfigured()) {
            throw AeonSyncRemoteException("Sync backend URL is missing.", retryable = false)
        }

        val entityTypesQuery = Uri.encode(entityTypes.joinToString(","))
        val request = Request.Builder()
            .url(url("/v1/sync/pull?cursor=$cursor&limit=$limit&entityTypes=$entityTypesQuery"))
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return executePull(request)
    }

    fun resolveConflict(
        accessToken: String,
        clientId: String,
        entityType: String,
        entityId: String,
        resolution: String,
        payloadJson: String?,
        baseRevision: Long?
    ): AeonSyncResolveResult {
        if (!isConfigured()) {
            throw AeonSyncRemoteException("Sync backend URL is missing.", retryable = false)
        }

        val body = JSONObject()
            .put("clientId", clientId)
            .put("entityType", entityType)
            .put("entityId", entityId)
            .put("resolution", resolution)
            .put("baseRevision", baseRevision ?: JSONObject.NULL)

        if (!payloadJson.isNullOrBlank()) {
            body.put("payload", payloadJson.asJsonObject())
            body.put("idempotencyKey", "resolve_${UUID.randomUUID().toString().replace("-", "")}")
        }

        val request = Request.Builder()
            .url(url("/v1/sync/resolve-conflict"))
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()

        return executeResolve(request)
    }

    private fun executePush(request: Request): AeonSyncPushResult {
        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    throw AeonSyncRemoteException(
                        message = parseErrorMessage(responseBody, response.code),
                        retryable = response.code == 429 || response.code >= 500,
                        authFailure = response.code == 401 || response.code == 403
                    )
                }

                return parsePushResponse(responseBody)
            }
        } catch (exception: AeonSyncRemoteException) {
            throw exception
        } catch (exception: UnknownHostException) {
            throw AeonSyncRemoteException("Network unavailable.", retryable = true, cause = exception)
        } catch (exception: SocketTimeoutException) {
            throw AeonSyncRemoteException("Sync request timed out.", retryable = true, cause = exception)
        } catch (exception: IOException) {
            throw AeonSyncRemoteException("Sync connection failed.", retryable = true, cause = exception)
        } catch (exception: JSONException) {
            throw AeonSyncRemoteException("Invalid sync server response.", retryable = true, cause = exception)
        } catch (exception: IllegalArgumentException) {
            throw AeonSyncRemoteException("Sync service unavailable.", retryable = false, cause = exception)
        }
    }

    private fun executePull(request: Request): AeonSyncPullResult {
        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    throw AeonSyncRemoteException(
                        message = parseErrorMessage(responseBody, response.code),
                        retryable = response.code == 429 || response.code >= 500,
                        authFailure = response.code == 401 || response.code == 403
                    )
                }

                return parsePullResponse(responseBody)
            }
        } catch (exception: AeonSyncRemoteException) {
            throw exception
        } catch (exception: UnknownHostException) {
            throw AeonSyncRemoteException("Network unavailable.", retryable = true, cause = exception)
        } catch (exception: SocketTimeoutException) {
            throw AeonSyncRemoteException("Sync request timed out.", retryable = true, cause = exception)
        } catch (exception: IOException) {
            throw AeonSyncRemoteException("Sync connection failed.", retryable = true, cause = exception)
        } catch (exception: JSONException) {
            throw AeonSyncRemoteException("Invalid sync server response.", retryable = true, cause = exception)
        } catch (exception: IllegalArgumentException) {
            throw AeonSyncRemoteException("Sync service unavailable.", retryable = false, cause = exception)
        }
    }

    private fun executeResolve(request: Request): AeonSyncResolveResult {
        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    throw AeonSyncRemoteException(
                        message = parseErrorMessage(responseBody, response.code),
                        retryable = response.code == 429 || response.code >= 500,
                        authFailure = response.code == 401 || response.code == 403
                    )
                }

                return parseResolveResponse(responseBody)
            }
        } catch (exception: AeonSyncRemoteException) {
            throw exception
        } catch (exception: UnknownHostException) {
            throw AeonSyncRemoteException("Network unavailable.", retryable = true, cause = exception)
        } catch (exception: SocketTimeoutException) {
            throw AeonSyncRemoteException("Sync request timed out.", retryable = true, cause = exception)
        } catch (exception: IOException) {
            throw AeonSyncRemoteException("Sync connection failed.", retryable = true, cause = exception)
        } catch (exception: JSONException) {
            throw AeonSyncRemoteException("Invalid sync server response.", retryable = true, cause = exception)
        } catch (exception: IllegalArgumentException) {
            throw AeonSyncRemoteException("Sync service unavailable.", retryable = false, cause = exception)
        }
    }

    private fun pushBody(
        clientId: String,
        entries: List<AeonSyncOutboxEntity>
    ): JSONObject {
        val changes = JSONArray()
        entries.forEach { entry ->
            changes.put(
                JSONObject()
                    .put("idempotencyKey", entry.idempotencyKey)
                    .put("entityType", entry.entityType)
                    .put("entityId", entry.entityId)
                    .put("operation", entry.operation)
                    .put("payload", entry.payloadJson.asJsonObject())
                    .put("baseRevision", entry.baseRevision ?: JSONObject.NULL)
            )
        }

        return JSONObject()
            .put("clientId", clientId)
            .put("changes", changes)
    }

    private fun parsePushResponse(body: String): AeonSyncPushResult {
        val root = if (body.isBlank()) JSONObject() else JSONObject(body)
        val items = root.optJSONArray("acknowledgements") ?: JSONArray()
        val acknowledgements = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    AeonSyncAcknowledgement(
                        idempotencyKey = item.optString("idempotencyKey"),
                        entityType = item.optString("entityType"),
                        entityId = item.optString("entityId"),
                        status = item.optString("status"),
                        serverRevision = item.optNullableLong("serverRevision"),
                        serverPayloadJson = item.optJSONObject("serverPayload")?.toString(),
                        serverDeletedAt = item.optNullableString("serverDeletedAt")
                    )
                )
            }
        }

        return AeonSyncPushResult(acknowledgements = acknowledgements)
    }

    private fun parsePullResponse(body: String): AeonSyncPullResult {
        val root = if (body.isBlank()) JSONObject() else JSONObject(body)
        val items = root.optJSONArray("changes") ?: JSONArray()
        val changes = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    AeonSyncPulledChange(
                        revision = item.optLong("revision"),
                        clientId = item.optString("clientId"),
                        entityType = item.optString("entityType"),
                        entityId = item.optString("entityId"),
                        operation = item.optString("operation"),
                        payloadJson = item.optJSONObject("payload")?.toString().orEmpty(),
                        deletedAt = item.optString("deletedAt").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
        }

        return AeonSyncPullResult(
            cursor = root.optLong("cursor"),
            hasMore = root.optBoolean("hasMore", false),
            changes = changes
        )
    }

    private fun parseResolveResponse(body: String): AeonSyncResolveResult {
        val root = if (body.isBlank()) JSONObject() else JSONObject(body)
        val acknowledgement = root.optJSONObject("acknowledgement")

        return AeonSyncResolveResult(
            serverRevision = acknowledgement?.optNullableLong("serverRevision")
                ?: root.optNullableLong("serverRevision"),
            serverPayloadJson = root.optJSONObject("serverPayload")?.toString(),
            serverDeletedAt = root.optNullableString("serverDeletedAt")
        )
    }

    private fun parseErrorMessage(
        body: String,
        code: Int
    ): String {
        return runCatching {
            JSONObject(body)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: when (code) {
            401, 403 -> "Sync authentication failed."
            429 -> "Sync rate limit reached."
            in 500..599 -> "Sync service is temporarily unavailable."
            else -> "Sync request was rejected."
        }
    }

    private fun url(path: String): String {
        return "${baseUrl.trim().trimEnd('/')}$path"
    }

    private fun String.asJsonObject(): JSONObject {
        return runCatching {
            JSONObject(ifBlank { "{}" })
        }.getOrElse {
            JSONObject()
        }
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        return if (!has(key) || isNull(key)) null else optLong(key)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (!has(key) || isNull(key)) {
            null
        } else {
            optString(key).takeIf { it.isNotBlank() && it != "null" }
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
