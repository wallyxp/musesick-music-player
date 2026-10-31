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
    DEFAULT(
        displayName = "Default",
        description = "Dynamic ambient gradient wallpaper based on the local time of day with floating glassmorphism"
    ),
    AQUA(
        displayName = "Aqua",
        description = "Immersive water goddess theme with Damegami artwork background, custom cutouts and vibrant glassmorphism"
    ),
    CUSTOM_IMAGE(
        displayName = "Custom Image",
        description = "Uses your custom photo as a darkened and blurred wallpaper background with floating glassmorphism"
    ),
    CUSTOM_COLOR(
        displayName = "Choose Color",
        description = "Deep black background styled with your selected accent color and floating glassmorphism"
    );

    companion object {
        fun fromString(value: String?): AppTheme {
            return try {
                if (value == null) DEFAULT
                else if (value == "AMBIENT") CUSTOM_IMAGE
                else if (value == "MATERIAL_YOU" || value == "GRUVBOX" || value == "EVERFOREST" ||
                    value == "TOKYO_NIGHT" || value == "CATPPUCCIN" || value == "ROSE_PINE" ||
                    value == "KANAGAWA" || value == "NORD") DEFAULT
                else valueOf(value)
            } catch (e: Exception) {
                DEFAULT
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
    ColorPreset("amber_gold", "Amber Gold", Color(0xFFFFA000), "#FFA000"),
    ColorPreset("royal_violet", "Royal Violet", Color(0xFF6750A4), "#6750A4"),
    ColorPreset("electric_blue", "Electric Blue", Color(0xFF007AFF), "#007AFF"),
    ColorPreset("emerald_green", "Emerald Green", Color(0xFF00A86B), "#00A86B"),
    ColorPreset("crimson_red", "Crimson Red", Color(0xFFD32F2F), "#D32F2F"),
    ColorPreset("sunset_orange", "Sunset Orange", Color(0xFFFF6F00), "#FF6F00"),
    ColorPreset("radiant_cyan", "Radiant Cyan", Color(0xFF00BCD4), "#00BCD4"),
    ColorPreset("rose_pink", "Rose Pink", Color(0xFFE91E63), "#E91E63"),
    ColorPreset("midnight_slate", "Midnight Slate", Color(0xFF607D8B), "#607D8B"),
    ColorPreset("neon_lime", "Neon Lime", Color(0xFF76FF03), "#76FF03")
)
