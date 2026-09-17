package com.yuvraj.openchatai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yuvraj.openchatai.data.attachments.AttachmentProcessor
import com.yuvraj.openchatai.data.model.AppSettings
import com.yuvraj.openchatai.data.model.Attachment
import com.yuvraj.openchatai.data.model.AttachmentKind
import com.yuvraj.openchatai.data.model.Conversation
import com.yuvraj.openchatai.data.model.Memory
import com.yuvraj.openchatai.data.model.PromptPreset
import com.yuvraj.openchatai.data.model.Provider
import com.yuvraj.openchatai.data.model.builtInPromptPresets
import com.yuvraj.openchatai.data.model.SearchSource
import com.yuvraj.openchatai.data.model.StoredMessage
import com.yuvraj.openchatai.data.model.ToolExchange
import com.yuvraj.openchatai.data.network.ApiMessage
import com.yuvraj.openchatai.data.network.FunctionCallDto
import com.yuvraj.openchatai.data.network.ChatRequest
import com.yuvraj.openchatai.data.network.ModelsFetchResult
import com.yuvraj.openchatai.data.network.OpenAIService
import com.yuvraj.openchatai.data.network.StreamEvent
import com.yuvraj.openchatai.data.network.ToolCallDto
import com.yuvraj.openchatai.data.network.WebSearchService
import com.yuvraj.openchatai.data.network.saveMemoryToolDefinition
import com.yuvraj.openchatai.data.network.webSearchToolDefinition
import com.yuvraj.openchatai.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.UUID

/**
 * Single source of truth for providers, settings, conversations and the
 * generation pipeline (streaming + web-search tool-call loop).
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val api = OpenAIService()
    private val webSearch = WebSearchService()
    private val attachmentProcessor = AttachmentProcessor(application)
    private val json = Json { ignoreUnknownKeys = true }

    private val _providers = MutableStateFlow<List<Provider>>(repository.loadProviders())
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private val _settings = MutableStateFlow<AppSettings>(repository.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(repository.loadConversations())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _memories = MutableStateFlow<List<Memory>>(repository.loadMemories())
    val memories: StateFlow<List<Memory>> = _memories.asStateFlow()

    private val _customPresets = MutableStateFlow<List<PromptPreset>>(repository.loadPromptPresets())
    val customPresets: StateFlow<List<PromptPreset>> = _customPresets.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _isGenerating = MutableStateFlow<Boolean>(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationStatus = MutableStateFlow<String?>(null)
    val generationStatus: StateFlow<String?> = _generationStatus.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<Attachment>>(emptyList())
    val pendingAttachments: StateFlow<List<Attachment>> = _pendingAttachments.asStateFlow()

    private val _isProcessingAttachments = MutableStateFlow<Boolean>(false)
    val isProcessingAttachments: StateFlow<Boolean> = _isProcessingAttachments.asStateFlow()

    private val _attachmentError = MutableStateFlow<String?>(null)
    val attachmentError: StateFlow<String?> = _attachmentError.asStateFlow()

    private var generationJob: Job? = null

    fun activeProvider(): Provider? {
        val list = _providers.value
        return list.find { it.id == _settings.value.activeProviderId } ?: list.firstOrNull()
    }

    fun activeModel(): String? {
        val provider = activeProvider() ?: return null
        return provider.selectedModel ?: provider.models.firstOrNull()
    }

    // region Conversations

    fun newChat() {
        stopGeneration()
        _currentConversationId.value = null
    }

    fun selectConversation(id: String) {
        stopGeneration()
        _currentConversationId.value = id
    }

    fun deleteConversation(id: String) {
        if (_currentConversationId.value == id) {
            stopGeneration()
            _currentConversationId.value = null
        }
        _conversations.update { list -> list.filterNot { it.id == id } }
        persistConversations()
    }

    fun clearAllConversations() {
        stopGeneration()
        _currentConversationId.value = null
        _conversations.value = emptyList()
        persistConversations()
    }

    // endregion

    // region Providers

    fun saveProvider(provider: Provider) {
        _providers.update { list ->
            val exists = list.any { it.id == provider.id }
            if (exists) list.map { if (it.id == provider.id) provider else it } else list + provider
        }
        if (_settings.value.activeProviderId == null) {
            updateSettings { it.copy(activeProviderId = provider.id) }
        }
        repository.saveProviders(_providers.value)
    }

    fun deleteProvider(id: String) {
        _providers.update { list -> list.filterNot { it.id == id } }
        if (_settings.value.activeProviderId == id) {
            updateSettings { it.copy(activeProviderId = _providers.value.firstOrNull()?.id) }
        }
        repository.saveProviders(_providers.value)
    }

    fun setActiveProvider(id: String) {
        updateSettings { it.copy(activeProviderId = id) }
    }

    /** Selects a model on the active provider, adding it to the list if custom. */
    fun selectModel(model: String) {
        val provider = activeProvider() ?: return
        val updated = provider.copy(
            selectedModel = model,
            models = if (provider.models.contains(model)) provider.models else provider.models + model,
        )
        saveProvider(updated)
    }

    fun fetchModels(baseUrl: String, apiKey: String, onResult: (Result<ModelsFetchResult>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { api.fetchModels(baseUrl, apiKey) }
            onResult(result)
        }
    }

    // endregion

    // region Settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.update(transform)
        repository.saveSettings(_settings.value)
    }

    // region Prompt presets

    /** All presets shown in Settings: built-ins first, then user-saved ones. */
    fun allPresets(): List<PromptPreset> = builtInPromptPresets + _customPresets.value

    /** Applies a preset as the active system prompt. */
    fun applyPreset(preset: PromptPreset) {
        updateSettings { it.copy(systemPrompt = preset.prompt, activePresetId = preset.id) }
    }

    /** Clears the system prompt and active preset. */
    fun clearSystemPrompt() {
        updateSettings { it.copy(systemPrompt = "", activePresetId = null) }
    }

    /** Saves the current system prompt as a named custom preset. Returns false for blank input. */
    fun saveCurrentPromptAsPreset(name: String): Boolean {
        val trimmedName = name.trim()
        val prompt = _settings.value.systemPrompt.trim()
        if (trimmedName.isEmpty() || prompt.isEmpty()) return false
        val preset = PromptPreset(id = UUID.randomUUID().toString(), name = trimmedName, prompt = prompt)
        _customPresets.update { it + preset }
        repository.savePromptPresets(_customPresets.value)
        updateSettings { it.copy(activePresetId = preset.id) }
        return true
    }

    fun deletePreset(id: String) {
        _customPresets.update { list -> list.filterNot { it.id == id } }
        repository.savePromptPresets(_customPresets.value)
        if (_settings.value.activePresetId == id) {
            updateSettings { it.copy(activePresetId = null) }
        }
    }

    // endregion

    // region Memories

    /** Adds a memory manually from settings. Returns false for blank/duplicate input. */
    fun addMemory(content: String): Boolean {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return false
        if (_memories.value.any { it.content.equals(trimmed, ignoreCase = true) }) return false
        _memories.update { it + Memory(id = UUID.randomUUID().toString(), content = trimmed, source = Memory.SOURCE_USER) }
        repository.saveMemories(_memories.value)
        return true
    }

    fun deleteMemory(id: String) {
        _memories.update { list -> list.filterNot { it.id == id } }
        repository.saveMemories(_memories.value)
    }

    fun clearAllMemories() {
        _memories.value = emptyList()
        repository.saveMemories(_memories.value)
    }

    /** Stores a memory saved by the AI via the save_memory tool. */
    private fun saveAiMemory(content: String): Boolean {
        val trimmed = content.trim().take(MAX_MEMORY_LENGTH)
        if (trimmed.isEmpty()) return false
        if (_memories.value.any { it.content.equals(trimmed, ignoreCase = true) }) return true
        _memories.update { list ->
            (list + Memory(id = UUID.randomUUID().toString(), content = trimmed)).takeLast(MAX_MEMORIES)
        }
        repository.saveMemories(_memories.value)
        return true
    }

    /** Builds the system prompt including saved memories when memory is enabled. */
    private fun buildSystemPrompt(settings: AppSettings): String {
        val parts = mutableListOf<String>()
        if (settings.systemPrompt.isNotBlank()) parts += settings.systemPrompt.trim()
        parts += "MATH FORMATTING: this chat interface cannot render LaTeX. Write all mathematical " +
            "expressions in plain Unicode text — e.g. x², √2, 3/4, ×, ÷, ±, ≤, ≥, ≠, π, Δ, ∑, ∫. " +
            "Never use LaTeX delimiters such as \\( \\), \\[ \\], $ $ or $$ $$, and never emit " +
            "commands like \\frac, \\sqrt or \\times. For complex derivations use fenced code blocks " +
            "with plain-text math instead."
        if (settings.toolCallingEnabled && settings.webSearchEnabled) {
            val today = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.US)
                .format(java.util.Date())
            parts += "You have LIVE INTERNET ACCESS through the web_search tool. Today's date is $today — " +
                "your training data is outdated, so you MUST call web_search before answering anything " +
                "involving current events, news, prices, scores, releases, versions, weather, people in " +
                "the news, or any fact that could have changed since your training. Never say you cannot " +
                "browse the internet or that your knowledge has a cutoff — search instead. If you are " +
                "unsure whether information is current, search. For deep questions you may search more " +
                "than once with different queries to explore the topic. " +
                "IMAGE IDENTIFICATION: when the user attaches an image and asks who/what it is (a person, " +
                "character, product, place, artwork, plant, etc.), NEVER give up with 'I cannot identify " +
                "this'. Instead, act like a reverse image search: extract the most distinctive visible " +
                "details (text, logos, art style, hair/eye colors, clothing, unique features) and call " +
                "web_search with descriptive queries built from them, e.g. 'anime girl white hair blue " +
                "eyes red ribbon character name'. Try 2-3 differently-phrased queries if the first is " +
                "inconclusive, then combine what you see with the search results to name the best match " +
                "(state your confidence). " +
                "After searching, read the returned page content and write a clear, synthesized answer " +
                "in your own words that directly addresses the user's question — like a research " +
                "summary. Never reply with just a list of links or URLs. Cite sources inline in " +
                "markdown like [title](url) where relevant. Search ONLY for what the user's latest " +
                "message needs, and never repeat a search that already appears earlier in this " +
                "conversation's tool history — reuse those earlier results instead."
        }
        if (settings.memoryEnabled && _memories.value.isNotEmpty()) {
            parts += buildString {
                append("Long-term memories about the user (saved in previous conversations):\n")
                _memories.value.forEach { append("- ").append(it.content).append('\n') }
            }.trimEnd()
        }
        return parts.joinToString("\n\n")
    }

    // endregion

    // region Attachments

    /** Processes picked files (images, PDFs, documents) into pending attachments. */
    fun addAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isProcessingAttachments.value = true
            _attachmentError.value = null
            val errors = mutableListOf<String>()
            for (uri in uris) {
                if (_pendingAttachments.value.size >= MAX_ATTACHMENTS) {
                    errors += "You can attach up to $MAX_ATTACHMENTS files per message."
                    break
                }
                runCatching { attachmentProcessor.process(uri) }
                    .onSuccess { attachment -> _pendingAttachments.update { it + attachment } }
                    .onFailure { e -> errors += (e.message ?: "Could not read one of the files.") }
            }
            if (errors.isNotEmpty()) _attachmentError.value = errors.first()
            _isProcessingAttachments.value = false
        }
    }

    fun removeAttachment(id: String) {
        _pendingAttachments.update { list -> list.filterNot { it.id == id } }
    }

    fun dismissAttachmentError() {
        _attachmentError.value = null
    }

    // endregion

    // region Generation

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val attachments = _pendingAttachments.value
        if ((trimmed.isEmpty() && attachments.isEmpty()) || _isGenerating.value) return
        if (_isProcessingAttachments.value) return
        val provider = activeProvider() ?: return
        val model = provider.selectedModel ?: provider.models.firstOrNull() ?: return

        val convId = _currentConversationId.value ?: run {
            val conversation = Conversation(
                id = UUID.randomUUID().toString(),
                title = trimmed.ifBlank { attachments.firstOrNull()?.name ?: "New chat" }.take(48),
                providerId = provider.id,
                model = model,
            )
            _conversations.update { listOf(conversation) + it }
            _currentConversationId.value = conversation.id
            conversation.id
        }

        addMessage(
            convId,
            StoredMessage(
                id = UUID.randomUUID().toString(),
                role = "user",
                content = trimmed,
                attachments = attachments,
            ),
        )
        _pendingAttachments.value = emptyList()
        persistConversations()
        startGeneration(convId, provider, model)
    }

    fun regenerateLastResponse() {
        if (_isGenerating.value) return
        val convId = _currentConversationId.value ?: return
        val conversation = _conversations.value.find { it.id == convId } ?: return
        val lastUserIndex = conversation.messages.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) return
        // Validate provider/model BEFORE truncating so we never drop the last
        // response without being able to produce a new one.
        val provider = activeProvider() ?: return
        val model = provider.selectedModel ?: provider.models.firstOrNull() ?: return
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id != convId) conv
                else conv.copy(messages = conv.messages.take(lastUserIndex + 1))
            }
        }
        persistConversations()
        startGeneration(convId, provider, model)
    }

    /** Deletes a single message from the current conversation. */
    fun deleteMessage(messageId: String) {
        if (_isGenerating.value) return
        val convId = _currentConversationId.value ?: return
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id != convId) conv
                else conv.copy(messages = conv.messages.filterNot { it.id == messageId })
            }
        }
        persistConversations()
    }

    /**
     * Replaces the text of a user message, drops everything after it and
     * regenerates the assistant response from that point.
     */
    fun editAndResend(messageId: String, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return
        val convId = _currentConversationId.value ?: return
        val conversation = _conversations.value.find { it.id == convId } ?: return
        val index = conversation.messages.indexOfFirst { it.id == messageId }
        if (index < 0 || conversation.messages[index].role != "user") return
        val provider = activeProvider() ?: return
        val model = provider.selectedModel ?: provider.models.firstOrNull() ?: return
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id != convId) conv
                else {
                    val kept = conv.messages.take(index + 1).toMutableList()
                    kept[index] = kept[index].copy(content = trimmed)
                    conv.copy(
                        messages = kept,
                        title = if (index == 0) trimmed.take(48) else conv.title,
                    )
                }
            }
        }
        persistConversations()
        startGeneration(convId, provider, model)
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
    }

    private fun startGeneration(convId: String, provider: Provider, model: String) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            runGeneration(convId, provider, model)
        }
    }

    private suspend fun runGeneration(convId: String, provider: Provider, model: String) {
        _isGenerating.value = true
        _generationStatus.value = null

        val assistantId = UUID.randomUUID().toString()
        addMessage(
            convId,
            StoredMessage(id = assistantId, role = "assistant", content = "", model = model),
        )

        val currentSettings = _settings.value
        val working = mutableListOf<ApiMessage>()
        val systemPrompt = buildSystemPrompt(currentSettings)
        if (systemPrompt.isNotBlank()) {
            working += ApiMessage.text("system", systemPrompt)
        }
        _conversations.value.find { it.id == convId }?.messages
            ?.filter {
                !it.isError && it.id != assistantId &&
                    (it.content.isNotBlank() || it.attachments.isNotEmpty() || it.toolExchanges.isNotEmpty())
            }
            ?.forEach { stored ->
                // Replay past tool calls + results so the model knows what was
                // already searched and never repeats those searches.
                if (stored.role == "assistant" && stored.toolExchanges.isNotEmpty()) {
                    working += ApiMessage(
                        role = "assistant",
                        content = null,
                        toolCalls = stored.toolExchanges.map { exchange ->
                            ToolCallDto(
                                id = exchange.callId,
                                function = FunctionCallDto(name = exchange.name, arguments = exchange.arguments),
                            )
                        },
                    )
                    stored.toolExchanges.forEach { exchange ->
                        working += ApiMessage(
                            role = "tool",
                            content = JsonPrimitive(exchange.result),
                            toolCallId = exchange.callId,
                        )
                    }
                }
                if (stored.content.isNotBlank() || stored.attachments.isNotEmpty()) {
                    working += ApiMessage(role = stored.role, content = buildMessageContent(stored))
                }
            }

        val tools = if (currentSettings.toolCallingEnabled) {
            buildList {
                if (currentSettings.webSearchEnabled) add(webSearchToolDefinition)
                if (currentSettings.memoryEnabled) add(saveMemoryToolDefinition)
            }.ifEmpty { null }
        } else {
            null
        }

        val collected = StringBuilder()
        val reasoningCollected = StringBuilder()
        val sources = mutableListOf<SearchSource>()
        val exchanges = mutableListOf<ToolExchange>()
        val searchedQueries = collectPreviouslySearchedQueries(convId, assistantId)

        try {
            var round = 0
            while (round < MAX_TOOL_ROUNDS) {
                val roundRaw = StringBuilder()
                val roundReasoning = StringBuilder()
                val baseContent = collected.toString()
                val baseReasoning = reasoningCollected.toString()
                var pendingCalls: List<ToolCallDto> = emptyList()
                var failure: String? = null

                // Re-derives visible content and reasoning for this round from the
                // raw stream (handling inline <think> tags) and pushes to the UI.
                fun refreshAssistantMessage() {
                    val (thought, visible) = splitThinkTags(roundRaw.toString())
                    val content = buildString {
                        append(baseContent)
                        if (visible.isNotBlank()) {
                            if (baseContent.isNotEmpty()) append("\n\n")
                            append(visible)
                        }
                    }
                    val reasoning = buildString {
                        append(baseReasoning)
                        val roundThought = (roundReasoning.toString() + thought).trim()
                        if (roundThought.isNotEmpty()) {
                            if (baseReasoning.isNotEmpty()) append("\n\n")
                            append(roundThought)
                        }
                    }
                    collected.setLength(0)
                    collected.append(content)
                    reasoningCollected.setLength(0)
                    reasoningCollected.append(reasoning)
                    updateMessage(convId, assistantId) {
                        it.copy(content = content, reasoning = reasoning.ifBlank { null })
                    }
                }

                val request = ChatRequest(
                    model = model,
                    messages = working.toList(),
                    stream = currentSettings.streamingEnabled,
                    temperature = if (currentSettings.useCustomTemperature) {
                        currentSettings.temperature.toDouble()
                    } else {
                        null
                    },
                    tools = tools,
                )

                api.chat(provider.baseUrl, provider.apiKey, request).collect { event ->
                    when (event) {
                        is StreamEvent.Delta -> {
                            roundRaw.append(event.text)
                            refreshAssistantMessage()
                        }
                        is StreamEvent.ReasoningDelta -> {
                            roundReasoning.append(event.text)
                            refreshAssistantMessage()
                        }
                        is StreamEvent.ToolCallsCompleted -> pendingCalls = event.calls
                        is StreamEvent.Failed -> failure = event.message
                        StreamEvent.Finished -> Unit
                    }
                }

                val error = failure
                if (error != null) {
                    if (collected.isEmpty()) {
                        updateMessage(convId, assistantId) {
                            it.copy(content = error, isError = true)
                        }
                    } else {
                        updateMessage(convId, assistantId) {
                            it.copy(content = collected.toString() + "\n\n> Request failed: " + error)
                        }
                    }
                    break
                }
                if (pendingCalls.isEmpty()) break

                // Dedupe BEFORE recording the assistant tool_calls: every call
                // listed there must get a matching tool response or the next
                // request is rejected with HTTP 400 by most providers.
                val uniqueCalls = pendingCalls.distinctBy { it.function.name + "|" + it.function.arguments }
                val roundVisible = splitThinkTags(roundRaw.toString()).second
                working += ApiMessage(
                    role = "assistant",
                    content = roundVisible.ifBlank { null }?.let { JsonPrimitive(it) },
                    toolCalls = uniqueCalls,
                )
                for (call in uniqueCalls) {
                    working += executeToolCall(call, convId, assistantId, sources, exchanges, searchedQueries)
                }
                _generationStatus.value = "Thinking…"
                persistConversations()
                round++
            }
        } finally {
            _generationStatus.value = null
            _isGenerating.value = false
            updateMessage(convId, assistantId) { message ->
                val withExchanges = message.copy(toolExchanges = exchanges.toList())
                if (withExchanges.content.isBlank() && !withExchanges.isError) {
                    withExchanges.copy(content = "No response was returned by the model.", isError = true)
                } else {
                    withExchanges
                }
            }
            persistConversations()
        }
    }

    /**
     * Splits raw model output into (reasoning, visible content) by extracting
     * inline <think>...</think> blocks emitted by reasoning models (DeepSeek R1,
     * QwQ, etc.). A partially streamed tag at the end is trimmed so it never
     * flashes in the UI while tokens arrive.
     */
    private fun splitThinkTags(rawInput: String): Pair<String, String> {
        var raw = rawInput
        for (tag in listOf("</think>", "<think>")) {
            for (len in tag.length - 1 downTo 1) {
                if (raw.endsWith(tag.substring(0, len))) {
                    raw = raw.dropLast(len)
                    break
                }
            }
        }
        if (!raw.contains("<think>")) return "" to raw
        val thinking = StringBuilder()
        val visible = StringBuilder()
        var rest = raw
        while (true) {
            val start = rest.indexOf("<think>")
            if (start < 0) {
                visible.append(rest)
                break
            }
            visible.append(rest.substring(0, start))
            val afterStart = rest.substring(start + "<think>".length)
            val end = afterStart.indexOf("</think>")
            if (end < 0) {
                thinking.append(afterStart)
                break
            }
            thinking.append(afterStart.substring(0, end))
            rest = afterStart.substring(end + "</think>".length)
        }
        return thinking.toString().trim() to visible.toString().trimStart('\n')
    }

    /** Queries already searched in earlier turns of this conversation (normalized). */
    private fun collectPreviouslySearchedQueries(convId: String, assistantId: String): MutableSet<String> {
        val queries = mutableSetOf<String>()
        _conversations.value.find { it.id == convId }?.messages
            ?.filter { it.id != assistantId }
            ?.flatMap { it.toolExchanges }
            ?.filter { it.name == "web_search" }
            ?.forEach { exchange ->
                parseArgument(exchange.arguments, "query")?.let { queries += normalizeQuery(it) }
            }
        return queries
    }

    private fun normalizeQuery(query: String): String =
        query.trim().lowercase().replace(Regex("\\s+"), " ")

    private suspend fun executeToolCall(
        call: ToolCallDto,
        convId: String,
        assistantId: String,
        sources: MutableList<SearchSource>,
        exchanges: MutableList<ToolExchange>,
        searchedQueries: MutableSet<String>,
    ): ApiMessage {
        fun record(result: String): ApiMessage {
            exchanges += ToolExchange(
                callId = call.id,
                name = call.function.name,
                arguments = call.function.arguments,
                result = result,
            )
            return ApiMessage(role = "tool", content = JsonPrimitive(result), toolCallId = call.id)
        }

        if (call.function.name == "save_memory") {
            val content = parseArgument(call.function.arguments, "content")
            val saved = !content.isNullOrBlank() && saveAiMemory(content)
            if (saved) _generationStatus.value = "Saving to memory…"
            return record(if (saved) "Memory saved." else "Error: missing \"content\" argument.")
        }
        if (call.function.name != "web_search") {
            return record("Error: unknown tool \"${call.function.name}\".")
        }
        val query = parseArgument(call.function.arguments, "query")
        if (query.isNullOrBlank()) {
            return record("Error: missing \"query\" argument.")
        }
        // Hard dedupe: refuse to repeat a search already done in this
        // conversation — point the model at the earlier results instead.
        if (!searchedQueries.add(normalizeQuery(query))) {
            return record(
                "This query was already searched earlier in this conversation and its results are in the " +
                    "tool history above. Do NOT search it again — reuse those results and answer the " +
                    "user's latest message directly.",
            )
        }
        _generationStatus.value = "Searching the web: $query"
        val results = runCatching { webSearch.search(query) }.getOrDefault(emptyList())
        results.forEach { result ->
            if (sources.none { it.url == result.url }) {
                sources += SearchSource(title = result.title, url = result.url)
            }
        }
        updateMessage(convId, assistantId) { it.copy(sources = sources.toList()) }
        if (results.isEmpty()) {
            return record("No web results found for \"$query\".")
        }
        val payload = buildJsonArray {
            results.forEach { result ->
                addJsonObject {
                    put("title", result.title)
                    put("url", result.url)
                    put("snippet", result.snippet)
                    result.pageContent?.let { put("page_content", it) }
                }
            }
        }.toString()
        val fullContent = "Search results for \"$query\" (with extracted page content). " +
            "Use them to write a synthesized answer in your own words that directly answers the user. " +
            "Do NOT just list the URLs.\n" + payload
        // Persist a compact record (no page content) so replayed history on
        // follow-up turns stays small but still shows what was searched.
        val compactResult = buildString {
            append("Already searched the web for \"").append(query).append("\". Top results: ")
            append(results.joinToString("; ") { "${it.title} (${it.url})" })
            append(". These results were already used to answer — do not repeat this search.")
        }.take(MAX_STORED_TOOL_RESULT_LENGTH)
        exchanges += ToolExchange(
            callId = call.id,
            name = call.function.name,
            arguments = call.function.arguments,
            result = compactResult,
        )
        return ApiMessage(role = "tool", content = JsonPrimitive(fullContent), toolCallId = call.id)
    }

    /**
     * Builds the API content for a stored message: plain string when there are
     * no attachments, otherwise a multimodal parts array. Document/PDF text is
     * inlined into the text part; images become image_url data-URL parts.
     */
    private fun buildMessageContent(message: StoredMessage): JsonElement {
        if (message.attachments.isEmpty()) return JsonPrimitive(message.content)

        val textBuilder = StringBuilder(message.content)
        message.attachments
            .filter { it.kind != AttachmentKind.IMAGE && !it.textContent.isNullOrBlank() }
            .forEach { doc ->
                if (textBuilder.isNotEmpty()) textBuilder.append("\n\n")
                textBuilder.append("Attached file \"${doc.name}\":\n")
                textBuilder.append(doc.textContent)
            }

        val images = message.attachments.filter {
            it.kind == AttachmentKind.IMAGE && !it.base64Data.isNullOrBlank()
        }
        if (images.isEmpty()) return JsonPrimitive(textBuilder.toString())

        return buildJsonArray {
            if (textBuilder.isNotBlank()) {
                addJsonObject {
                    put("type", "text")
                    put("text", textBuilder.toString())
                }
            }
            images.forEach { image ->
                addJsonObject {
                    put("type", "image_url")
                    putJsonObject("image_url") {
                        put("url", "data:${image.mimeType};base64,${image.base64Data}")
                    }
                }
            }
        }
    }

    private fun parseArgument(arguments: String, key: String): String? = try {
        json.parseToJsonElement(arguments).jsonObject[key]?.jsonPrimitive?.content
    } catch (e: Exception) {
        null
    }

    // endregion

    private fun addMessage(convId: String, message: StoredMessage) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id != convId) conv
                else conv.copy(
                    messages = conv.messages + message,
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }
    }

    private fun updateMessage(
        convId: String,
        messageId: String,
        transform: (StoredMessage) -> StoredMessage,
    ) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id != convId) conv
                else conv.copy(
                    messages = conv.messages.map { if (it.id == messageId) transform(it) else it },
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }
    }

    private fun persistConversations() {
        repository.saveConversations(_conversations.value)
    }

    private companion object {
        const val MAX_TOOL_ROUNDS = 4
        const val MAX_ATTACHMENTS = 6
        const val MAX_MEMORIES = 100
        const val MAX_MEMORY_LENGTH = 300
        const val MAX_STORED_TOOL_RESULT_LENGTH = 1_200
    }
}
