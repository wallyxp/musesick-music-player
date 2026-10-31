package com.wally.musesick.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar

data class AmbientSlotPalette(
    val name: String,
    val description: String,
    val colors: List<Color>
)

object AmbientPalettes {
    val slots = listOf(
        // 0: 00:00 - 01:59 Midnight
        AmbientSlotPalette(
            name = "Deep Midnight",
            description = "Starlight & cosmic indigo",
            colors = listOf(
                Color(0xFF070B19),
                Color(0xFF0F172A),
                Color(0xFF1E1B4B),
                Color(0xFF0B132B)
            )
        ),
        // 1: 02:00 - 03:59 Witching Hour / Late Night
        AmbientSlotPalette(
            name = "Witching Hour",
            description = "Obsidian violet & deep shadow",
            colors = listOf(
                Color(0xFF0B061A),
                Color(0xFF1B0C30),
                Color(0xFF19103C),
                Color(0xFF050512)
            )
        ),
        // 2: 04:00 - 05:59 Predawn First Light
        AmbientSlotPalette(
            name = "Predawn",
            description = "Faint horizon glow & indigo plum",
            colors = listOf(
                Color(0xFF13112E),
                Color(0xFF2C1642),
                Color(0xFF4C1944),
                Color(0xFF1B1433)
            )
        ),
        // 3: 06:00 - 07:59 Sunrise
        AmbientSlotPalette(
            name = "Sunrise Glow",
            description = "Vibrant dawn coral & golden rose",
            colors = listOf(
                Color(0xFF831843),
                Color(0xFFC2410C),
                Color(0xFFD97706),
                Color(0xFF581C87)
            )
        ),
        // 4: 08:00 - 09:59 Morning Sun
        AmbientSlotPalette(
            name = "Morning Sun",
            description = "Fresh daylight amber & turquoise",
            colors = listOf(
                Color(0xFFB45309),
                Color(0xFF0284C7),
                Color(0xFF0D9488),
                Color(0xFF0369A1)
            )
        ),
        // 5: 10:00 - 11:59 Clear Sky
        AmbientSlotPalette(
            name = "Clear Sky",
            description = "Vibrant azure & open daylight",
            colors = listOf(
                Color(0xFF0369A1),
                Color(0xFF0284C7),
                Color(0xFF0EA5E9),
                Color(0xFF1D4ED8)
            )
        ),
        // 6: 12:00 - 13:59 Midday Zenith
        AmbientSlotPalette(
            name = "Midday Zenith",
            description = "Luminous solar blue & brilliant cyan",
            colors = listOf(
                Color(0xFF0284C7),
                Color(0xFF38BDF8),
                Color(0xFF0D9488),
                Color(0xFF1E40AF)
            )
        ),
        // 7: 14:00 - 15:59 Afternoon Horizon
        AmbientSlotPalette(
            name = "Afternoon Horizon",
            description = "Warm azure & deep oceanic teal",
            colors = listOf(
                Color(0xFF075985),
                Color(0xFF0284C7),
                Color(0xFF0F766E),
                Color(0xFF1E3A8A)
            )
        ),
        // 8: 16:00 - 17:59 Golden Hour
        AmbientSlotPalette(
            name = "Golden Hour",
            description = "Rich sunset amber, coral & crimson",
            colors = listOf(
                Color(0xFFD97706),
                Color(0xFFEA580C),
                Color(0xFFB91C1C),
                Color(0xFF701A75)
            )
        ),
        // 9: 18:00 - 19:59 Sunset Dusk
        AmbientSlotPalette(
            name = "Sunset Dusk",
            description = "Velvet twilight magenta & dusk violet",
            colors = listOf(
                Color(0xFF831843),
                Color(0xFF701A75),
                Color(0xFF4C1D95),
                Color(0xFF2E1065)
            )
        ),
        // 10: 20:00 - 21:59 Nightfall
        AmbientSlotPalette(
            name = "Nightfall",
            description = "Deep violet & sapphire twilight",
            colors = listOf(
                Color(0xFF3B0764),
                Color(0xFF1E1B4B),
                Color(0xFF1E3A8A),
                Color(0xFF0F172A)
            )
        ),
        // 11: 22:00 - 23:59 Starry Night
        AmbientSlotPalette(
            name = "Starry Night",
            description = "Cosmic blue & deep abyss navy",
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF172554),
                Color(0xFF1E1B4B),
                Color(0xFF020617)
            )
        )
    )

    fun getCurrentSlot(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return (hour / 2).coerceIn(0, 11)
    }

    fun getPalette(slot: Int): AmbientSlotPalette {
        val safeIndex = ((slot % 12) + 12) % 12
        return slots[safeIndex]
    }

    fun getMillisUntilNextSlot(): Long {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val nextSlotHour = ((currentHour / 2) + 1) * 2
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nextSlotHour % 24)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (nextSlotHour >= 24) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - calendar.timeInMillis).coerceAtLeast(1000L)
    }
}

/**
 * Remembers a dynamic ambient gradient brush based on the 2-hour slot of the day.
 * - Instantly refreshed upon initial composition when the app is opened.
 * - Automatically recalculates and smoothly transitions colors at every 2-hour boundary.
 */
@Composable
fun rememberAmbientBrush(): Brush {
    var slot by remember {
        mutableIntStateOf(AmbientPalettes.getCurrentSlot())
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            slot = AmbientPalettes.getCurrentSlot()
            val delayMs = AmbientPalettes.getMillisUntilNextSlot()
            delay(delayMs + 600L) // Small buffer to ensure passing boundary
        }
    }

    val palette = AmbientPalettes.getPalette(slot)

    val color1 by animateColorAsState(targetValue = palette.colors[0], animationSpec = tween(2000), label = "amb1")
    val color2 by animateColorAsState(targetValue = palette.colors[1], animationSpec = tween(2000), label = "amb2")
    val color3 by animateColorAsState(targetValue = palette.colors[2], animationSpec = tween(2000), label = "amb3")
    val color4 by animateColorAsState(targetValue = palette.colors[3], animationSpec = tween(2000), label = "amb4")

    return remember(color1, color2, color3, color4) {
        Brush.verticalGradient(
            colors = listOf(color1, color2, color3, color4)
        )
    }
}

