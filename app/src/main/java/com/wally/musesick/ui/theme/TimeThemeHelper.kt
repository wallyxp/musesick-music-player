package com.wally.musesick.ui.theme

import androidx.compose.ui.graphics.Color
import java.util.Calendar

object TimeThemeHelper {
    /**
     * Returns a curated list of rich background colors reflecting the local time of day:
     * - Dawn & Sunrise (5:00 - 7:59): Coral rose, twilight violet, morning gold
     * - Morning (8:00 - 11:59): Ocean cerulean, crisp cyan, sky blue
     * - Afternoon (12:00 - 16:59): Electric royal blue, sapphire, deep azure
     * - Golden Hour & Sunset (17:00 - 19:59): Dusk violet, fiery crimson, sunset amber
     * - Night (20:00 - 23:59): Midnight cobalt, nebula purple, deep indigo
     * - Late Night (0:00 - 4:59): Deep obsidian cosmic space, dark violet, starry navy
     */
    fun getGradientColors(hour: Int = getCurrentHour()): List<Color> {
        return when (hour) {
            in 5..7 -> listOf(
                Color(0xFF1B1430),
                Color(0xFF4C1844),
                Color(0xFF8B2659),
                Color(0xFFD14A57),
                Color(0xFFFFA050)
            )
            in 8..11 -> listOf(
                Color(0xFF0B192C),
                Color(0xFF1E3E62),
                Color(0xFF006B7B),
                Color(0xFF009688),
                Color(0xFF4DD0E1)
            )
            in 12..16 -> listOf(
                Color(0xFF0F172A),
                Color(0xFF1E3A8A),
                Color(0xFF1D4ED8),
                Color(0xFF3B82F6),
                Color(0xFF60A5FA)
            )
            in 17..19 -> listOf(
                Color(0xFF1E0A2A),
                Color(0xFF4A1239),
                Color(0xFF7B1D45),
                Color(0xFFB83244),
                Color(0xFFFA7930)
            )
            in 20..23 -> listOf(
                Color(0xFF090A1A),
                Color(0xFF131538),
                Color(0xFF23164D),
                Color(0xFF381566),
                Color(0xFF1A1A40)
            )
            else -> listOf(
                Color(0xFF06060E),
                Color(0xFF0E0C22),
                Color(0xFF181135),
                Color(0xFF261345),
                Color(0xFF0F142A)
            )
        }
    }

    /**
     * Accent color matching the current period of the day.
     */
    fun getAccentColor(hour: Int = getCurrentHour()): Color {
        return when (hour) {
            in 5..7 -> Color(0xFFFF7043)   // Sunrise coral
            in 8..11 -> Color(0xFF26C6DA)  // Morning cyan
            in 12..16 -> Color(0xFF60A5FA) // Afternoon azure
            in 17..19 -> Color(0xFFFFA726) // Sunset amber
            in 20..23 -> Color(0xFFBA68C8) // Night lavender
            else -> Color(0xFFB388FF)      // Late night electric violet
        }
    }

    fun getTimePeriodName(hour: Int = getCurrentHour()): String {
        return when (hour) {
            in 5..7 -> "Dawn & Sunrise"
            in 8..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..19 -> "Golden Hour & Sunset"
            in 20..23 -> "Night"
            else -> "Late Night"
        }
    }

    fun getCurrentHour(): Int {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
}

