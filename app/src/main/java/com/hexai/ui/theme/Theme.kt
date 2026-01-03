package com.hexai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val HexColorScheme = darkColorScheme(
    primary = HexGreen,
    onPrimary = HexBlack,
    primaryContainer = HexMidGrey,
    onPrimaryContainer = HexGreen,

    secondary = HexGrey300,
    onSecondary = HexBlack,
    secondaryContainer = HexMidGrey,
    onSecondaryContainer = HexGrey200,

    tertiary = HexGrey400,
    onTertiary = HexBlack,
    tertiaryContainer = HexMidGrey,
    onTertiaryContainer = HexGrey300,

    error = HexError,
    onError = HexBlack,
    errorContainer = HexMidGrey,
    onErrorContainer = HexError,

    background = HexBlack,
    onBackground = HexTextPrimary,

    surface = HexDarkGrey,
    onSurface = HexTextPrimary,
    surfaceVariant = HexMidGrey,
    onSurfaceVariant = HexTextSecondary,

    outline = HexGrey400,
    outlineVariant = HexGrey500,

    inverseSurface = HexGrey100,
    inverseOnSurface = HexBlack,
    inversePrimary = HexGreenDim
)

@Composable
fun HexAITheme(
    content: @Composable () -> Unit
) {
    val colorScheme = HexColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = HexBlack.toArgb()
            window.navigationBarColor = HexBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HexTypography,
        content = content
    )
}

// Legacy alias
@Composable
fun CyberChatTheme(content: @Composable () -> Unit) = HexAITheme(content)
