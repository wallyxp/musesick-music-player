package com.wally.musesick.model

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Data model representing a structured theme palette with all UI color roles.
 */
data class ThemePalette(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color = outline.copy(alpha = 0.35f),
    val error: Color = Color(0xFFBA1A1A),
    val onError: Color = Color.White
) {
    fun toColorScheme(): ColorScheme {
        return if (isDark) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary,
                onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = onTertiaryContainer,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                outline = outline,
                outlineVariant = outlineVariant,
                error = error,
                onError = onError
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary,
                onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = onTertiaryContainer,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                outline = outline,
                outlineVariant = outlineVariant,
                error = error,
                onError = onError
            )
        }
    }
}

object ThemePalettes {
    // AQUA PALETTES (Darugami & Damegami replicating Custom Image theme with signature accents)
    private val AQUA_DARUGAMI = ThemePalette(
        id = "darugami",
        displayName = "Darugami",
        isDark = true,
        background = Color.Transparent,
        surface = Color(0xB3181818),
        surfaceVariant = Color(0x4D2A2A2A),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFDDDDDD),
        primary = Color(0xFFFFB74D), // Aqua signature warm radiant gold
        onPrimary = Color.Black,
        primaryContainer = Color(0x4DFFB74D),
        onPrimaryContainer = Color(0xFFFFE082),
        secondary = Color(0xFF81D4FA), // Celestial Aqua ice-blue
        onSecondary = Color.Black,
        secondaryContainer = Color(0x3381D4FA),
        onSecondaryContainer = Color(0xFFE1F5FE),
        tertiary = Color(0xFFA5D6A7),
        onTertiary = Color.Black,
        tertiaryContainer = Color(0x33A5D6A7),
        onTertiaryContainer = Color(0xFFE8F5E9),
        outline = Color(0x40FFFFFF),
        outlineVariant = Color(0x26FFFFFF),
        error = Color(0xFFFF5252),
        onError = Color.White
    )

    private val AQUA_DAMEGAMI = ThemePalette(
        id = "damegami",
        displayName = "Damegami",
        isDark = true,
        background = Color.Transparent,
        surface = Color(0xB3181818),
        surfaceVariant = Color(0x4D2A2A2A),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFDDDDDD),
        primary = Color(0xFF4FC3F7), // Aqua divine radiant sky-blue
        onPrimary = Color.Black,
        primaryContainer = Color(0x4D4FC3F7),
        onPrimaryContainer = Color(0xFFE1F5FE),
        secondary = Color(0xFFFFD54F),
        onSecondary = Color.Black,
        secondaryContainer = Color(0x33FFD54F),
        onSecondaryContainer = Color(0xFFFFF8E1),
        tertiary = Color(0xFFCE93D8),
        onTertiary = Color.Black,
        tertiaryContainer = Color(0x33CE93D8),
        onTertiaryContainer = Color(0xFFF3E5F5),
        outline = Color(0x40FFFFFF),
        outlineVariant = Color(0x26FFFFFF),
        error = Color(0xFFFF5252),
        onError = Color.White
    )

    private val PALETTES: Map<AppTheme, Map<String, ThemePalette>> = mapOf(
        AppTheme.AQUA to mapOf(
            "darugami" to AQUA_DARUGAMI,
            "damegami" to AQUA_DAMEGAMI
        )
    )

    fun getPalette(theme: AppTheme, variant: String?): ThemePalette? {
        val themeMap = PALETTES[theme] ?: return null
        val targetVariant = if (variant.isNullOrBlank()) theme.defaultVariant else variant
        return themeMap[targetVariant] ?: themeMap[theme.defaultVariant] ?: themeMap.values.firstOrNull()
    }
}
