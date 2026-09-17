package com.yuvraj.openchatai.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    val tools: List<ToolDefinition>? = null,
)

/**
 * Chat message sent to the API. `content` is either a JSON string (plain text)
 * or an array of content parts (text + image_url) for multimodal messages.
 */
@Serializable
data class ApiMessage(
    val role: String,
    val content: JsonElement? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallDto>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
) {
    companion object {
        fun text(role: String, text: String): ApiMessage =
            ApiMessage(role = role, content = JsonPrimitive(text))
    }
}

@Serializable
data class ToolCallDto(
    val id: String,
    val type: String = "function",
    val function: FunctionCallDto,
)

@Serializable
data class FunctionCallDto(
    val name: String,
    val arguments: String,
)

@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition,
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
data class ChatChoice(
    val delta: DeltaMessage? = null,
    val message: ResponseMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class DeltaMessage(
    val role: String? = null,
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    val reasoning: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallDelta>? = null,
)

@Serializable
data class ToolCallDelta(
    val index: Int? = null,
    val id: String? = null,
    val type: String? = null,
    val function: FunctionDelta? = null,
)

@Serializable
data class FunctionDelta(
    val name: String? = null,
    val arguments: String? = null,
)

@Serializable
data class ResponseMessage(
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    val reasoning: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallDto>? = null,
)

@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList(),
)

@Serializable
data class ModelInfo(
    val id: String,
)

/** Result of a successful model list fetch, including the base URL that worked. */
data class ModelsFetchResult(
    val baseUrl: String,
    val models: List<String>,
)

/** Events emitted while a chat completion is being produced. */
sealed interface StreamEvent {
    data class Delta(val text: String) : StreamEvent
    data class ReasoningDelta(val text: String) : StreamEvent
    data class ToolCallsCompleted(val calls: List<ToolCallDto>) : StreamEvent
    data class Failed(val message: String) : StreamEvent
    data object Finished : StreamEvent
}

/** The web_search tool definition offered to models when tool calling is enabled. */
val webSearchToolDefinition: ToolDefinition = ToolDefinition(
    function = FunctionDefinition(
        name = "web_search",
        description = "Live web search — your only source of current, real-time information. ALWAYS call this before answering questions about recent events, news, prices, releases, weather, sports, or any fact that may have changed after your training cutoff. Never claim you lack internet access; call this tool instead. You may call it multiple times with different queries to research a topic thoroughly. ALSO use it to identify things in attached images (characters, people, products, landmarks): describe the distinctive visual details you see as a search query — this works like a reverse image search. Results include extracted page content — read it and write a synthesized summary in your own words with inline source citations; never respond with only a list of links.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("query") {
                    put("type", "string")
                    put("description", "The search query to look up on the web")
                }
            }
            putJsonArray("required") { add(kotlinx.serialization.json.JsonPrimitive("query")) }
        },
    ),
)

/** The save_memory tool definition that lets the model store long-term facts about the user. */
val saveMemoryToolDefinition: ToolDefinition = ToolDefinition(
    function = FunctionDefinition(
        name = "save_memory",
        description = "Save a short, important fact about the user to long-term memory so it can be recalled in future conversations. Use when the user shares stable personal information such as their name, preferences, goals, profession, or explicitly asks you to remember something. Do not save trivial or temporary details.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("content") {
                    put("type", "string")
                    put("description", "The fact to remember, phrased as a short standalone sentence, e.g. 'The user prefers concise answers.'")
                }
            }
            putJsonArray("required") { add(kotlinx.serialization.json.JsonPrimitive("content")) }
        },
    ),
)
