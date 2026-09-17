package com.yuvraj.openchatai.data.model

import kotlinx.serialization.Serializable

/** An OpenAI-compatible API provider configured by the user. */
@Serializable
data class Provider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val models: List<String> = emptyList(),
    val selectedModel: String? = null,
)

/** A web source cited by the assistant after a search tool call. */
@Serializable
data class SearchSource(
    val title: String,
    val url: String,
)

/** Kind of user-attached file. */
object AttachmentKind {
    const val IMAGE = "image"
    const val PDF = "pdf"
    const val TEXT = "text"
}

/**
 * A file attached to a user message. Images carry base64 JPEG data sent to
 * vision models; PDFs and text documents carry extracted text.
 */
@Serializable
data class Attachment(
    val id: String,
    val name: String,
    val kind: String,
    val mimeType: String = "",
    val base64Data: String? = null,
    val textContent: String? = null,
    val sizeBytes: Long = 0,
)

/**
 * A compact record of one tool call made while producing an assistant message.
 * Replayed into API history on follow-up turns so the model knows which
 * searches were already performed and never repeats them.
 */
@Serializable
data class ToolExchange(
    val callId: String,
    val name: String,
    val arguments: String,
    val result: String,
)

/** A single persisted chat message. */
@Serializable
data class StoredMessage(
    val id: String,
    val role: String,
    val content: String,
    val reasoning: String? = null,
    val sources: List<SearchSource> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val toolExchanges: List<ToolExchange> = emptyList(),
    val model: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)

/** A persisted conversation with its full message history. */
@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val providerId: String? = null,
    val model: String? = null,
    val messages: List<StoredMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * A long-term memory about the user, persisted across all conversations.
 * Saved either automatically by the AI (via the save_memory tool) or manually.
 */
@Serializable
data class Memory(
    val id: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = SOURCE_AI,
) {
    companion object {
        const val SOURCE_AI = "ai"
        const val SOURCE_USER = "user"
    }
}

/** A named, reusable system prompt. */
@Serializable
data class PromptPreset(
    val id: String,
    val name: String,
    val prompt: String,
    val isBuiltIn: Boolean = false,
)

/** Built-in system prompt presets always available in Settings. */
val builtInPromptPresets: List<PromptPreset> = listOf(
    PromptPreset(
        id = "builtin_coder",
        name = "Coder",
        prompt = "You are an expert software engineer. Give correct, idiomatic code with brief explanations. " +
            "Prefer complete, runnable examples. Point out pitfalls and edge cases. Be direct and concise.",
        isBuiltIn = true,
    ),
    PromptPreset(
        id = "builtin_writer",
        name = "Writer",
        prompt = "You are a skilled writing assistant. Help draft, edit, and polish text with a natural, engaging " +
            "style. Match the user's tone, fix grammar quietly, and offer stronger phrasing when useful.",
        isBuiltIn = true,
    ),
    PromptPreset(
        id = "builtin_translator",
        name = "Translator",
        prompt = "You are a professional translator. Detect the source language and translate accurately while " +
            "preserving tone, idioms, and formatting. When asked, explain nuances briefly.",
        isBuiltIn = true,
    ),
    PromptPreset(
        id = "builtin_concise",
        name = "Concise",
        prompt = "Answer as briefly as possible while staying accurate and complete. No filler, no preamble, " +
            "no repetition of the question. Use short bullet points where they help.",
        isBuiltIn = true,
    ),
    PromptPreset(
        id = "builtin_teacher",
        name = "Teacher",
        prompt = "You are a patient teacher. Explain concepts step by step with simple language and concrete " +
            "examples or analogies. Check understanding and build up from fundamentals.",
        isBuiltIn = true,
    ),
)

/** Global app settings persisted across sessions. */
@Serializable
data class AppSettings(
    val activeProviderId: String? = null,
    val toolCallingEnabled: Boolean = true,
    val webSearchEnabled: Boolean = true,
    val streamingEnabled: Boolean = true,
    val memoryEnabled: Boolean = true,
    val systemPrompt: String = "",
    val activePresetId: String? = null,
    val temperature: Float = 0.7f,
    val useCustomTemperature: Boolean = false,
)
