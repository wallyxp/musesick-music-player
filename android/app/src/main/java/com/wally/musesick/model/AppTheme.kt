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
        description = "Musesick signature frosted glass theme with dark primary background"
    ),
    CUSTOM_IMAGE(
        displayName = "Custom Image",
        description = "Uses your custom photo as a darkened and blurred wallpaper background with floating glassmorphism"
    ),
    CUSTOM_COLOR(
        displayName = "Choose Color",
        description = "Floating glassmorphism with an AMOLED black background and your selected accent color"
    ),
    AQUA(
        displayName = "Aqua",
        description = "Immersive water goddess theme with Darugami & Damegami artwork backgrounds and vibrant glassmorphism",
        defaultVariant = "darugami",
        availableVariants = listOf(
            ThemeVariantOption("darugami", "Darugami"),
            ThemeVariantOption("damegami", "Damegami")
        )
    ),
    MATERIAL_YOU(
        displayName = "Material You",
        description = "Retains the classic app interface with dynamic Material 3 color harmonies"
    );

    companion object {
        fun fromString(value: String?): AppTheme {
            return try {
                when (value) {
                    null -> DEFAULT
                    "DEFAULT" -> DEFAULT
                    "CUSTOM_IMAGE", "AMBIENT" -> CUSTOM_IMAGE
                    "CUSTOM_COLOR" -> CUSTOM_COLOR
                    "AQUA" -> AQUA
                    "MATERIAL_YOU" -> MATERIAL_YOU
                    else -> DEFAULT
                }
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

val AccentColorPresets = listOf(
    ColorPreset("royal_violet", "Royal Violet", Color(0xFF6750A4), "#6750A4"),
    ColorPreset("sapphire_blue", "Sapphire Blue", Color(0xFF1E88E5), "#1E88E5"),
    ColorPreset("emerald_green", "Emerald Green", Color(0xFF2E7D32), "#2E7D32"),
    ColorPreset("crimson_red", "Crimson Red", Color(0xFFC62828), "#C62828"),
    ColorPreset("amber_gold", "Amber Gold", Color(0xFFF57F17), "#F57F17"),
    ColorPreset("cyber_cyan", "Cyber Cyan", Color(0xFF00ACC1), "#00ACC1"),
    ColorPreset("neon_magenta", "Neon Magenta", Color(0xFFD81B60), "#D81B60"),
    ColorPreset("sunset_orange", "Sunset Orange", Color(0xFFE64A19), "#E64A19"),
    ColorPreset("deep_teal", "Deep Teal", Color(0xFF00897B), "#00897B"),
    ColorPreset("pure_white", "Pure White", Color(0xFFE0E0E0), "#E0E0E0")
)
