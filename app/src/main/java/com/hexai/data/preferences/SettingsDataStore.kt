package com.hexai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hexai.data.api.ModelSettings
import com.hexai.data.api.ReasoningEffort
import com.hexai.data.api.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hexai_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        // Server config keys
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val API_KEY = stringPreferencesKey("api_key")
        private val SELECTED_MODEL = stringPreferencesKey("selected_model")

        // Model settings keys
        private val TEMPERATURE = floatPreferencesKey("temperature")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val TOP_P = floatPreferencesKey("top_p")
        private val FREQUENCY_PENALTY = floatPreferencesKey("frequency_penalty")
        private val PRESENCE_PENALTY = floatPreferencesKey("presence_penalty")
        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val REASONING_EFFORT = stringPreferencesKey("reasoning_effort")
    }

    val serverConfig: Flow<ServerConfig> = context.dataStore.data.map { preferences ->
        ServerConfig(
            url = preferences[SERVER_URL] ?: "",
            apiKey = preferences[API_KEY] ?: "",
            selectedModel = preferences[SELECTED_MODEL] ?: ""
        )
    }

    val modelSettings: Flow<ModelSettings> = context.dataStore.data.map { preferences ->
        ModelSettings(
            temperature = preferences[TEMPERATURE] ?: 0.7f,
            maxTokens = preferences[MAX_TOKENS] ?: 2048,
            topP = preferences[TOP_P] ?: 1.0f,
            frequencyPenalty = preferences[FREQUENCY_PENALTY] ?: 0f,
            presencePenalty = preferences[PRESENCE_PENALTY] ?: 0f,
            systemPrompt = preferences[SYSTEM_PROMPT] ?: "",
            reasoningEffort = try {
                ReasoningEffort.valueOf(preferences[REASONING_EFFORT] ?: "MEDIUM")
            } catch (e: IllegalArgumentException) {
                ReasoningEffort.MEDIUM
            }
        )
    }

    suspend fun saveServerConfig(config: ServerConfig) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = config.url
            preferences[API_KEY] = config.apiKey
            preferences[SELECTED_MODEL] = config.selectedModel
        }
    }

    suspend fun saveModelSettings(settings: ModelSettings) {
        context.dataStore.edit { preferences ->
            preferences[TEMPERATURE] = settings.temperature
            preferences[MAX_TOKENS] = settings.maxTokens
            preferences[TOP_P] = settings.topP
            preferences[FREQUENCY_PENALTY] = settings.frequencyPenalty
            preferences[PRESENCE_PENALTY] = settings.presencePenalty
            preferences[SYSTEM_PROMPT] = settings.systemPrompt
            preferences[REASONING_EFFORT] = settings.reasoningEffort.name
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
