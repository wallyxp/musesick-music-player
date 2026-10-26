package com.wally.musesick

import com.wally.musesick.model.AccentColorPresets
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.PlayerStyle
import com.wally.musesick.model.ThemePalettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsModelsTest {

    @Test
    fun testPlayerStyle_fromString() {
        assertEquals(PlayerStyle.FULLSCREEN_ALBUM_ART, PlayerStyle.fromString("FULLSCREEN_ALBUM_ART"))
        assertEquals(PlayerStyle.NON_FULLSCREEN_ALBUM_ART, PlayerStyle.fromString("NON_FULLSCREEN_ALBUM_ART"))
        // Legacy mappings and removed styles safely fallback
        assertEquals(PlayerStyle.FULLSCREEN_ALBUM_ART, PlayerStyle.fromString("MODERN"))
        assertEquals(PlayerStyle.NON_FULLSCREEN_ALBUM_ART, PlayerStyle.fromString("GENERIC"))
        assertEquals(PlayerStyle.FULLSCREEN_ALBUM_ART, PlayerStyle.fromString("LIQUID_GLASS"))
        // Fallbacks
        assertEquals(PlayerStyle.FULLSCREEN_ALBUM_ART, PlayerStyle.fromString("UNKNOWN_STYLE"))
        assertEquals(PlayerStyle.FULLSCREEN_ALBUM_ART, PlayerStyle.fromString(null))
    }

    @Test
    fun testAppTheme_fromString() {
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("DEFAULT"))
        assertEquals(AppTheme.MATERIAL_YOU, AppTheme.fromString("MATERIAL_YOU"))
        assertEquals(AppTheme.AQUA, AppTheme.fromString("AQUA"))
        assertEquals("Aqua", AppTheme.AQUA.displayName)
        assertEquals(AppTheme.CUSTOM_COLOR, AppTheme.fromString("CUSTOM_COLOR"))
        assertEquals(AppTheme.CUSTOM_IMAGE, AppTheme.fromString("CUSTOM_IMAGE"))
        assertEquals("Custom Image", AppTheme.CUSTOM_IMAGE.displayName)

        // AMBIENT maps to CUSTOM_IMAGE
        assertEquals(AppTheme.CUSTOM_IMAGE, AppTheme.fromString("AMBIENT"))

        // Removed themes & unknown fallbacks default to DEFAULT
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("GRUVBOX"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("LIQUID_GLASS"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("UNKNOWN_THEME"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString(null))
    }

    @Test
    fun testThemePalettes_variants() {
        // Test that aqua theme with variants returns valid palettes for all its defined variants
        val themesWithVariants = listOf(
            AppTheme.AQUA
        )

        for (theme in themesWithVariants) {
            assertTrue(theme.availableVariants.isNotEmpty())
            assertTrue(theme.defaultVariant.isNotBlank())

            // Default variant lookup
            val defaultPalette = ThemePalettes.getPalette(theme, null)
            assertNotNull("Default palette for ${theme.name} should exist", defaultPalette)
            assertEquals(theme.defaultVariant, defaultPalette?.id)

            // Each specific variant lookup
            for (variant in theme.availableVariants) {
                val palette = ThemePalettes.getPalette(theme, variant.id)
                assertNotNull("Palette for ${theme.name} variant ${variant.id} should exist", palette)
                assertEquals(variant.id, palette?.id)
                assertNotNull("ColorScheme should be generatable", palette?.toColorScheme())
            }
        }
    }

    @Test
    fun testAccentColorPresets() {
        assertTrue(AccentColorPresets.isNotEmpty())
        assertEquals(10, AccentColorPresets.size)
        val defaultPreset = AccentColorPresets.first()
        assertEquals("royal_violet", defaultPreset.id)
        assertEquals("#6750A4", defaultPreset.hexCode)
    }

    @Test
    fun testAmbientPalettes() {
        assertEquals(12, com.wally.musesick.ui.theme.AmbientPalettes.slots.size)
        val currentSlot = com.wally.musesick.ui.theme.AmbientPalettes.getCurrentSlot()
        assertTrue(currentSlot in 0..11)
        val palette = com.wally.musesick.ui.theme.AmbientPalettes.getPalette(currentSlot)
        assertNotNull(palette.name)
        assertTrue(palette.colors.size >= 2)
        val millisUntilNext = com.wally.musesick.ui.theme.AmbientPalettes.getMillisUntilNextSlot()
        assertTrue(millisUntilNext > 0L)
    }
}
