package com.hexai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hexai.data.api.*
import com.hexai.data.preferences.SettingsDataStore
import com.hexai.data.repository.ChatRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository()
    private val settingsDataStore = SettingsDataStore(application)
    private var hexApiClient: HexApiClient? = null

    // Server configuration state
    private val _serverConfig = MutableStateFlow(ServerConfig())
    val serverConfig: StateFlow<ServerConfig> = _serverConfig.asStateFlow()

    // Connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    // Ping latency for health check
    private val _pingLatency = MutableStateFlow<Long?>(null)
    val pingLatency: StateFlow<Long?> = _pingLatency.asStateFlow()

    // Connection security (https vs http)
    private val _isSecureConnection = MutableStateFlow(false)
    val isSecureConnection: StateFlow<Boolean> = _isSecureConnection.asStateFlow()

    // Models list
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    // Model loading/unloading support (llama-serve)
    private val _supportsModelManagement = MutableStateFlow(false)
    val supportsModelManagement: StateFlow<Boolean> = _supportsModelManagement.asStateFlow()

    private val _isLoadingModel = MutableStateFlow(false)
    val isLoadingModel: StateFlow<Boolean> = _isLoadingModel.asStateFlow()

    // Messages
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // Current streaming message state
    private val _currentContent = MutableStateFlow("")
    private val _currentThinking = MutableStateFlow("")
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Inference stats
    private val _inferenceStats = MutableStateFlow(InferenceStats())
    val inferenceStats: StateFlow<InferenceStats> = _inferenceStats.asStateFlow()

    // Model settings
    private val _modelSettings = MutableStateFlow(ModelSettings())
    val modelSettings: StateFlow<ModelSettings> = _modelSettings.asStateFlow()

    // UI preferences
    private val _showThinking = MutableStateFlow(true)
    val showThinking: StateFlow<Boolean> = _showThinking.asStateFlow()

    private val _showStats = MutableStateFlow(true)
    val showStats: StateFlow<Boolean> = _showStats.asStateFlow()

    private var streamingJob: Job? = null
    private var healthCheckJob: Job? = null
    private var streamStartTime: Long = 0
    private var tokenCount: Int = 0

    init {
        // Load saved settings on startup
        viewModelScope.launch {
            settingsDataStore.serverConfig.collect { config ->
                _serverConfig.value = config
                // Auto-connect if URL is saved
                if (config.url.isNotBlank() && !_isConnected.value && !_isConnecting.value) {
                    connect()
                }
            }
        }

        viewModelScope.launch {
            settingsDataStore.modelSettings.collect { settings ->
                _modelSettings.value = settings
            }
        }
    }

    fun updateServerUrl(url: String) {
        _serverConfig.update { it.copy(url = url) }
    }

    fun updateApiKey(key: String) {
        _serverConfig.update { it.copy(apiKey = key) }
    }

    fun selectModel(modelId: String) {
        _serverConfig.update { it.copy(selectedModel = modelId) }
        viewModelScope.launch {
            settingsDataStore.saveServerConfig(_serverConfig.value)
        }
    }

    fun connect() {
        val config = _serverConfig.value
        if (config.url.isBlank()) {
            _connectionError.value = "Please enter a server URL"
            return
        }

        _isConnecting.value = true
        _connectionError.value = null

        viewModelScope.launch {
            // Use auto-protocol detection (tries https first, then http)
            val connectionResult = repository.testConnectionWithAutoProtocol(
                config.url,
                config.apiKey.takeIf { it.isNotBlank() }
            )

            if (connectionResult.success) {
                // Update config with the resolved URL (including protocol)
                val resolvedUrl = connectionResult.url
                _serverConfig.update { it.copy(url = resolvedUrl) }
                _isSecureConnection.value = connectionResult.isSecure

                repository.configure(resolvedUrl, config.apiKey.takeIf { it.isNotBlank() })
                hexApiClient = HexApiClient(resolvedUrl, config.apiKey.takeIf { it.isNotBlank() })
                _isConnected.value = true
                _isConnecting.value = false

                // Save settings on successful connection (with resolved URL)
                settingsDataStore.saveServerConfig(_serverConfig.value)

                fetchModels()
                startHealthCheck()
                checkModelManagementSupport()
            } else {
                _connectionError.value = connectionResult.error ?: "Connection failed"
                _isConnecting.value = false
            }
        }
    }

    fun disconnect() {
        healthCheckJob?.cancel()
        _isConnected.value = false
        _isSecureConnection.value = false
        _availableModels.value = emptyList()
        _messages.value = emptyList()
        _pingLatency.value = null
        _supportsModelManagement.value = false
        hexApiClient = null
        _serverConfig.update { it.copy(selectedModel = "") }
    }

    private fun startHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = viewModelScope.launch {
            try {
                while (_isConnected.value) {
                    try {
                        hexApiClient?.checkHealth()?.fold(
                            onSuccess = { latency ->
                                _pingLatency.value = latency
                            },
                            onFailure = {
                                _pingLatency.value = null
                            }
                        )
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        _pingLatency.value = null
                    }
                    delay(5000) // Check every 5 seconds
                }
            } catch (e: CancellationException) {
                // Health check cancelled, that's fine
            }
        }
    }

    private fun checkModelManagementSupport() {
        viewModelScope.launch {
            hexApiClient?.getLlamaModels()?.fold(
                onSuccess = { models ->
                    // If we get extended model info with status, model management is supported
                    _supportsModelManagement.value = models.any { it.status != null }
                },
                onFailure = {
                    _supportsModelManagement.value = false
                }
            )
        }
    }

    fun loadModel(modelId: String) {
        if (!_supportsModelManagement.value) return

        _isLoadingModel.value = true
        viewModelScope.launch {
            hexApiClient?.loadModel(modelId)?.fold(
                onSuccess = {
                    fetchModels() // Refresh model list
                },
                onFailure = { e ->
                    _error.value = "Failed to load model: ${e.message}"
                }
            )
            _isLoadingModel.value = false
        }
    }

    fun unloadModel(modelId: String) {
        if (!_supportsModelManagement.value) return

        _isLoadingModel.value = true
        viewModelScope.launch {
            hexApiClient?.unloadModel(modelId)?.fold(
                onSuccess = {
                    fetchModels() // Refresh model list
                },
                onFailure = { e ->
                    _error.value = "Failed to unload model: ${e.message}"
                }
            )
            _isLoadingModel.value = false
        }
    }

    private fun fetchModels() {
        val apiKey = _serverConfig.value.apiKey.takeIf { it.isNotBlank() }

        _isLoadingModels.value = true

        viewModelScope.launch {
            val result = repository.fetchModels(apiKey)

            result.fold(
                onSuccess = { models ->
                    _availableModels.value = models.sortedBy { it.id }
                    // If we have a saved model selection, use it
                    val savedModel = _serverConfig.value.selectedModel
                    if (savedModel.isNotBlank() && models.any { it.id == savedModel }) {
                        // Keep the saved model
                    } else if (models.isNotEmpty() && savedModel.isBlank()) {
                        _serverConfig.update { it.copy(selectedModel = models.first().id) }
                    }
                },
                onFailure = { e ->
                    _error.value = "Failed to fetch models: ${e.message}"
                }
            )

            _isLoadingModels.value = false
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isStreaming.value) return

        val config = _serverConfig.value
        if (config.selectedModel.isBlank()) {
            _error.value = "Please select a model first"
            return
        }

        // Add user message
        val userMessage = Message(
            role = MessageRole.USER,
            content = content.trim()
        )
        _messages.update { it + userMessage }

        // Create placeholder for assistant response
        val assistantMessage = Message(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )
        _messages.update { it + assistantMessage }

        // Reset streaming state
        _currentContent.value = ""
        _currentThinking.value = ""
        _isStreaming.value = true
        _error.value = null
        _inferenceStats.value = InferenceStats()
        streamStartTime = System.currentTimeMillis()
        tokenCount = 0

        // Convert messages to API format
        val chatMessages = _messages.value
            .filter { it.role != MessageRole.SYSTEM && !it.isStreaming }
            .map { msg ->
                ChatMessage(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    },
                    content = msg.content
                )
            }

        streamingJob = viewModelScope.launch {
            try {
                repository.streamChat(
                    model = config.selectedModel,
                    messages = chatMessages,
                    settings = _modelSettings.value
                ).catch { e ->
                    // Handle flow errors (but not CancellationException)
                    if (e !is CancellationException) {
                        emit(StreamEvent.Error(e.message ?: "Unknown error"))
                    }
                }.collect { event ->
                    when (event) {
                        is StreamEvent.Started -> {
                            // Stream started
                        }

                        is StreamEvent.Content -> {
                            tokenCount++
                            _currentContent.update { it + event.text }
                            updateStreamingMessage()
                        }

                        is StreamEvent.Reasoning -> {
                            _currentThinking.update { it + event.text }
                            updateStreamingMessage()
                        }

                        is StreamEvent.Usage -> {
                            val elapsed = System.currentTimeMillis() - streamStartTime
                            val tokensPerSec = if (elapsed > 0) {
                                (event.usage.completionTokens ?: tokenCount).toFloat() / (elapsed / 1000f)
                            } else 0f

                            _inferenceStats.update {
                                it.copy(
                                    promptTokens = event.usage.promptTokens ?: 0,
                                    completionTokens = event.usage.completionTokens ?: 0,
                                    totalTokens = event.usage.totalTokens ?: 0,
                                    tokensPerSecond = tokensPerSec
                                )
                            }
                        }

                        is StreamEvent.Timings -> {
                            // llama-serve timings - update inference stats
                            _inferenceStats.update {
                                it.copy(
                                    promptTokens = event.timings.promptN ?: it.promptTokens,
                                    completionTokens = event.timings.predictedN ?: it.completionTokens,
                                    totalTokens = (event.timings.promptN ?: 0) + (event.timings.predictedN ?: 0),
                                    tokensPerSecond = event.timings.predictedPerSecond ?: it.tokensPerSecond
                                )
                            }
                        }

                        is StreamEvent.Done -> {
                            _inferenceStats.update {
                                it.copy(
                                    timeToFirstToken = event.timeToFirstToken,
                                    totalTime = event.totalTime
                                )
                            }
                            finalizeMessage()
                        }

                        is StreamEvent.Error -> {
                            _error.value = event.message
                            finalizeMessage()
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Job was cancelled (user cancelled, app backgrounded, etc.)
                // Finalize message with whatever content we have, but don't show error
                finalizeMessage()
                throw e // Re-throw to properly cancel
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                finalizeMessage()
            }
        }
    }

    private fun updateStreamingMessage() {
        _messages.update { messages ->
            messages.mapIndexed { index, message ->
                if (index == messages.lastIndex && message.isStreaming) {
                    message.copy(
                        content = _currentContent.value,
                        thinkingContent = _currentThinking.value.takeIf { it.isNotBlank() }
                    )
                } else {
                    message
                }
            }
        }
    }

    private fun finalizeMessage() {
        _isStreaming.value = false

        _messages.update { messages ->
            messages.mapIndexed { index, message ->
                if (index == messages.lastIndex && message.isStreaming) {
                    message.copy(
                        content = _currentContent.value,
                        thinkingContent = _currentThinking.value.takeIf { it.isNotBlank() },
                        isStreaming = false
                    )
                } else {
                    message
                }
            }
        }

        // Calculate final stats if not received from server
        if (_inferenceStats.value.tokensPerSecond == 0f && tokenCount > 0) {
            val elapsed = System.currentTimeMillis() - streamStartTime
            _inferenceStats.update {
                it.copy(
                    completionTokens = tokenCount,
                    tokensPerSecond = tokenCount.toFloat() / (elapsed / 1000f),
                    totalTime = elapsed
                )
            }
        }
    }

    fun cancelStreaming() {
        streamingJob?.cancel()
        finalizeMessage()
    }

    fun clearMessages() {
        _messages.value = emptyList()
        _inferenceStats.value = InferenceStats()
    }

    fun clearError() {
        _error.value = null
    }

    fun clearConnectionError() {
        _connectionError.value = null
    }

    fun toggleShowThinking() {
        _showThinking.update { !it }
    }

    fun toggleShowStats() {
        _showStats.update { !it }
    }

    fun updateModelSettings(settings: ModelSettings) {
        _modelSettings.value = settings
        viewModelScope.launch {
            settingsDataStore.saveModelSettings(settings)
        }
    }

    fun updateTemperature(value: Float) {
        _modelSettings.update { it.copy(temperature = value) }
        saveModelSettings()
    }

    fun updateMaxTokens(value: Int) {
        _modelSettings.update { it.copy(maxTokens = value) }
        saveModelSettings()
    }

    fun updateTopP(value: Float) {
        _modelSettings.update { it.copy(topP = value) }
        saveModelSettings()
    }

    fun updateFrequencyPenalty(value: Float) {
        _modelSettings.update { it.copy(frequencyPenalty = value) }
        saveModelSettings()
    }

    fun updatePresencePenalty(value: Float) {
        _modelSettings.update { it.copy(presencePenalty = value) }
        saveModelSettings()
    }

    fun updateSystemPrompt(prompt: String) {
        _modelSettings.update { it.copy(systemPrompt = prompt) }
        saveModelSettings()
    }

    fun updateReasoningEffort(effort: ReasoningEffort) {
        _modelSettings.update { it.copy(reasoningEffort = effort) }
        saveModelSettings()
    }

    private fun saveModelSettings() {
        viewModelScope.launch {
            settingsDataStore.saveModelSettings(_modelSettings.value)
        }
    }

    fun resetModelSettings() {
        val currentSystemPrompt = _modelSettings.value.systemPrompt
        _modelSettings.value = ModelSettings(systemPrompt = currentSystemPrompt)
        saveModelSettings()
    }

    fun exportToMarkdown(): String {
        val messages = _messages.value
        if (messages.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("# HexAI Chat Export")
        sb.appendLine()
        sb.appendLine("*Exported: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}*")
        sb.appendLine()

        for (message in messages) {
            sb.appendLine("---")
            sb.appendLine()
            val roleLabel = when (message.role) {
                MessageRole.USER -> "User"
                MessageRole.ASSISTANT -> "Assistant"
                MessageRole.SYSTEM -> "System"
            }
            sb.appendLine("## $roleLabel")
            sb.appendLine()
            sb.appendLine(message.content)
            sb.appendLine()

            if (!message.thinkingContent.isNullOrBlank()) {
                sb.appendLine("<details>")
                sb.appendLine("<summary>Thinking</summary>")
                sb.appendLine()
                sb.appendLine(message.thinkingContent)
                sb.appendLine()
                sb.appendLine("</details>")
                sb.appendLine()
            }
        }

        return sb.toString()
    }

    fun importFromMarkdown(content: String): Boolean {
        try {
            val newMessages = mutableListOf<Message>()
            val sections = content.split(Regex("(?=^---$)", RegexOption.MULTILINE))

            for (section in sections) {
                val trimmed = section.trim()
                if (trimmed.isEmpty() || trimmed == "---") continue

                // Extract role from ## header
                val roleMatch = Regex("^##\\s+(User|Assistant|System)", RegexOption.MULTILINE).find(trimmed)
                if (roleMatch != null) {
                    val role = when (roleMatch.groupValues[1]) {
                        "User" -> MessageRole.USER
                        "Assistant" -> MessageRole.ASSISTANT
                        "System" -> MessageRole.SYSTEM
                        else -> continue
                    }

                    // Get content after the role header
                    var messageContent = trimmed.substringAfter(roleMatch.value).trim()

                    // Extract thinking content if present
                    var thinkingContent: String? = null
                    val thinkingMatch = Regex("<details>\\s*<summary>Thinking</summary>([\\s\\S]*?)</details>", RegexOption.MULTILINE).find(messageContent)
                    if (thinkingMatch != null) {
                        thinkingContent = thinkingMatch.groupValues[1].trim()
                        messageContent = messageContent.replace(thinkingMatch.value, "").trim()
                    }

                    if (messageContent.isNotEmpty()) {
                        newMessages.add(
                            Message(
                                role = role,
                                content = messageContent,
                                thinkingContent = thinkingContent
                            )
                        )
                    }
                }
            }

            if (newMessages.isNotEmpty()) {
                _messages.value = newMessages
                return true
            }
            return false
        } catch (e: Exception) {
            _error.value = "Failed to import: ${e.message}"
            return false
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
        healthCheckJob?.cancel()
    }
}
