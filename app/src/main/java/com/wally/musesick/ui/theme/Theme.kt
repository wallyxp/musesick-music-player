package com.wally.musesick.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.ThemePalettes

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline
)

fun createCustomDarkColorScheme(primaryColor: Color): ColorScheme {
    return createCustomColorGlassColorScheme(primaryColor)
}

fun createCustomColorGlassColorScheme(accentColor: Color): ColorScheme {
    return darkColorScheme(
        primary = accentColor,
        onPrimary = Color.Black,
        primaryContainer = accentColor.copy(alpha = 0.35f),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFE0E0E0),
        onSecondary = Color.Black,
        secondaryContainer = Color(0x33FFFFFF),
        onSecondaryContainer = Color.White,
        tertiary = Color(0xFFB0BEC5),
        onTertiary = Color.Black,
        tertiaryContainer = Color(0x29FFFFFF),
        onTertiaryContainer = Color.White,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        errorContainer = md_theme_dark_errorContainer,
        onErrorContainer = md_theme_dark_onErrorContainer,
        background = Color.Transparent,
        onBackground = Color.White,
        surface = Color(0xB3181818),
        onSurface = Color.White,
        surfaceVariant = Color(0x4D2A2A2A),
        onSurfaceVariant = Color(0xFFDDDDDD),
        outline = Color(0x40FFFFFF),
        outlineVariant = Color(0x26FFFFFF)
    )
}

fun createTimeGradientColorScheme(accentColor: Color): ColorScheme {
    return createCustomColorGlassColorScheme(accentColor)
}

fun createCustomImageColorScheme(): ColorScheme {
    return darkColorScheme(
        primary = Color.White,
        onPrimary = Color.Black,
        primaryContainer = Color(0x4DFFFFFF),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFE0E0E0),
        onSecondary = Color.Black,
        secondaryContainer = Color(0x33FFFFFF),
        onSecondaryContainer = Color.White,
        tertiary = Color(0xFFB0BEC5),
        onTertiary = Color.Black,
        tertiaryContainer = Color(0x29FFFFFF),
        onTertiaryContainer = Color.White,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        errorContainer = md_theme_dark_errorContainer,
        onErrorContainer = md_theme_dark_onErrorContainer,
        background = Color.Transparent,
        onBackground = Color.White,
        surface = Color(0xB3181818),
        onSurface = Color.White,
        surfaceVariant = Color(0x4D2A2A2A),
        onSurfaceVariant = Color(0xFFDDDDDD),
        outline = Color(0x40FFFFFF),
        outlineVariant = Color(0x26FFFFFF)
    )
}

@Composable
fun MusesickAppTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    appThemeVariant: String = "",
    customAccentColor: Color = Color(0xFFFFA000),
    content: @Composable () -> Unit
) {
    val currentHour = remember {
        TimeThemeHelper.getCurrentHour()
    }
    val colorScheme = remember(appTheme, appThemeVariant, customAccentColor, currentHour) {
        when (appTheme) {
            AppTheme.DEFAULT -> createTimeGradientColorScheme(TimeThemeHelper.getAccentColor(currentHour))
            AppTheme.CUSTOM_COLOR -> createCustomColorGlassColorScheme(customAccentColor)
            AppTheme.CUSTOM_IMAGE -> createCustomImageColorScheme()
            AppTheme.AQUA -> {
                val palette = ThemePalettes.getPalette(AppTheme.AQUA, appThemeVariant)
                palette?.toColorScheme() ?: createCustomImageColorScheme()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MusesickThemed(
    theme: AppTheme,
    variant: String? = null,
    customAccentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val currentHour = remember {
        TimeThemeHelper.getCurrentHour()
    }
    val colorScheme = remember(theme, variant, customAccentColor, currentHour) {
        when (theme) {
            AppTheme.DEFAULT -> createTimeGradientColorScheme(TimeThemeHelper.getAccentColor(currentHour))
            AppTheme.CUSTOM_COLOR -> createCustomColorGlassColorScheme(customAccentColor ?: Color(0xFFFFA000))
            AppTheme.CUSTOM_IMAGE -> createCustomImageColorScheme()
            AppTheme.AQUA -> {
                val palette = ThemePalettes.getPalette(theme, variant)
                palette?.toColorScheme() ?: createCustomImageColorScheme()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MaterialYouMusicTheme(
    content: @Composable () -> Unit
) {
    MusesickAppTheme(
        appTheme = AppTheme.DEFAULT,
        content = content
    )
}

