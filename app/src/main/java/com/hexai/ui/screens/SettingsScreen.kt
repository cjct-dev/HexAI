package com.hexai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hexai.data.api.ReasoningEffort
import com.hexai.ui.components.*
import com.hexai.ui.theme.*
import com.hexai.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val serverConfig by viewModel.serverConfig.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()
    val modelSettings by viewModel.modelSettings.collectAsState()
    val pingLatency by viewModel.pingLatency.collectAsState()
    val supportsModelManagement by viewModel.supportsModelManagement.collectAsState()
    val isSecureConnection by viewModel.isSecureConnection.collectAsState()

    Scaffold(
        modifier = modifier.background(DarkBackground),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    GlitchText(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonMagenta
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Section
            ConnectionSection(
                serverUrl = serverConfig.url,
                apiKey = serverConfig.apiKey,
                isConnected = isConnected,
                isConnecting = isConnecting,
                connectionError = connectionError,
                pingLatency = pingLatency,
                isSecure = isSecureConnection,
                onUrlChange = { viewModel.updateServerUrl(it) },
                onApiKeyChange = { viewModel.updateApiKey(it) },
                onConnect = { viewModel.connect() },
                onDisconnect = { viewModel.disconnect() },
                onClearError = { viewModel.clearConnectionError() }
            )

            // Model Selection
            AnimatedVisibility(
                visible = isConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ModelSelectionSection(
                    models = availableModels.map { it.id },
                    selectedModel = serverConfig.selectedModel,
                    isLoading = isLoadingModels,
                    onModelSelected = { viewModel.selectModel(it) }
                )
            }

            // Model Settings
            AnimatedVisibility(
                visible = isConnected && serverConfig.selectedModel.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModelParametersSection(
                        temperature = modelSettings.temperature,
                        maxTokens = modelSettings.maxTokens,
                        topP = modelSettings.topP,
                        frequencyPenalty = modelSettings.frequencyPenalty,
                        presencePenalty = modelSettings.presencePenalty,
                        onTemperatureChange = { viewModel.updateTemperature(it) },
                        onMaxTokensChange = { viewModel.updateMaxTokens(it) },
                        onTopPChange = { viewModel.updateTopP(it) },
                        onFrequencyPenaltyChange = { viewModel.updateFrequencyPenalty(it) },
                        onPresencePenaltyChange = { viewModel.updatePresencePenalty(it) },
                        onResetToDefaults = { viewModel.resetModelSettings() }
                    )

                    ReasoningSection(
                        reasoningEffort = modelSettings.reasoningEffort,
                        onReasoningEffortChange = { viewModel.updateReasoningEffort(it) }
                    )

                    ResponseModeSection(
                        useStreaming = modelSettings.useStreaming,
                        onStreamingChange = { viewModel.updateUseStreaming(it) }
                    )

                    SystemPromptSection(
                        systemPrompt = modelSettings.systemPrompt,
                        onSystemPromptChange = { viewModel.updateSystemPrompt(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ConnectionSection(
    serverUrl: String,
    apiKey: String,
    isConnected: Boolean,
    isConnecting: Boolean,
    connectionError: String?,
    pingLatency: Long?,
    isSecure: Boolean,
    onUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onClearError: () -> Unit
) {
    CyberpunkCard(accentColor = if (isConnected) HexGreen else HexGreen) {
        // Header
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.Cloud,
                    contentDescription = null,
                    tint = if (isConnected) HexGreen else HexGrey300,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SERVER CONNECTION",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isConnected) HexGreen else HexGrey200,
                    fontWeight = FontWeight.Bold
                )
            }

            // Connection status badges on separate line
            if (isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Connection status
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HexGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "CONNECTED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = HexGreen
                        )
                    }

                    // Encryption status indicator
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSecure) HexGreen.copy(alpha = 0.15f) else WarningYellow.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isSecure) "Secure connection" else "Insecure connection",
                                tint = if (isSecure) HexGreen else WarningYellow,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSecure) "HTTPS" else "HTTP",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSecure) HexGreen else WarningYellow
                            )
                        }
                    }

                    // Ping latency
                    pingLatency?.let { latency ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HexGrey400.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "${latency}ms",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = HexGrey200
                            )
                        }
                    }
                }
            }
        }

        NeonDivider(color = if (isConnected) SuccessGreen else NeonCyan)
        Spacer(modifier = Modifier.height(16.dp))

        // Server URL
        CyberpunkTextField(
            value = serverUrl,
            onValueChange = onUrlChange,
            label = "Server URL",
            placeholder = "192.168.1.100:8080",
            enabled = !isConnected,
            accentColor = NeonCyan,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = CyberGray400
                )
            }
        )

        // Helper text about auto-detection
        if (!isConnected) {
            Text(
                text = "Protocol (https/http) auto-detected if not specified",
                style = MaterialTheme.typography.labelSmall,
                color = HexTextMuted,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // API Key
        CyberpunkTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = "API Key (optional)",
            placeholder = "sk-...",
            isPassword = true,
            enabled = !isConnected,
            accentColor = NeonMagenta,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = CyberGray400
                )
            }
        )

        // Error message
        AnimatedVisibility(
            visible = connectionError != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            connectionError?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = ErrorRed.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearError,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = ErrorRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connect/Disconnect button
        if (isConnected) {
            CyberpunkButton(
                onClick = onDisconnect,
                accentColor = ErrorRed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LinkOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("DISCONNECT")
            }
        } else {
            CyberpunkButton(
                onClick = onConnect,
                enabled = serverUrl.isNotBlank() && !isConnecting,
                accentColor = NeonCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = NeonCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CONNECTING...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CONNECT")
                }
            }
        }
    }
}

@Composable
private fun ModelSelectionSection(
    models: List<String>,
    selectedModel: String,
    isLoading: Boolean,
    onModelSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredModels = remember(models, searchQuery) {
        if (searchQuery.isBlank()) models
        else models.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    CyberpunkCard(accentColor = HexGrey300) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = HexGrey200,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MODEL SELECTION",
                style = MaterialTheme.typography.titleSmall,
                color = HexGrey200,
                fontWeight = FontWeight.Bold
            )
        }

        NeonDivider(color = HexGrey300)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = HexGreen,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Loading models...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HexTextSecondary
                )
            }
        } else if (models.isEmpty()) {
            Text(
                text = "No models available",
                style = MaterialTheme.typography.bodyMedium,
                color = HexTextMuted
            )
        } else {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search models...", color = HexTextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = HexGrey400
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = HexGrey400
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HexTextPrimary,
                    unfocusedTextColor = HexTextPrimary,
                    focusedBorderColor = HexGreen,
                    unfocusedBorderColor = HexGrey400,
                    focusedContainerColor = HexCard,
                    unfocusedContainerColor = HexCard,
                    cursorColor = HexGreen
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredModels.isEmpty()) {
                Text(
                    text = "No models match \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HexTextMuted
                )
            } else {
                CyberpunkDropdown(
                    items = filteredModels,
                    selectedItem = if (filteredModels.contains(selectedModel)) selectedModel
                                   else filteredModels.firstOrNull() ?: "",
                    onItemSelected = onModelSelected,
                    accentColor = HexGreen
                )
            }
        }
    }
}

@Composable
private fun ModelParametersSection(
    temperature: Float,
    maxTokens: Int,
    topP: Float,
    frequencyPenalty: Float,
    presencePenalty: Float,
    onTemperatureChange: (Float) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
    onTopPChange: (Float) -> Unit,
    onFrequencyPenaltyChange: (Float) -> Unit,
    onPresencePenaltyChange: (Float) -> Unit,
    onResetToDefaults: () -> Unit
) {
    var showMaxTokensDialog by remember { mutableStateOf(false) }

    CyberpunkCard(accentColor = NeonPurple) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MODEL PARAMETERS",
                style = MaterialTheme.typography.titleSmall,
                color = NeonPurple,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            // Reset button
            Surface(
                onClick = onResetToDefaults,
                shape = RoundedCornerShape(4.dp),
                color = CyberGray500.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberGray400)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = CyberGray300,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RESET",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGray300
                    )
                }
            }
        }

        NeonDivider(color = NeonPurple)
        Spacer(modifier = Modifier.height(16.dp))

        // Temperature
        CyberpunkSliderWithInput(
            value = temperature,
            onValueChange = onTemperatureChange,
            valueRange = 0f..2f,
            label = "Temperature",
            valueLabel = String.format("%.2f", temperature),
            accentColor = NeonCyan,
            decimalPlaces = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Max Tokens - with clickable value
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Max Tokens",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Surface(
                    onClick = { showMaxTokensDialog = true },
                    shape = RoundedCornerShape(4.dp),
                    color = NeonMagenta.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "$maxTokens",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonMagenta,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Slider(
                value = maxTokens.toFloat(),
                onValueChange = { onMaxTokensChange(it.toInt()) },
                valueRange = 256f..8192f,
                steps = 31,
                colors = SliderDefaults.colors(
                    thumbColor = NeonMagenta,
                    activeTrackColor = NeonMagenta,
                    inactiveTrackColor = CyberGray500
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top P
        CyberpunkSliderWithInput(
            value = topP,
            onValueChange = onTopPChange,
            valueRange = 0f..1f,
            label = "Top P",
            valueLabel = String.format("%.2f", topP),
            accentColor = NeonGreen,
            decimalPlaces = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Frequency Penalty
        CyberpunkSliderWithInput(
            value = frequencyPenalty,
            onValueChange = onFrequencyPenaltyChange,
            valueRange = -2f..2f,
            label = "Frequency Penalty",
            valueLabel = String.format("%.2f", frequencyPenalty),
            accentColor = NeonYellow,
            decimalPlaces = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Presence Penalty
        CyberpunkSliderWithInput(
            value = presencePenalty,
            onValueChange = onPresencePenaltyChange,
            valueRange = -2f..2f,
            label = "Presence Penalty",
            valueLabel = String.format("%.2f", presencePenalty),
            accentColor = NeonOrange,
            decimalPlaces = 2
        )
    }

    // Max Tokens Dialog
    if (showMaxTokensDialog) {
        IntNumberInputDialog(
            currentValue = maxTokens,
            valueRange = 256..8192,
            label = "Max Tokens",
            accentColor = NeonMagenta,
            onDismiss = { showMaxTokensDialog = false },
            onConfirm = { newValue ->
                onMaxTokensChange(newValue)
                showMaxTokensDialog = false
            }
        )
    }
}

@Composable
private fun ReasoningSection(
    reasoningEffort: ReasoningEffort,
    onReasoningEffortChange: (ReasoningEffort) -> Unit
) {
    CyberpunkCard(accentColor = NeonBlue) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = NeonBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REASONING LEVEL",
                style = MaterialTheme.typography.titleSmall,
                color = NeonBlue,
                fontWeight = FontWeight.Bold
            )
        }

        NeonDivider(color = NeonBlue)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "For models that support extended thinking (e.g., Claude, o1)",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReasoningEffort.values().forEach { effort ->
                val isSelected = effort == reasoningEffort
                Surface(
                    onClick = { onReasoningEffortChange(effort) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) NeonBlue.copy(alpha = 0.2f) else DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) NeonBlue else CyberGray400
                    )
                ) {
                    Text(
                        text = effort.name,
                        modifier = Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) NeonBlue else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponseModeSection(
    useStreaming: Boolean,
    onStreamingChange: (Boolean) -> Unit
) {
    CyberpunkCard(accentColor = NeonOrange) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = NeonOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RESPONSE MODE",
                style = MaterialTheme.typography.titleSmall,
                color = NeonOrange,
                fontWeight = FontWeight.Bold
            )
        }

        NeonDivider(color = NeonOrange)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Choose how responses are delivered:",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Streaming option
            Surface(
                onClick = { onStreamingChange(true) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = if (useStreaming) NeonOrange.copy(alpha = 0.2f) else DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (useStreaming) NeonOrange else CyberGray400
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Stream,
                        contentDescription = null,
                        tint = if (useStreaming) NeonOrange else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "STREAM",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (useStreaming) NeonOrange else TextSecondary,
                        fontWeight = if (useStreaming) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "Real-time",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Bulk option
            Surface(
                onClick = { onStreamingChange(false) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = if (!useStreaming) NeonOrange.copy(alpha = 0.2f) else DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (!useStreaming) NeonOrange else CyberGray400
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = if (!useStreaming) NeonOrange else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "BULK",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (!useStreaming) NeonOrange else TextSecondary,
                        fontWeight = if (!useStreaming) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "Unstable network",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemPromptSection(
    systemPrompt: String,
    onSystemPromptChange: (String) -> Unit
) {
    CyberpunkCard(accentColor = NeonGreen) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SYSTEM PROMPT",
                style = MaterialTheme.typography.titleSmall,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
        }

        NeonDivider(color = NeonGreen)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = systemPrompt,
            onValueChange = onSystemPromptChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = {
                Text(
                    text = "Enter a system prompt to set the AI's behavior...",
                    color = TextMuted
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = CyberGray400,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard,
                cursorColor = NeonGreen
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}
