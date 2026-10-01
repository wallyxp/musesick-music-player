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
                error = error,
                onError = onError
            )
        }
    }
}

object ThemePalettes {

    // 1. GRUVBOX
    private val GRUVBOX_DARK = ThemePalette(
        id = "dark",
        displayName = "Dark",
        isDark = true,
        background = Color(0xFF282828),
        surface = Color(0xFF3C3836),
        surfaceVariant = Color(0xFF504945),
        onBackground = Color(0xFFEBDBB2),
        onSurface = Color(0xFFFBF1C7),
        onSurfaceVariant = Color(0xFFD5C4A1),
        primary = Color(0xFFFE8019),
        onPrimary = Color(0xFF282828),
        primaryContainer = Color(0xFF504945),
        onPrimaryContainer = Color(0xFFFE8019),
        secondary = Color(0xFFFABD2F),
        onSecondary = Color(0xFF282828),
        secondaryContainer = Color(0xFF3C3836),
        onSecondaryContainer = Color(0xFFFABD2F),
        tertiary = Color(0xFF8EC07C),
        onTertiary = Color(0xFF282828),
        tertiaryContainer = Color(0xFF3C3836),
        onTertiaryContainer = Color(0xFF8EC07C),
        outline = Color(0xFF7C6F64),
        error = Color(0xFFFB4934),
        onError = Color(0xFF282828)
    )

    private val GRUVBOX_LIGHT = ThemePalette(
        id = "light",
        displayName = "Light",
        isDark = false,
        background = Color(0xFFFBF1C7),
        surface = Color(0xFFEBDBB2),
        surfaceVariant = Color(0xFFD5C4A1),
        onBackground = Color(0xFF3C3836),
        onSurface = Color(0xFF282828),
        onSurfaceVariant = Color(0xFF504945),
        primary = Color(0xFFD65D0E),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEBDBB2),
        onPrimaryContainer = Color(0xFFD65D0E),
        secondary = Color(0xFFB57614),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD5C4A1),
        onSecondaryContainer = Color(0xFFB57614),
        tertiary = Color(0xFF427B58),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD5C4A1),
        onTertiaryContainer = Color(0xFF427B58),
        outline = Color(0xFFA89984),
        error = Color(0xFFCC241D),
        onError = Color.White
    )

    // 2. EVERFOREST
    private fun createEverforestDark(id: String, displayName: String, bg0: Color, bg1: Color, bg2: Color, bg4: Color) = ThemePalette(
        id = id,
        displayName = displayName,
        isDark = true,
        background = bg0,
        surface = bg1,
        surfaceVariant = bg2,
        onBackground = Color(0xFFD3C6AA),
        onSurface = Color(0xFFD3C6AA),
        onSurfaceVariant = Color(0xFF9DA9A0),
        primary = Color(0xFFA7C080),
        onPrimary = bg0,
        primaryContainer = bg2,
        onPrimaryContainer = Color(0xFFA7C080),
        secondary = Color(0xFF83C092),
        onSecondary = bg0,
        secondaryContainer = bg1,
        onSecondaryContainer = Color(0xFF83C092),
        tertiary = Color(0xFFE69875),
        onTertiary = bg0,
        tertiaryContainer = bg1,
        onTertiaryContainer = Color(0xFFE69875),
        outline = bg4,
        error = Color(0xFFE67E80),
        onError = bg0
    )

    private fun createEverforestLight(id: String, displayName: String, bg0: Color, bg1: Color, bg2: Color, outlineColor: Color) = ThemePalette(
        id = id,
        displayName = displayName,
        isDark = false,
        background = bg0,
        surface = bg1,
        surfaceVariant = bg2,
        onBackground = Color(0xFF5C6A72),
        onSurface = Color(0xFF5C6A72),
        onSurfaceVariant = Color(0xFF829181),
        primary = Color(0xFF8DA101),
        onPrimary = Color.White,
        primaryContainer = bg1,
        onPrimaryContainer = Color(0xFF8DA101),
        secondary = Color(0xFF35A77C),
        onSecondary = Color.White,
        secondaryContainer = bg2,
        onSecondaryContainer = Color(0xFF35A77C),
        tertiary = Color(0xFFF57D26),
        onTertiary = Color.White,
        tertiaryContainer = bg2,
        onTertiaryContainer = Color(0xFFF57D26),
        outline = outlineColor,
        error = Color(0xFFF85552),
        onError = Color.White
    )

    // 3. TOKYO NIGHT
    private fun createTokyoNightDark(id: String, displayName: String, bg: Color, bgDark: Color, bgHighlight: Color, fg: Color, blue: Color, cyan: Color, magenta: Color, outlineColor: Color) = ThemePalette(
        id = id,
        displayName = displayName,
        isDark = true,
        background = bg,
        surface = bgDark,
        surfaceVariant = bgHighlight,
        onBackground = fg,
        onSurface = fg,
        onSurfaceVariant = Color(0xFFA9B1D6),
        primary = blue,
        onPrimary = bgDark,
        primaryContainer = bgHighlight,
        onPrimaryContainer = blue,
        secondary = cyan,
        onSecondary = bgDark,
        secondaryContainer = bgHighlight,
        onSecondaryContainer = cyan,
        tertiary = magenta,
        onTertiary = bgDark,
        tertiaryContainer = bgHighlight,
        onTertiaryContainer = magenta,
        outline = outlineColor,
        error = Color(0xFFF7768E),
        onError = bgDark
    )

    private val TOKYO_NIGHT_DAY = ThemePalette(
        id = "day",
        displayName = "Day",
        isDark = false,
        background = Color(0xFFE1E2E7),
        surface = Color(0xFFD0D5E3),
        surfaceVariant = Color(0xFFC4C8DA),
        onBackground = Color(0xFF3760BF),
        onSurface = Color(0xFF3760BF),
        onSurfaceVariant = Color(0xFF6172B0),
        primary = Color(0xFF2E7DE9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD0D5E3),
        onPrimaryContainer = Color(0xFF2E7DE9),
        secondary = Color(0xFF007197),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFC4C8DA),
        onSecondaryContainer = Color(0xFF007197),
        tertiary = Color(0xFF9854F1),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFC4C8DA),
        onTertiaryContainer = Color(0xFF9854F1),
        outline = Color(0xFFA1A6C5),
        error = Color(0xFFF52A65),
        onError = Color.White
    )

    // 4. CATPPUCCIN
    private fun createCatppuccinDark(id: String, displayName: String, base: Color, mantle: Color, surface0: Color, text: Color, mauve: Color, blue: Color, teal: Color, outlineColor: Color, crust: Color) = ThemePalette(
        id = id,
        displayName = displayName,
        isDark = true,
        background = base,
        surface = mantle,
        surfaceVariant = surface0,
        onBackground = text,
        onSurface = text,
        onSurfaceVariant = Color(0xFFBAC2DE),
        primary = mauve,
        onPrimary = crust,
        primaryContainer = surface0,
        onPrimaryContainer = mauve,
        secondary = blue,
        onSecondary = crust,
        secondaryContainer = mantle,
        onSecondaryContainer = blue,
        tertiary = teal,
        onTertiary = crust,
        tertiaryContainer = mantle,
        onTertiaryContainer = teal,
        outline = outlineColor,
        error = Color(0xFFF38BA8),
        onError = crust
    )

    private val CATPPUCCIN_LATTE = ThemePalette(
        id = "latte",
        displayName = "Latte",
        isDark = false,
        background = Color(0xFFEFF1F5),
        surface = Color(0xFFE6E9EF),
        surfaceVariant = Color(0xFFCCD0DA),
        onBackground = Color(0xFF4C4F69),
        onSurface = Color(0xFF4C4F69),
        onSurfaceVariant = Color(0xFF6C6F85),
        primary = Color(0xFF8839EF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE6E9EF),
        onPrimaryContainer = Color(0xFF8839EF),
        secondary = Color(0xFF1E66F5),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCD0DA),
        onSecondaryContainer = Color(0xFF1E66F5),
        tertiary = Color(0xFF179299),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFCCD0DA),
        onTertiaryContainer = Color(0xFF179299),
        outline = Color(0xFFACB0BE),
        error = Color(0xFFD20F39),
        onError = Color.White
    )

    // 5. ROSE PINE
    private fun createRosePineDark(id: String, displayName: String, base: Color, surface: Color, overlay: Color, text: Color, rose: Color, pine: Color, gold: Color, outlineColor: Color) = ThemePalette(
        id = id,
        displayName = displayName,
        isDark = true,
        background = base,
        surface = surface,
        surfaceVariant = overlay,
        onBackground = text,
        onSurface = text,
        onSurfaceVariant = Color(0xFF908CAA),
        primary = rose,
        onPrimary = base,
        primaryContainer = overlay,
        onPrimaryContainer = rose,
        secondary = pine,
        onSecondary = Color.White,
        secondaryContainer = surface,
        onSecondaryContainer = pine,
        tertiary = gold,
        onTertiary = base,
        tertiaryContainer = surface,
        onTertiaryContainer = gold,
        outline = outlineColor,
        error = Color(0xFFEB6F92),
        onError = base
    )

    private val ROSE_PINE_DAWN = ThemePalette(
        id = "dawn",
        displayName = "Dawn",
        isDark = false,
        background = Color(0xFFFAF4ED),
        surface = Color(0xFFFFF8F3),
        surfaceVariant = Color(0xFFF2E9E1),
        onBackground = Color(0xFF575279),
        onSurface = Color(0xFF575279),
        onSurfaceVariant = Color(0xFF797593),
        primary = Color(0xFFD7827E),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFF8F3),
        onPrimaryContainer = Color(0xFFD7827E),
        secondary = Color(0xFF286983),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF2E9E1),
        onSecondaryContainer = Color(0xFF286983),
        tertiary = Color(0xFFEA9D34),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF2E9E1),
        onTertiaryContainer = Color(0xFFEA9D34),
        outline = Color(0xFFCECACD),
        error = Color(0xFFB4637A),
        onError = Color.White
    )

    // 6. KANAGAWA
    private val KANAGAWA_WAVE = ThemePalette(
        id = "wave",
        displayName = "Wave",
        isDark = true,
        background = Color(0xFF1F1F28),
        surface = Color(0xFF2A2A37),
        surfaceVariant = Color(0xFF363646),
        onBackground = Color(0xFFDCD7BA),
        onSurface = Color(0xFFDCD7BA),
        onSurfaceVariant = Color(0xFFC8C093),
        primary = Color(0xFF7E9CD8),
        onPrimary = Color(0xFF1F1F28),
        primaryContainer = Color(0xFF363646),
        onPrimaryContainer = Color(0xFF7E9CD8),
        secondary = Color(0xFF7AA89F),
        onSecondary = Color(0xFF1F1F28),
        secondaryContainer = Color(0xFF2A2A37),
        onSecondaryContainer = Color(0xFF7AA89F),
        tertiary = Color(0xFFDCA561),
        onTertiary = Color(0xFF1F1F28),
        tertiaryContainer = Color(0xFF2A2A37),
        onTertiaryContainer = Color(0xFFDCA561),
        outline = Color(0xFF54546D),
        error = Color(0xFFE46876),
        onError = Color(0xFF1F1F28)
    )

    private val KANAGAWA_DRAGON = ThemePalette(
        id = "dragon",
        displayName = "Dragon",
        isDark = true,
        background = Color(0xFF181616),
        surface = Color(0xFF282727),
        surfaceVariant = Color(0xFF393836),
        onBackground = Color(0xFFC5C9C5),
        onSurface = Color(0xFFC5C9C5),
        onSurfaceVariant = Color(0xFFA6A69C),
        primary = Color(0xFF87A987),
        onPrimary = Color(0xFF181616),
        primaryContainer = Color(0xFF393836),
        onPrimaryContainer = Color(0xFF87A987),
        secondary = Color(0xFF8BA4B0),
        onSecondary = Color(0xFF181616),
        secondaryContainer = Color(0xFF282727),
        onSecondaryContainer = Color(0xFF8BA4B0),
        tertiary = Color(0xFFC4B28A),
        onTertiary = Color(0xFF181616),
        tertiaryContainer = Color(0xFF282727),
        onTertiaryContainer = Color(0xFFC4B28A),
        outline = Color(0xFF625E5A),
        error = Color(0xFFC4746E),
        onError = Color(0xFF181616)
    )

    private val KANAGAWA_LOTUS = ThemePalette(
        id = "lotus",
        displayName = "Lotus",
        isDark = false,
        background = Color(0xFFF2ECBC),
        surface = Color(0xFFE5DDB0),
        surfaceVariant = Color(0xFFDCD5AC),
        onBackground = Color(0xFF544F6E),
        onSurface = Color(0xFF544F6E),
        onSurfaceVariant = Color(0xFF716E61),
        primary = Color(0xFF6693BF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5DDB0),
        onPrimaryContainer = Color(0xFF6693BF),
        secondary = Color(0xFF597B75),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCD5AC),
        onSecondaryContainer = Color(0xFF597B75),
        tertiary = Color(0xFFCC6D00),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFDCD5AC),
        onTertiaryContainer = Color(0xFFCC6D00),
        outline = Color(0xFF8A8980),
        error = Color(0xFFC84053),
        onError = Color.White
    )

    // 7. NORD
    private val NORD_STANDARD = ThemePalette(
        id = "standard",
        displayName = "Standard",
        isDark = true,
        background = Color(0xFF2E3440),
        surface = Color(0xFF3B4252),
        surfaceVariant = Color(0xFF434C5E),
        onBackground = Color(0xFFECEFF4),
        onSurface = Color(0xFFECEFF4),
        onSurfaceVariant = Color(0xFFD8DEE9),
        primary = Color(0xFF88C0D0),
        onPrimary = Color(0xFF2E3440),
        primaryContainer = Color(0xFF434C5E),
        onPrimaryContainer = Color(0xFF88C0D0),
        secondary = Color(0xFF81A1C1),
        onSecondary = Color(0xFF2E3440),
        secondaryContainer = Color(0xFF3B4252),
        onSecondaryContainer = Color(0xFF81A1C1),
        tertiary = Color(0xFFA3BE8C),
        onTertiary = Color(0xFF2E3440),
        tertiaryContainer = Color(0xFF3B4252),
        onTertiaryContainer = Color(0xFFA3BE8C),
        outline = Color(0xFF4C566A),
        error = Color(0xFFBF616A),
        onError = Color(0xFF2E3440)
    )

    private val NORD_LIGHT_SNOW_STORM = ThemePalette(
        id = "light_snow_storm",
        displayName = "Light Snow Storm",
        isDark = false,
        background = Color(0xFFECEFF4),
        surface = Color(0xFFE5E9F0),
        surfaceVariant = Color(0xFFD8DEE9),
        onBackground = Color(0xFF2E3440),
        onSurface = Color(0xFF2E3440),
        onSurfaceVariant = Color(0xFF4C566A),
        primary = Color(0xFF5E81AC),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5E9F0),
        onPrimaryContainer = Color(0xFF5E81AC),
        secondary = Color(0xFF88C0D0),
        onSecondary = Color(0xFF2E3440),
        secondaryContainer = Color(0xFFD8DEE9),
        onSecondaryContainer = Color(0xFF88C0D0),
        tertiary = Color(0xFFA3BE8C),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD8DEE9),
        onTertiaryContainer = Color(0xFFA3BE8C),
        outline = Color(0xFF4C566A),
        error = Color(0xFFBF616A),
        onError = Color.White
    )

    private val PALETTES: Map<AppTheme, Map<String, ThemePalette>> = mapOf(
        AppTheme.GRUVBOX to mapOf(
            "dark" to GRUVBOX_DARK,
            "light" to GRUVBOX_LIGHT
        ),
        AppTheme.EVERFOREST to mapOf(
            "dark_hard" to createEverforestDark("dark_hard", "Dark Hard", Color(0xFF272E33), Color(0xFF2E383C), Color(0xFF374145), Color(0xFF495156)),
            "dark_medium" to createEverforestDark("dark_medium", "Dark Medium", Color(0xFF2D353B), Color(0xFF343F44), Color(0xFF3D484D), Color(0xFF4F585E)),
            "dark_soft" to createEverforestDark("dark_soft", "Dark Soft", Color(0xFF333C43), Color(0xFF3A464C), Color(0xFF434F55), Color(0xFF555F66)),
            "light_hard" to createEverforestLight("light_hard", "Light Hard", Color(0xFFFFFBEF), Color(0xFFF8F5E4), Color(0xFFF2EFDF), Color(0xFFE8E5D5)),
            "light_medium" to createEverforestLight("light_medium", "Light Medium", Color(0xFFFDF6E3), Color(0xFFF4F0D9), Color(0xFFEFEBD4), Color(0xFFE0DCC7)),
            "light_soft" to createEverforestLight("light_soft", "Light Soft", Color(0xFFF3EBD7), Color(0xFFEEE7D0), Color(0xFFE5DFC5), Color(0xFFD8D1B7))
        ),
        AppTheme.TOKYO_NIGHT to mapOf(
            "night" to createTokyoNightDark("night", "Night", Color(0xFF1A1B26), Color(0xFF16161E), Color(0xFF292E42), Color(0xFFC0CAF5), Color(0xFF7AA2F7), Color(0xFF7DCFFF), Color(0xFFBB9AF7), Color(0xFF414868)),
            "storm" to createTokyoNightDark("storm", "Storm", Color(0xFF24283B), Color(0xFF1F2335), Color(0xFF292E42), Color(0xFFC0CAF5), Color(0xFF7AA2F7), Color(0xFF7DCFFF), Color(0xFFBB9AF7), Color(0xFF414868)),
            "moon" to createTokyoNightDark("moon", "Moon", Color(0xFF222436), Color(0xFF1E2030), Color(0xFF2F334D), Color(0xFFC8D3F5), Color(0xFF82AAFF), Color(0xFF86E1FC), Color(0xFFC099FF), Color(0xFF444A73)),
            "day" to TOKYO_NIGHT_DAY
        ),
        AppTheme.CATPPUCCIN to mapOf(
            "mocha" to createCatppuccinDark("mocha", "Mocha", Color(0xFF1E1E2E), Color(0xFF181825), Color(0xFF313244), Color(0xFFCDD6F4), Color(0xFFCBA6F7), Color(0xFF89B4FA), Color(0xFF94E2D5), Color(0xFF585B70), Color(0xFF11111B)),
            "macchiato" to createCatppuccinDark("macchiato", "Macchiato", Color(0xFF24273A), Color(0xFF1E2030), Color(0xFF363A4F), Color(0xFFCAD3F5), Color(0xFFC6A0F6), Color(0xFF8AADF4), Color(0xFF8BD5CA), Color(0xFF5B6078), Color(0xFF181926)),
            "frappe" to createCatppuccinDark("frappe", "Frappé", Color(0xFF303446), Color(0xFF292C3C), Color(0xFF414559), Color(0xFFC6D0F5), Color(0xFFCA9EE6), Color(0xFF8CAAEE), Color(0xFF81C8BE), Color(0xFF626880), Color(0xFF232634)),
            "latte" to CATPPUCCIN_LATTE
        ),
        AppTheme.ROSE_PINE to mapOf(
            "main" to createRosePineDark("main", "Main", Color(0xFF191724), Color(0xFF1F1D2E), Color(0xFF26233A), Color(0xFFE0DEF4), Color(0xFFEBBCBA), Color(0xFF31748F), Color(0xFFF6C177), Color(0xFF524F67)),
            "moon" to createRosePineDark("moon", "Moon", Color(0xFF232136), Color(0xFF2A273F), Color(0xFF393552), Color(0xFFE0DEF4), Color(0xFFEA9A97), Color(0xFF3E8FB0), Color(0xFFF6C177), Color(0xFF56526E)),
            "dawn" to ROSE_PINE_DAWN
        ),
        AppTheme.KANAGAWA to mapOf(
            "wave" to KANAGAWA_WAVE,
            "dragon" to KANAGAWA_DRAGON,
            "lotus" to KANAGAWA_LOTUS
        ),
        AppTheme.NORD to mapOf(
            "standard" to NORD_STANDARD,
            "light_snow_storm" to NORD_LIGHT_SNOW_STORM
        )
    )

    fun getPalette(theme: AppTheme, variant: String?): ThemePalette? {
        val themeMap = PALETTES[theme] ?: return null
        val targetVariant = if (variant.isNullOrBlank()) theme.defaultVariant else variant
        return themeMap[targetVariant] ?: themeMap[theme.defaultVariant] ?: themeMap.values.firstOrNull()
    }
}

