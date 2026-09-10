package com.wally.musesick.model

import androidx.compose.ui.graphics.Color

data class ThemeVariantOption(
    val id: String,
    val displayName: String
)

enum class AppTheme(
    val displayName: String,
    val description: String,
    val defaultVariant: String = "",
    val availableVariants: List<ThemeVariantOption> = emptyList()
) {
    MATERIAL_YOU(
        displayName = "Material You",
        description = "Retains the classic app interface with dynamic Material 3 color harmonies"
    ),
    GRUVBOX(
        displayName = "Gruvbox",
        description = "Retro groove color scheme with rich earth tones and high contrast",
        defaultVariant = "dark",
        availableVariants = listOf(
            ThemeVariantOption("dark", "Dark"),
            ThemeVariantOption("light", "Light")
        )
    ),
    EVERFOREST(
        displayName = "Everforest",
        description = "Comfortable natural green palette designed to be warm and easy on the eyes",
        defaultVariant = "dark_medium",
        availableVariants = listOf(
            ThemeVariantOption("dark_hard", "Dark Hard"),
            ThemeVariantOption("dark_medium", "Dark Medium"),
            ThemeVariantOption("dark_soft", "Dark Soft"),
            ThemeVariantOption("light_hard", "Light Hard"),
            ThemeVariantOption("light_medium", "Light Medium"),
            ThemeVariantOption("light_soft", "Light Soft")
        )
    ),
    TOKYO_NIGHT(
        displayName = "Tokyo Night",
        description = "Vibrant dark theme celebrating the neon lights of downtown Tokyo",
        defaultVariant = "night",
        availableVariants = listOf(
            ThemeVariantOption("night", "Night"),
            ThemeVariantOption("storm", "Storm"),
            ThemeVariantOption("moon", "Moon"),
            ThemeVariantOption("day", "Day")
        )
    ),
    CATPPUCCIN(
        displayName = "Catppuccin",
        description = "Soothing pastel theme palette with warm, high-contrast accents",
        defaultVariant = "mocha",
        availableVariants = listOf(
            ThemeVariantOption("mocha", "Mocha"),
            ThemeVariantOption("macchiato", "Macchiato"),
            ThemeVariantOption("frappe", "Frappé"),
            ThemeVariantOption("latte", "Latte")
        )
    ),
    ROSE_PINE(
        displayName = "Rosé Pine",
        description = "Moody, natural theme featuring soft lavenders, muted roses, and deep ocean blues",
        defaultVariant = "main",
        availableVariants = listOf(
            ThemeVariantOption("main", "Main"),
            ThemeVariantOption("moon", "Moon"),
            ThemeVariantOption("dawn", "Dawn")
        )
    ),
    KANAGAWA(
        displayName = "Kanagawa",
        description = "Elegant theme inspired by the famous Great Wave off Kanagawa woodblock print",
        defaultVariant = "wave",
        availableVariants = listOf(
            ThemeVariantOption("wave", "Wave"),
            ThemeVariantOption("dragon", "Dragon"),
            ThemeVariantOption("lotus", "Lotus")
        )
    ),
    NORD(
        displayName = "Nord",
        description = "Arctic, north-bluish clean and elegant color palette",
        defaultVariant = "standard",
        availableVariants = listOf(
            ThemeVariantOption("standard", "Standard"),
            ThemeVariantOption("light_snow_storm", "Light Snow Storm")
        )
    ),
    CUSTOM_COLOR(
        displayName = "Choose Color",
        description = "Keeps the clean Material You interface styled with your selected accent color"
    );

    companion object {
        fun fromString(value: String?): AppTheme {
            return try {
                if (value == null) MATERIAL_YOU else valueOf(value)
            } catch (e: Exception) {
                MATERIAL_YOU
            }
        }
    }
}

data class ColorPreset(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val hexCode: String
)

val AccentColorPresets: List<ColorPreset> = listOf(
    ColorPreset("royal_violet", "Royal Violet", Color(0xFF6750A4), "#6750A4"),
    ColorPreset("electric_blue", "Electric Blue", Color(0xFF007AFF), "#007AFF"),
    ColorPreset("emerald_green", "Emerald Green", Color(0xFF00A86B), "#00A86B"),
    ColorPreset("crimson_red", "Crimson Red", Color(0xFFD32F2F), "#D32F2F"),
    ColorPreset("sunset_orange", "Sunset Orange", Color(0xFFFF6F00), "#FF6F00"),
    ColorPreset("radiant_cyan", "Radiant Cyan", Color(0xFF00BCD4), "#00BCD4"),
    ColorPreset("rose_pink", "Rose Pink", Color(0xFFE91E63), "#E91E63"),
    ColorPreset("amber_gold", "Amber Gold", Color(0xFFFFA000), "#FFA000"),
    ColorPreset("midnight_slate", "Midnight Slate", Color(0xFF607D8B), "#607D8B"),
    ColorPreset("neon_lime", "Neon Lime", Color(0xFF76FF03), "#76FF03")
)
