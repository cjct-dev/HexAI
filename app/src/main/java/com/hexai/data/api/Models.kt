package com.hexai.data.api

import com.google.gson.annotations.SerializedName

// Request models
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Float? = null,
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    @SerializedName("top_p")
    val topP: Float? = null,
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Float? = null,
    @SerializedName("presence_penalty")
    val presencePenalty: Float? = null,
    @SerializedName("reasoning_effort")
    val reasoningEffort: String? = null // "low", "medium", "high" for compatible APIs
)

data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

// Response models for streaming
data class ChatCompletionChunk(
    val id: String?,
    val `object`: String?,
    val created: Long?,
    val model: String?,
    val choices: List<ChunkChoice>?,
    val usage: Usage?,
    val timings: Timings? // llama-serve specific
)

// llama-serve timing statistics
data class Timings(
    @SerializedName("prompt_n")
    val promptN: Int?,
    @SerializedName("prompt_ms")
    val promptMs: Float?,
    @SerializedName("prompt_per_token_ms")
    val promptPerTokenMs: Float?,
    @SerializedName("prompt_per_second")
    val promptPerSecond: Float?,
    @SerializedName("predicted_n")
    val predictedN: Int?,
    @SerializedName("predicted_ms")
    val predictedMs: Float?,
    @SerializedName("predicted_per_token_ms")
    val predictedPerTokenMs: Float?,
    @SerializedName("predicted_per_second")
    val predictedPerSecond: Float?
)

data class ChunkChoice(
    val index: Int?,
    val delta: Delta?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class Delta(
    val role: String?,
    val content: String?,
    @SerializedName("reasoning_content")
    val reasoningContent: String? // For models with thinking output
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,
    @SerializedName("completion_tokens")
    val completionTokens: Int?,
    @SerializedName("total_tokens")
    val totalTokens: Int?
)

// Non-streaming chat completion response
data class ChatCompletionResponse(
    val id: String?,
    val `object`: String?,
    val created: Long?,
    val model: String?,
    val choices: List<CompletionChoice>?,
    val usage: Usage?,
    val timings: Timings?
)

data class CompletionChoice(
    val index: Int?,
    val message: CompletionMessage?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class CompletionMessage(
    val role: String?,
    val content: String?,
    @SerializedName("reasoning_content")
    val reasoningContent: String?
)

// Models list response
data class ModelsResponse(
    val `object`: String?,
    val data: List<ModelInfo>?
)

data class ModelInfo(
    val id: String,
    val `object`: String?,
    val created: Long?,
    @SerializedName("owned_by")
    val ownedBy: String?
)

// UI State models
data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val thinkingContent: String? = null,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class InferenceStats(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val tokensPerSecond: Float = 0f,
    val timeToFirstToken: Long = 0L,
    val totalTime: Long = 0L
)

data class ModelSettings(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val topP: Float = 1.0f,
    val frequencyPenalty: Float = 0f,
    val presencePenalty: Float = 0f,
    val systemPrompt: String = "",
    val reasoningEffort: ReasoningEffort = ReasoningEffort.MEDIUM,
    val useStreaming: Boolean = true // false = bulk/non-streaming mode
)

enum class ReasoningEffort(val value: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high")
}

data class ServerConfig(
    val url: String = "",
    val apiKey: String = "",
    val selectedModel: String = ""
)

// llama-serve model management (router mode)
data class ModelLoadRequest(
    val model: String
)

data class ModelUnloadRequest(
    val model: String
)

// Extended model info for llama-serve
data class LlamaModelInfo(
    val id: String,
    val `object`: String?,
    val created: Long?,
    @SerializedName("owned_by")
    val ownedBy: String?,
    val status: String?, // "loaded", "loading", "unloaded"
    @SerializedName("in_cache")
    val inCache: Boolean?,
    val path: String?
)

data class LlamaModelsResponse(
    val `object`: String?,
    val data: List<LlamaModelInfo>?
)

// Health check response
data class HealthResponse(
    val status: String?
)
