package com.hexai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hexai.data.api.Message
import com.hexai.data.api.MessageRole
import com.hexai.ui.theme.*

@Composable
fun MessageBubble(
    message: Message,
    showThinking: Boolean,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isUser) NeonCyan else NeonMagenta
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isUser) "YOU" else "AI",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) NeonCyan else NeonMagenta,
                fontWeight = FontWeight.Bold
            )
        }

        // Message bubble
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .then(
                    if (message.isStreaming) {
                        Modifier.border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(NeonCyan, NeonMagenta, NeonPurple)
                            ),
                            shape = bubbleShape
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = if (isUser) NeonCyan.copy(alpha = 0.3f) else NeonMagenta.copy(alpha = 0.3f),
                            shape = bubbleShape
                        )
                    }
                ),
            shape = bubbleShape,
            color = if (isUser) DarkCard else DarkSurfaceVariant
        ) {
            SelectionContainer {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Thinking block (collapsible)
                    if (!message.thinkingContent.isNullOrBlank()) {
                        ThinkingBlock(
                            content = message.thinkingContent,
                            isExpanded = showThinking,
                            isStreaming = message.isStreaming
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Main content
                    if (message.content.isNotBlank()) {
                        if (isUser) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        } else {
                            MarkdownText(
                                text = message.content,
                                textColor = TextPrimary
                            )
                        }
                    }

                    // Streaming indicator
                    if (message.isStreaming && message.content.isEmpty() && message.thinkingContent.isNullOrBlank()) {
                        StreamingIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingBlock(
    content: String,
    isExpanded: Boolean,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    var localExpanded by remember(isExpanded) { mutableStateOf(isExpanded) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = DarkBackground.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            NeonPurple.copy(alpha = 0.4f)
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { localExpanded = !localExpanded }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NeonPurple
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "THINKING",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurple,
                        fontWeight = FontWeight.Bold
                    )
                    if (isStreaming) {
                        Spacer(modifier = Modifier.width(8.dp))
                        PulsingDot(color = NeonPurple)
                    }
                }
                Icon(
                    imageVector = if (localExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = NeonPurple
                )
            }

            // Content
            AnimatedVisibility(
                visible = localExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 8.dp,
                        end = 8.dp,
                        bottom = 8.dp
                    )
                ) {
                    Divider(
                        color = NeonPurple.copy(alpha = 0.2f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun StreamingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonCyan.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = alpha))
    )
}
