package com.yuvraj.openchatai.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenAIException(message: String) : Exception(message)

/**
 * Client for any OpenAI-compatible API (OpenAI, OpenRouter, Groq, Together,
 * Mistral, DeepSeek, local servers, ...). Handles model listing, streaming
 * chat completions (SSE) and tool-call accumulation.
 */
class OpenAIService {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 600_000
        }
    }

    /** Normalizes user input like "api.openai.com/v1/" into a usable base URL. */
    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim().removeSuffix("/")
        if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    /**
     * Fetches available model ids. Tries the URL as given, then with "/v1"
     * appended, so users can paste either form.
     */
    suspend fun fetchModels(rawBaseUrl: String, apiKey: String): ModelsFetchResult {
        val base = normalizeBaseUrl(rawBaseUrl)
        val candidates = buildList {
            add(base)
            if (!base.endsWith("/v1")) add("$base/v1")
        }
        var lastError = "Could not fetch models"
        for (candidate in candidates) {
            for (attempt in 0..MAX_RETRIES) {
                try {
                    val response = client.get("$candidate/models") {
                        if (apiKey.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $apiKey")
                    }
                    val body = response.bodyAsText()
                    if (response.status.isSuccess()) {
                        val parsed = json.decodeFromString<ModelsResponse>(body)
                        val ids = parsed.data.map { it.id }.distinct().sorted()
                        if (ids.isNotEmpty()) return ModelsFetchResult(candidate, ids)
                        lastError = "Provider returned an empty model list"
                        break
                    } else {
                        val code = response.status.value
                        lastError = friendlyHttpError(code, body)
                        if (code in RETRYABLE_STATUSES && attempt < MAX_RETRIES) {
                            delay(RETRY_BASE_DELAY_MS * (attempt + 1))
                            continue
                        }
                        break
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e.message ?: "Network error"
                    break
                }
            }
        }
        throw OpenAIException(lastError)
    }

    /**
     * Runs one chat completion round. Emits content deltas as they stream in,
     * a single ToolCallsCompleted once tool calls are fully accumulated, and
     * always ends with Finished.
     */
    fun chat(rawBaseUrl: String, apiKey: String, request: ChatRequest): Flow<StreamEvent> = channelFlow {
        val base = normalizeBaseUrl(rawBaseUrl)
        try {
            if (request.stream) {
                streamRequest(base, apiKey, request)
            } else {
                plainRequest(base, apiKey, request)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            send(StreamEvent.Failed(e.message ?: "Network error"))
        }
        send(StreamEvent.Finished)
    }

    private suspend fun ProducerScope<StreamEvent>.streamRequest(
        base: String,
        apiKey: String,
        request: ChatRequest,
    ) {
        var attempt = 0
        while (true) {
            var retryDelayMs = -1L
            client.preparePost("$base/chat/completions") {
                if (apiKey.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $apiKey")
                header(HttpHeaders.Accept, "text/event-stream")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(ChatRequest.serializer(), request))
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val code = response.status.value
                    if (code in RETRYABLE_STATUSES && attempt < MAX_RETRIES) {
                        retryDelayMs = RETRY_BASE_DELAY_MS * (attempt + 1)
                        return@execute
                    }
                    val body = response.bodyAsText()
                    send(StreamEvent.Failed(friendlyHttpError(code, body)))
                    return@execute
                }
                val channel = response.bodyAsChannel()
                val accumulators = LinkedHashMap<Int, ToolCallAccumulator>()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty() || payload == "[DONE]") continue
                    val chunk = runCatching {
                        json.decodeFromString<ChatResponse>(payload)
                    }.getOrNull() ?: continue
                    val choice = chunk.choices.firstOrNull() ?: continue
                    choice.delta?.content?.takeIf { it.isNotEmpty() }?.let {
                        send(StreamEvent.Delta(it))
                    }
                    (choice.delta?.reasoningContent ?: choice.delta?.reasoning)
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { send(StreamEvent.ReasoningDelta(it)) }
                    choice.delta?.toolCalls?.forEach { tc ->
                        val index = tc.index ?: 0
                        val acc = accumulators.getOrPut(index) { ToolCallAccumulator() }
                        tc.id?.let { if (it.isNotEmpty()) acc.id = it }
                        tc.function?.name?.let { acc.name += it }
                        tc.function?.arguments?.let { acc.arguments.append(it) }
                    }
                }
                val calls = accumulators.entries.mapNotNull { (index, acc) -> acc.toDto(index) }
                if (calls.isNotEmpty()) send(StreamEvent.ToolCallsCompleted(calls))
            }
            if (retryDelayMs < 0) return
            attempt++
            delay(retryDelayMs)
        }
    }

    private suspend fun ProducerScope<StreamEvent>.plainRequest(
        base: String,
        apiKey: String,
        request: ChatRequest,
    ) {
        var response = client.post("$base/chat/completions") {
            if (apiKey.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatRequest.serializer(), request))
        }
        var attempt = 0
        while (!response.status.isSuccess() && response.status.value in RETRYABLE_STATUSES && attempt < MAX_RETRIES) {
            attempt++
            delay(RETRY_BASE_DELAY_MS * attempt)
            response = client.post("$base/chat/completions") {
                if (apiKey.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(ChatRequest.serializer(), request))
            }
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            send(StreamEvent.Failed(friendlyHttpError(response.status.value, body)))
            return
        }
        val parsed = json.decodeFromString<ChatResponse>(body)
        val message = parsed.choices.firstOrNull()?.message
        (message?.reasoningContent ?: message?.reasoning)
            ?.takeIf { it.isNotEmpty() }
            ?.let { send(StreamEvent.ReasoningDelta(it)) }
        message?.content?.takeIf { it.isNotEmpty() }?.let { send(StreamEvent.Delta(it)) }
        message?.toolCalls?.takeIf { it.isNotEmpty() }?.let { send(StreamEvent.ToolCallsCompleted(it)) }
    }

    /** Maps HTTP failures to clear, actionable messages for the user. */
    private fun friendlyHttpError(code: Int, body: String): String {
        val providerMessage = extractErrorMessage(body)
        val hint = when (code) {
            401, 403 -> "Invalid or missing API key — check it in Providers."
            404 -> "Endpoint not found — check the base URL and selected model."
            429 -> "Rate limited — you're sending requests too fast or ran out of quota."
            500 -> "The provider had an internal error. Try again."
            502, 503, 504 -> "The provider's server is temporarily unavailable. Tried multiple times — please retry in a moment or switch models/providers."
            else -> null
        }
        return when {
            providerMessage != null && hint != null -> "$providerMessage (HTTP $code). $hint"
            providerMessage != null -> "$providerMessage (HTTP $code)"
            hint != null -> "HTTP $code — $hint"
            else -> "HTTP $code"
        }
    }

    private fun extractErrorMessage(body: String): String? {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val error = root["error"]
            when {
                error != null -> runCatching {
                    error.jsonObject["message"]?.jsonPrimitive?.content
                }.getOrNull() ?: runCatching { error.jsonPrimitive.content }.getOrNull()
                else -> root["message"]?.jsonPrimitive?.content
            }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        val RETRYABLE_STATUSES = setOf(429, 500, 502, 503, 504)
        const val MAX_RETRIES = 2
        const val RETRY_BASE_DELAY_MS = 1_200L
    }

    private class ToolCallAccumulator {
        var id: String = ""
        var name: String = ""
        val arguments: StringBuilder = StringBuilder()

        fun toDto(index: Int): ToolCallDto? {
            if (name.isEmpty()) return null
            val callId = id.ifEmpty { "call_$index" }
            return ToolCallDto(
                id = callId,
                function = FunctionCallDto(name = name, arguments = arguments.toString()),
            )
        }
    }
}
