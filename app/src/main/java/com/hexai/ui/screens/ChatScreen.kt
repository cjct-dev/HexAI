package com.hexai.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hexai.data.api.Message
import com.hexai.ui.components.*
import com.hexai.ui.theme.*
import com.hexai.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val showThinking by viewModel.showThinking.collectAsState()
    val showStats by viewModel.showStats.collectAsState()
    val inferenceStats by viewModel.inferenceStats.collectAsState()
    val error by viewModel.error.collectAsState()
    val serverConfig by viewModel.serverConfig.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let {
            val markdown = viewModel.exportToMarkdown()
            if (markdown.isNotEmpty()) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(markdown.toByteArray())
                }
            }
        }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                viewModel.importFromMarkdown(content)
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.background(DarkBackground),
        containerColor = DarkBackground,
        topBar = {
            HexTopBar(
                modelName = serverConfig.selectedModel.takeIf { it.isNotBlank() } ?: "No model",
                showThinking = showThinking,
                showStats = showStats,
                hasMessages = messages.isNotEmpty(),
                isConnected = isConnected,
                onToggleThinking = { viewModel.toggleShowThinking() },
                onToggleStats = { viewModel.toggleShowStats() },
                onNavigateToSettings = onNavigateToSettings,
                onClearChat = { viewModel.clearMessages() },
                onExportChat = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    exportLauncher.launch("hexai_$timestamp.md")
                },
                onImportChat = { importLauncher.launch("text/*") }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.navigationBarsPadding()
            ) {
                // Stats panel
                StatsPanel(
                    stats = inferenceStats,
                    isVisible = showStats
                )

                // Input area
                ChatInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank() && !isStreaming) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onCancel = { viewModel.cancelStreaming() },
                    isStreaming = isStreaming,
                    enabled = serverConfig.selectedModel.isNotBlank()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (messages.isEmpty()) {
                EmptyStateView(
                    hasModel = serverConfig.selectedModel.isNotBlank(),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            showThinking = showThinking
                        )
                    }
                }
            }

            // Error snackbar
            AnimatedVisibility(
                visible = error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                error?.let { errorMessage ->
                    Surface(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = ErrorRed.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HexTopBar(
    modelName: String,
    showThinking: Boolean,
    showStats: Boolean,
    hasMessages: Boolean,
    isConnected: Boolean,
    onToggleThinking: () -> Unit,
    onToggleStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onClearChat: () -> Unit,
    onExportChat: () -> Unit,
    onImportChat: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlitchText(
                    text = "HEXAI",
                    style = MaterialTheme.typography.titleMedium,
                    color = HexGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Connection status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isConnected) HexGreen else HexGrey400)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = HexTextMuted,
                    maxLines = 1
                )
            }
        },
        actions = {
            // Toggle thinking visibility
            IconButton(onClick = onToggleThinking) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Toggle thinking",
                    tint = if (showThinking) HexGreen else HexGrey400
                )
            }

            // Toggle stats visibility
            IconButton(onClick = onToggleStats) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Toggle stats",
                    tint = if (showStats) HexGreen else HexGrey400
                )
            }

            // More menu (import/export/clear)
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = HexGrey300
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(HexCard)
                ) {
                    DropdownMenuItem(
                        text = { Text("Import Chat", color = HexTextPrimary) },
                        onClick = {
                            showMenu = false
                            onImportChat()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = HexGreen
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Chat", color = if (hasMessages) HexTextPrimary else HexTextMuted) },
                        onClick = {
                            if (hasMessages) {
                                showMenu = false
                                onExportChat()
                            }
                        },
                        enabled = hasMessages,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = null,
                                tint = if (hasMessages) HexGrey200 else HexGrey400
                            )
                        }
                    )
                    HorizontalDivider(color = HexGrey500)
                    DropdownMenuItem(
                        text = { Text("Clear Chat", color = if (hasMessages) HexError else HexTextMuted) },
                        onClick = {
                            if (hasMessages) {
                                showMenu = false
                                onClearChat()
                            }
                        },
                        enabled = hasMessages,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = if (hasMessages) HexError else HexGrey400
                            )
                        }
                    )
                }
            }

            // Settings
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = HexGrey200
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkSurface,
            titleContentColor = TextPrimary
        )
    )
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    isStreaming: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            NeonCyan.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 150.dp),
                placeholder = {
                    Text(
                        text = if (enabled) "Enter message..." else "Select a model first",
                        color = TextMuted
                    )
                },
                enabled = enabled && !isStreaming,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberGray400,
                    disabledBorderColor = CyberGray500,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    disabledContainerColor = DarkCard,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isStreaming) {
                FloatingActionButton(
                    onClick = onCancel,
                    containerColor = ErrorRed,
                    contentColor = DarkBackground,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop"
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = onSend,
                    containerColor = NeonCyan,
                    contentColor = DarkBackground,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    hasModel: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = CyberGray400
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasModel) "START A CONVERSATION" else "CONFIGURE YOUR CONNECTION",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasModel) {
                "Type a message below to begin chatting with the AI"
            } else {
                "Go to settings to connect to a server and select a model"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}
