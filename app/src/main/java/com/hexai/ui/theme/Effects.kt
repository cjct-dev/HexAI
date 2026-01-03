package com.hexai.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// Neon glow effect modifier
fun Modifier.neonGlow(
    color: Color = NeonCyan,
    radius: Dp = 8.dp,
    alpha: Float = 0.6f
): Modifier = this
    .drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    color.copy(alpha = alpha * 0.5f),
                    Color.Transparent
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension
            )
        )
    }

// Neon border effect
fun Modifier.neonBorder(
    color: Color = NeonCyan,
    width: Dp = 1.dp,
    shape: Shape = RectangleShape,
    glowRadius: Dp = 4.dp
): Modifier = this
    .border(width, color.copy(alpha = 0.8f), shape)
    .drawBehind {
        val strokeWidth = width.toPx()
        drawRect(
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = strokeWidth + glowRadius.toPx())
        )
    }

// Scanline overlay effect
@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    lineSpacing: Dp = 3.dp,
    lineOpacity: Float = 0.08f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = lineSpacing.value * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlineOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val spacingPx = lineSpacing.toPx()
                var y = offsetY
                while (y < size.height) {
                    drawLine(
                        color = Color.Black.copy(alpha = lineOpacity),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += spacingPx
                }
            }
    )
}

// Glitch effect composable
@Composable
fun Modifier.glitchEffect(enabled: Boolean = false): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "glitch")

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0
                0f at 1800
                Random.nextFloat() * 10 - 5 at 1850
                Random.nextFloat() * -10 + 5 at 1900
                0f at 1950
                0f at 2000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "glitchX"
    )

    return this.offset(x = offsetX.dp)
}

// Pulsing animation for buttons/elements
@Composable
fun pulsingAlpha(
    minAlpha: Float = 0.7f,
    maxAlpha: Float = 1f,
    duration: Int = 1500
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    return alpha
}

// Typing cursor blink animation
@Composable
fun blinkingCursor(): Boolean {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(530)
            visible = !visible
        }
    }

    return visible
}

// Gradient text brush
fun neonGradientBrush(
    colors: List<Color> = listOf(HexGreen, HexGrey300, HexGrey400)
): Brush = Brush.linearGradient(colors = colors)

fun hexGradientBrush(
    colors: List<Color> = listOf(HexGreen, HexGreenMuted, HexGrey400)
): Brush = Brush.linearGradient(colors = colors)

// Matrix-style rain background modifier
fun Modifier.matrixBackground(
    color: Color = NeonGreen,
    density: Float = 0.3f
): Modifier = this.drawBehind {
    val characters = "01"
    val random = Random(System.currentTimeMillis() / 1000)

    for (x in 0 until (size.width / 20).toInt()) {
        if (random.nextFloat() > density) continue
        for (y in 0 until (size.height / 20).toInt()) {
            if (random.nextFloat() > 0.1f) continue
            drawContext.canvas.nativeCanvas.drawText(
                characters[random.nextInt(characters.length)].toString(),
                x * 20f,
                y * 20f,
                android.graphics.Paint().apply {
                    this.color = color.copy(alpha = random.nextFloat() * 0.3f).toArgb()
                    textSize = 14f
                    typeface = android.graphics.Typeface.MONOSPACE
                }
            )
        }
    }
}
