package com.hexai.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response as OkHttpResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import java.util.concurrent.TimeUnit
import java.io.IOException
import java.net.SocketException
import com.google.gson.Gson
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface OpenAIApiService {
    @GET("v1/models")
    suspend fun getModels(
        @Header("Authorization") authorization: String?
    ): Response<ModelsResponse>

    companion object {
        fun create(baseUrl: String): OpenAIApiService {
            val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenAIApiService::class.java)
        }
    }
}

class StreamingApiClient(
    private val baseUrl: String,
    private val apiKey: String?
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun streamChatCompletion(request: ChatCompletionRequest): Flow<StreamEvent> = flow {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val url = "${normalizedUrl}v1/chat/completions"

        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .apply {
                if (!apiKey.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $apiKey")
                }
                addHeader("Content-Type", "application/json")
                addHeader("Accept", "text/event-stream")
            }
            .build()

        val startTime = System.currentTimeMillis()
        var firstTokenTime: Long? = null
        var tokenCount = 0
        var response: okhttp3.Response? = null

        try {
            response = withContext(Dispatchers.IO) {
                client.newCall(httpRequest).execute()
            }

            // Check if cancelled before proceeding
            currentCoroutineContext().ensureActive()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                emit(StreamEvent.Error("HTTP ${response.code}: $errorBody"))
                return@flow
            }

            emit(StreamEvent.Started)

            val source = response.body?.source() ?: run {
                emit(StreamEvent.Error("Empty response body"))
                return@flow
            }

            while (currentCoroutineContext().isActive && !source.exhausted()) {
                val line = withContext(Dispatchers.IO) {
                    source.readUtf8Line()
                } ?: break

                // Check cancellation after each line read
                currentCoroutineContext().ensureActive()

                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()

                    if (data == "[DONE]") {
                        val totalTime = System.currentTimeMillis() - startTime
                        emit(StreamEvent.Done(
                            timeToFirstToken = firstTokenTime?.minus(startTime) ?: 0,
                            totalTime = totalTime
                        ))
                        break
                    }

                    try {
                        val chunk = gson.fromJson(data, ChatCompletionChunk::class.java)

                        chunk.choices?.firstOrNull()?.delta?.let { delta ->
                            delta.content?.let { content ->
                                if (firstTokenTime == null) {
                                    firstTokenTime = System.currentTimeMillis()
                                }
                                tokenCount++
                                emit(StreamEvent.Content(content))
                            }

                            delta.reasoningContent?.let { reasoning ->
                                if (firstTokenTime == null) {
                                    firstTokenTime = System.currentTimeMillis()
                                }
                                emit(StreamEvent.Reasoning(reasoning))
                            }
                        }

                        chunk.usage?.let { usage ->
                            emit(StreamEvent.Usage(usage))
                        }

                        // llama-serve timings support
                        chunk.timings?.let { timings ->
                            emit(StreamEvent.Timings(timings))
                        }

                        chunk.choices?.firstOrNull()?.finishReason?.let { reason ->
                            if (reason == "stop" || reason == "end_turn") {
                                val totalTime = System.currentTimeMillis() - startTime
                                emit(StreamEvent.Done(
                                    timeToFirstToken = firstTokenTime?.minus(startTime) ?: 0,
                                    totalTime = totalTime
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        // Skip malformed JSON chunks
                    }
                }
            }

        } catch (e: CancellationException) {
            // Coroutine was cancelled (user cancelled, app backgrounded, etc.)
            // Don't emit error, just let the flow complete gracefully
            throw e // Re-throw to properly cancel the coroutine
        } catch (e: SocketException) {
            // Connection was closed (app backgrounded, network change, etc.)
            val message = when {
                e.message?.contains("abort", ignoreCase = true) == true ->
                    "Connection interrupted (app may have been backgrounded)"
                e.message?.contains("reset", ignoreCase = true) == true ->
                    "Connection reset by server"
                else -> "Connection error: ${e.message}"
            }
            emit(StreamEvent.Error(message))
        } catch (e: IOException) {
            // Network error - provide user-friendly message
            val message = when {
                e.message?.contains("closed", ignoreCase = true) == true ->
                    "Connection closed unexpectedly"
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Connection timed out"
                else -> "Network error: ${e.message}"
            }
            emit(StreamEvent.Error(message))
        } catch (e: Exception) {
            emit(StreamEvent.Error(e.message ?: "Unknown error"))
        } finally {
            // Always close the response to free resources
            try {
                response?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }.flowOn(Dispatchers.IO)
}

sealed class StreamEvent {
    object Started : StreamEvent()
    data class Content(val text: String) : StreamEvent()
    data class Reasoning(val text: String) : StreamEvent()
    data class Usage(val usage: com.hexai.data.api.Usage) : StreamEvent()
    data class Timings(val timings: com.hexai.data.api.Timings) : StreamEvent()
    data class Done(val timeToFirstToken: Long, val totalTime: Long) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}

// Health check and model management client
class HexApiClient(private val baseUrl: String, private val apiKey: String?) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun normalizedUrl(): String = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    suspend fun checkHealth(): Result<Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url("${normalizedUrl()}health")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                response.close()
                Result.success(latency)
            } else {
                response.close()
                Result.failure(Exception("Health check failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadModel(modelId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = gson.toJson(ModelLoadRequest(modelId))
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${normalizedUrl()}models/load")
                .post(requestBody)
                .apply {
                    if (!apiKey.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $apiKey")
                    }
                }
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()

            if (success) Result.success(true)
            else Result.failure(Exception("Failed to load model: ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unloadModel(modelId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = gson.toJson(ModelUnloadRequest(modelId))
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${normalizedUrl()}models/unload")
                .post(requestBody)
                .apply {
                    if (!apiKey.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $apiKey")
                    }
                }
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()

            if (success) Result.success(true)
            else Result.failure(Exception("Failed to unload model: ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLlamaModels(): Result<List<LlamaModelInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${normalizedUrl()}models")
                .get()
                .apply {
                    if (!apiKey.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $apiKey")
                    }
                }
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                response.close()
                val modelsResponse = gson.fromJson(body, LlamaModelsResponse::class.java)
                Result.success(modelsResponse.data ?: emptyList())
            } else {
                response.close()
                Result.failure(Exception("Failed to get models: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
