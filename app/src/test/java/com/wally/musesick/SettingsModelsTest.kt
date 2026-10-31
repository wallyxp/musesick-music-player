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
        assertEquals(AppTheme.AQUA, AppTheme.fromString("AQUA"))
        assertEquals("Aqua", AppTheme.AQUA.displayName)
        assertEquals(AppTheme.CUSTOM_COLOR, AppTheme.fromString("CUSTOM_COLOR"))
        assertEquals(AppTheme.CUSTOM_IMAGE, AppTheme.fromString("CUSTOM_IMAGE"))
        assertEquals("Custom Image", AppTheme.CUSTOM_IMAGE.displayName)

        // AMBIENT maps to CUSTOM_IMAGE
        assertEquals(AppTheme.CUSTOM_IMAGE, AppTheme.fromString("AMBIENT"))

        // Legacy themes map to DEFAULT
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("MATERIAL_YOU"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("GRUVBOX"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("EVERFOREST"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("TOKYO_NIGHT"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("CATPPUCCIN"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("ROSE_PINE"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("KANAGAWA"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("NORD"))

        // Fallbacks default to DEFAULT
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("LIQUID_GLASS"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString("UNKNOWN_THEME"))
        assertEquals(AppTheme.DEFAULT, AppTheme.fromString(null))
    }

    @Test
    fun testThemePalettes_variants() {
        // Aqua is now a consolidated single theme (Damegami color scheme) without variants
        assertTrue(AppTheme.AQUA.availableVariants.isEmpty())

        // Palette lookup returns Damegami palette
        val aquaPalette = ThemePalettes.getPalette(AppTheme.AQUA, null)
        assertNotNull("Default palette for AQUA should exist", aquaPalette)
        assertEquals("damegami", aquaPalette?.id)
        assertNotNull("ColorScheme should be generatable", aquaPalette?.toColorScheme())
    }

    @Test
    fun testAccentColorPresets() {
        assertTrue(AccentColorPresets.isNotEmpty())
        assertEquals(10, AccentColorPresets.size)
        val defaultPreset = AccentColorPresets.first()
        assertEquals("amber_gold", defaultPreset.id)
        assertEquals("#FFA000", defaultPreset.hexCode)
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
