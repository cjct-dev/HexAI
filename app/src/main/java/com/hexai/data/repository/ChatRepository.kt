package com.hexai.data.repository

import com.hexai.data.api.*
import kotlinx.coroutines.flow.Flow

data class ConnectionResult(
    val success: Boolean,
    val url: String,
    val isSecure: Boolean,
    val error: String? = null
)

class ChatRepository {

    private var apiService: OpenAIApiService? = null
    private var streamingClient: StreamingApiClient? = null
    private var configuredUrl: String = ""
    var isSecureConnection: Boolean = false
        private set

    fun configure(url: String, apiKey: String?) {
        configuredUrl = url
        apiService = OpenAIApiService.create(url)
        streamingClient = StreamingApiClient(url, apiKey)
    }

    suspend fun fetchModels(apiKey: String?): Result<List<ModelInfo>> {
        val service = apiService ?: return Result.failure(Exception("API not configured"))

        return try {
            val authHeader = if (!apiKey.isNullOrBlank()) "Bearer $apiKey" else null
            val response = service.getModels(authHeader)

            if (response.isSuccessful) {
                val models = response.body()?.data ?: emptyList()
                Result.success(models)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Failed to fetch models: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChat(
        model: String,
        messages: List<ChatMessage>,
        settings: ModelSettings
    ): Flow<StreamEvent> {
        val client = streamingClient
            ?: throw IllegalStateException("Streaming client not configured")

        val request = ChatCompletionRequest(
            model = model,
            messages = buildMessageList(messages, settings.systemPrompt),
            stream = true,
            temperature = settings.temperature,
            maxTokens = settings.maxTokens.takeIf { it > 0 },
            topP = settings.topP,
            frequencyPenalty = settings.frequencyPenalty.takeIf { it != 0f },
            presencePenalty = settings.presencePenalty.takeIf { it != 0f },
            reasoningEffort = settings.reasoningEffort.value
        )

        return client.streamChatCompletion(request)
    }

    suspend fun bulkChat(
        model: String,
        messages: List<ChatMessage>,
        settings: ModelSettings
    ): Result<ChatCompletionResponse> {
        val client = streamingClient
            ?: return Result.failure(IllegalStateException("API client not configured"))

        val request = ChatCompletionRequest(
            model = model,
            messages = buildMessageList(messages, settings.systemPrompt),
            stream = false,
            temperature = settings.temperature,
            maxTokens = settings.maxTokens.takeIf { it > 0 },
            topP = settings.topP,
            frequencyPenalty = settings.frequencyPenalty.takeIf { it != 0f },
            presencePenalty = settings.presencePenalty.takeIf { it != 0f },
            reasoningEffort = settings.reasoningEffort.value
        )

        return client.chatCompletion(request)
    }

    private fun buildMessageList(
        messages: List<ChatMessage>,
        systemPrompt: String
    ): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()

        if (systemPrompt.isNotBlank()) {
            result.add(ChatMessage(role = "system", content = systemPrompt))
        }

        result.addAll(messages)
        return result
    }

    /**
     * Normalizes URL by adding protocol if missing.
     * Returns the normalized URL.
     */
    fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            else -> "https://$trimmed" // Default to https, will fall back to http
        }
    }

    /**
     * Tests connection with automatic protocol detection.
     * Tries https first, then falls back to http if needed.
     */
    suspend fun testConnectionWithAutoProtocol(inputUrl: String, apiKey: String?): ConnectionResult {
        val trimmedUrl = inputUrl.trim()

        // If URL already has a protocol, use it directly
        if (trimmedUrl.startsWith("https://") || trimmedUrl.startsWith("http://")) {
            val result = testConnection(trimmedUrl, apiKey)
            return if (result.isSuccess) {
                isSecureConnection = trimmedUrl.startsWith("https://")
                ConnectionResult(
                    success = true,
                    url = trimmedUrl,
                    isSecure = isSecureConnection
                )
            } else {
                ConnectionResult(
                    success = false,
                    url = trimmedUrl,
                    isSecure = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }

        // Try https first
        val httpsUrl = "https://$trimmedUrl"
        val httpsResult = testConnection(httpsUrl, apiKey)
        if (httpsResult.isSuccess) {
            isSecureConnection = true
            return ConnectionResult(
                success = true,
                url = httpsUrl,
                isSecure = true
            )
        }

        // Fall back to http
        val httpUrl = "http://$trimmedUrl"
        val httpResult = testConnection(httpUrl, apiKey)
        if (httpResult.isSuccess) {
            isSecureConnection = false
            return ConnectionResult(
                success = true,
                url = httpUrl,
                isSecure = false
            )
        }

        // Both failed
        isSecureConnection = false
        return ConnectionResult(
            success = false,
            url = inputUrl,
            isSecure = false,
            error = httpResult.exceptionOrNull()?.message ?: "Connection failed"
        )
    }

    suspend fun testConnection(url: String, apiKey: String?): Result<Boolean> {
        return try {
            val tempService = OpenAIApiService.create(url)
            val authHeader = if (!apiKey.isNullOrBlank()) "Bearer $apiKey" else null
            val response = tempService.getModels(authHeader)

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Connection failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
