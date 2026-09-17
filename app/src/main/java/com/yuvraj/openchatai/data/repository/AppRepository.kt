package com.yuvraj.openchatai.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.yuvraj.openchatai.data.model.AppSettings
import com.yuvraj.openchatai.data.model.Conversation
import com.yuvraj.openchatai.data.model.Memory
import com.yuvraj.openchatai.data.model.PromptPreset
import com.yuvraj.openchatai.data.model.Provider
import kotlinx.serialization.json.Json
import java.io.File

/**
 * On-device persistence layer. Small state (providers, settings, memories)
 * lives in SharedPreferences; conversation history lives in a JSON file in
 * internal storage (written atomically via a temp file) so large histories
 * with attachments survive safely across app restarts.
 */
class AppRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("openchat_store", Context.MODE_PRIVATE)

    private val filesDir: File = context.filesDir
    private val conversationsFile: File = File(filesDir, "conversations.json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadProviders(): List<Provider> = decodeOrDefault(KEY_PROVIDERS, emptyList())

    fun saveProviders(providers: List<Provider>) {
        prefs.edit().putString(KEY_PROVIDERS, json.encodeToString(providers)).apply()
    }

    fun loadSettings(): AppSettings = decodeOrDefault(KEY_SETTINGS, AppSettings())

    fun saveSettings(settings: AppSettings) {
        prefs.edit().putString(KEY_SETTINGS, json.encodeToString(settings)).apply()
    }

    fun loadMemories(): List<Memory> = decodeOrDefault(KEY_MEMORIES, emptyList())

    fun saveMemories(memories: List<Memory>) {
        prefs.edit().putString(KEY_MEMORIES, json.encodeToString(memories)).apply()
    }

    fun loadPromptPresets(): List<PromptPreset> = decodeOrDefault(KEY_PROMPT_PRESETS, emptyList())

    fun savePromptPresets(presets: List<PromptPreset>) {
        prefs.edit().putString(KEY_PROMPT_PRESETS, json.encodeToString(presets)).apply()
    }

    /**
     * Loads conversation history from the internal-storage file, falling back
     * to (and migrating from) the legacy SharedPreferences entry.
     */
    fun loadConversations(): List<Conversation> {
        val fromFile = readConversationsFile()
        if (fromFile != null) return fromFile

        val legacy = decodeOrDefault<List<Conversation>>(KEY_CONVERSATIONS_LEGACY, emptyList())
        if (legacy.isNotEmpty()) {
            saveConversations(legacy)
            prefs.edit().remove(KEY_CONVERSATIONS_LEGACY).apply()
        }
        return legacy
    }

    /** Writes conversations atomically: temp file first, then rename. */
    fun saveConversations(conversations: List<Conversation>) {
        try {
            val tmp = File(filesDir, "conversations.json.tmp")
            tmp.writeText(json.encodeToString(conversations))
            if (!tmp.renameTo(conversationsFile)) {
                conversationsFile.writeText(json.encodeToString(conversations))
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to save conversations", e)
        }
    }

    private fun readConversationsFile(): List<Conversation>? {
        if (!conversationsFile.exists()) return null
        return try {
            json.decodeFromString<List<Conversation>>(conversationsFile.readText())
        } catch (e: Exception) {
            Log.w("AppRepository", "Failed to read conversations file", e)
            null
        }
    }

    private inline fun <reified T> decodeOrDefault(key: String, default: T): T {
        val raw = prefs.getString(key, null) ?: return default
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: Exception) {
            Log.w("AppRepository", "Failed to decode $key, falling back to default")
            default
        }
    }

    private companion object {
        const val KEY_PROVIDERS = "providers_v1"
        const val KEY_SETTINGS = "settings_v1"
        const val KEY_MEMORIES = "memories_v1"
        const val KEY_PROMPT_PRESETS = "prompt_presets_v1"
        const val KEY_CONVERSATIONS_LEGACY = "conversations_v1"
    }
}
