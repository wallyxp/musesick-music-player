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
        surface = Color(0xB3151B2E),
        surfaceVariant = Color(0x4D1F2640),
        onBackground = Color(0xFFE6EEF8),
        onSurface = Color(0xFFF0F5FC),
        onSurfaceVariant = Color(0xFFBAC2DC),
        primary = Color(0xFFBFEBF8),
        onPrimary = Color(0xFF0F2636),
        primaryContainer = Color(0xFF3D4171),
        onPrimaryContainer = Color(0xFFBFEBF8),
        secondary = Color(0xFF4D5596),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF353C6F),
        onSecondaryContainer = Color(0xFFDDE2FF),
        tertiary = Color(0xFF84E4B6),
        onTertiary = Color(0xFF003822),
        tertiaryContainer = Color(0xFF005133),
        onTertiaryContainer = Color(0xFFA0F2CD),
        outline = Color(0xFF3E4770),
        outlineVariant = Color(0x33BFEBF8),
        error = Color(0xFFFF5449),
        onError = Color.White
    )

    private val AQUA_DAMEGAMI = ThemePalette(
        id = "damegami",
        displayName = "Damegami",
        isDark = true,
        background = Color.Transparent,
        surface = Color(0xB31C1C2C),
        surfaceVariant = Color(0x4D28283E),
        onBackground = Color(0xFFF5F2EA),
        onSurface = Color(0xFFFBF8F2),
        onSurfaceVariant = Color(0xFFCBC6B8),
        primary = Color(0xFFFCCC7A),
        onPrimary = Color(0xFF3E2D00),
        primaryContainer = Color(0xFF5B4300),
        onPrimaryContainer = Color(0xFFFFDF9E),
        secondary = Color(0xFF3D4171),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF282B4E),
        onSecondaryContainer = Color(0xFFDDE2FF),
        tertiary = Color(0xFF84E4B6),
        onTertiary = Color(0xFF003822),
        tertiaryContainer = Color(0xFF005133),
        onTertiaryContainer = Color(0xFFA0F2CD),
        outline = Color(0xFF6B5838),
        outlineVariant = Color(0x33FCCC7A),
        error = Color(0xFFFF5449),
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
