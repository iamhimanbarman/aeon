package com.aeon.app.data.ai

import android.net.Uri
import com.aeon.app.domain.ai.AiModel
import com.aeon.app.domain.ai.AiModelApi
import com.aeon.app.domain.ai.AiPromptMessage
import com.aeon.app.domain.ai.AiPromptRole
import com.aeon.app.domain.ai.AiRemoteError
import com.aeon.app.domain.ai.AiRemoteResult
import com.aeon.app.domain.ai.AiRequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

interface AiRemoteClient {
    suspend fun sendMessage(
        model: AiModel,
        messages: List<AiPromptMessage>,
        options: AiRequestOptions
    ): AiRemoteResult
}

class BedrockAiRemoteClient(
    private val keyProvider: AiKeyProvider,
    private val region: String
) : AiRemoteClient {
    override suspend fun sendMessage(
        model: AiModel,
        messages: List<AiPromptMessage>,
        options: AiRequestOptions
    ): AiRemoteResult = requestWithRetry {
        when (model.api) {
            AiModelApi.OpenAiChat -> openAiRequest(model, messages, options)
            AiModelApi.BedrockConverse -> converseRequest(model, messages, options)
        }
    }

    private suspend fun requestWithRetry(block: suspend () -> AiRemoteResult): AiRemoteResult {
        if (!keyProvider.directCloudAllowed) return AiRemoteResult.Failure(AiRemoteError.CloudDisabled)
        if (!keyProvider.hasKey()) return AiRemoteResult.Failure(AiRemoteError.MissingKey)
        val result = block()
        if (result is AiRemoteResult.Failure && result.error == AiRemoteError.ProviderUnavailable) {
            delay(450)
            return block()
        }
        return result
    }

    private suspend fun openAiRequest(
        model: AiModel,
        messages: List<AiPromptMessage>,
        options: AiRequestOptions
    ): AiRemoteResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("model", model.modelId)
            .put("max_tokens", options.maxTokens)
            .put("temperature", options.temperature)
            .put("stream", false)
            .put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", options.systemPrompt))
                messages.forEach { message ->
                    put(JSONObject().put("role", message.role.wireRole()).put("content", message.content))
                }
            })
        val connection = connection(
            "https://bedrock-runtime.$region.amazonaws.com/openai/v1/chat/completions"
        )
        try {
            connection.outputStream.bufferedWriter().use { it.write(payload.toString()) }
            val status = connection.responseCode
            if (status !in 200..299) return@withContext AiRemoteResult.Failure(status.toRemoteError())
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val content = json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
                .trim()
            if (content.isBlank()) return@withContext AiRemoteResult.Failure(AiRemoteError.InvalidResponse)
            val usage = json.optJSONObject("usage")
            AiRemoteResult.Success(
                content = content,
                model = model,
                inputTokens = usage?.optInt("prompt_tokens")?.takeIf { it > 0 },
                outputTokens = usage?.optInt("completion_tokens")?.takeIf { it > 0 }
            )
        } catch (_: SocketTimeoutException) {
            AiRemoteResult.Failure(AiRemoteError.Timeout)
        } catch (_: IOException) {
            AiRemoteResult.Failure(AiRemoteError.NoNetwork)
        } catch (_: Exception) {
            AiRemoteResult.Failure(AiRemoteError.InvalidResponse)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun converseRequest(
        model: AiModel,
        messages: List<AiPromptMessage>,
        options: AiRequestOptions
    ): AiRemoteResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("system", JSONArray().put(JSONObject().put("text", options.systemPrompt)))
            .put("messages", JSONArray().apply {
                messages.filter { it.role != AiPromptRole.System }.forEach { message ->
                    put(
                        JSONObject()
                            .put("role", message.role.wireRole())
                            .put("content", JSONArray().put(JSONObject().put("text", message.content)))
                    )
                }
            })
            .put(
                "inferenceConfig",
                JSONObject()
                    .put("maxTokens", options.maxTokens)
                    .put("temperature", options.temperature)
            )
        val encodedModel = Uri.encode(model.modelId)
        val connection = connection(
            "https://bedrock-runtime.$region.amazonaws.com/model/$encodedModel/converse"
        )
        try {
            connection.outputStream.bufferedWriter().use { it.write(payload.toString()) }
            val status = connection.responseCode
            if (status !in 200..299) return@withContext AiRemoteResult.Failure(status.toRemoteError())
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val content = json.optJSONObject("output")
                ?.optJSONObject("message")
                ?.optJSONArray("content")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()
                .trim()
            if (content.isBlank()) return@withContext AiRemoteResult.Failure(AiRemoteError.InvalidResponse)
            val usage = json.optJSONObject("usage")
            AiRemoteResult.Success(
                content = content,
                model = model,
                inputTokens = usage?.optInt("inputTokens")?.takeIf { it > 0 },
                outputTokens = usage?.optInt("outputTokens")?.takeIf { it > 0 }
            )
        } catch (_: SocketTimeoutException) {
            AiRemoteResult.Failure(AiRemoteError.Timeout)
        } catch (_: IOException) {
            AiRemoteResult.Failure(AiRemoteError.NoNetwork)
        } catch (_: Exception) {
            AiRemoteResult.Failure(AiRemoteError.InvalidResponse)
        } finally {
            connection.disconnect()
        }
    }

    private fun connection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${keyProvider.apiKey().orEmpty()}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            useCaches = false
        }
}

private fun AiPromptRole.wireRole(): String = when (this) {
    AiPromptRole.User -> "user"
    AiPromptRole.Assistant -> "assistant"
    AiPromptRole.System -> "system"
}

private fun Int.toRemoteError(): AiRemoteError = when (this) {
    401, 403 -> AiRemoteError.Unauthorized
    404 -> AiRemoteError.ModelUnavailable
    408, 504 -> AiRemoteError.Timeout
    429, in 500..599 -> AiRemoteError.ProviderUnavailable
    else -> AiRemoteError.InvalidResponse
}
