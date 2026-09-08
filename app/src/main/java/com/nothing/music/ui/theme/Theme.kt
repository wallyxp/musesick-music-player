package com.nothing.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = NothingTextPrimary,
    secondary = NothingBorderLight,
    onSecondary = NothingTextPrimary,
    background = NothingBlack,
    onBackground = NothingTextPrimary,
    surface = NothingSurface,
    onSurface = NothingTextPrimary,
    surfaceVariant = NothingCard,
    onSurfaceVariant = NothingTextSecondary,
    outline = NothingBorder
)

@Composable
fun NothingMusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

