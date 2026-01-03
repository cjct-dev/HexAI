package com.hexai.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hexai.data.api.InferenceStats
import com.hexai.ui.theme.*

@Composable
fun StatsPanel(
    stats: InferenceStats,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && stats.totalTokens > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                NeonCyan.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INFERENCE STATS",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(
                    color = CyberGray500,
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.Input,
                        label = "Prompt",
                        value = "${stats.promptTokens}",
                        color = NeonCyan
                    )
                    StatItem(
                        icon = Icons.Default.Output,
                        label = "Completion",
                        value = "${stats.completionTokens}",
                        color = NeonMagenta
                    )
                    StatItem(
                        icon = Icons.Default.Functions,
                        label = "Total",
                        value = "${stats.totalTokens}",
                        color = NeonPurple
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.Speed,
                        label = "Speed",
                        value = String.format("%.1f t/s", stats.tokensPerSecond),
                        color = NeonGreen
                    )
                    StatItem(
                        icon = Icons.Default.Timer,
                        label = "First Token",
                        value = "${stats.timeToFirstToken}ms",
                        color = NeonYellow
                    )
                    StatItem(
                        icon = Icons.Default.Schedule,
                        label = "Total Time",
                        value = formatTime(stats.totalTime),
                        color = NeonOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

private fun formatTime(milliseconds: Long): String {
    return when {
        milliseconds < 1000 -> "${milliseconds}ms"
        milliseconds < 60000 -> String.format("%.1fs", milliseconds / 1000f)
        else -> String.format("%.1fm", milliseconds / 60000f)
    }
}
