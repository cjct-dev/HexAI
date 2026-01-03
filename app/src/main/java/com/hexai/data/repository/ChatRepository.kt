package com.hexai.data.repository

import com.hexai.data.api.*
import kotlinx.coroutines.flow.Flow

class ChatRepository {

    private var apiService: OpenAIApiService? = null
    private var streamingClient: StreamingApiClient? = null

    fun configure(url: String, apiKey: String?) {
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
