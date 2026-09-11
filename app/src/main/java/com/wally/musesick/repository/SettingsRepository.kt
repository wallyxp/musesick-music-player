package com.wally.musesick.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.PlayerStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("musesick_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "has_completed_artist_onboarding"
        private const val KEY_PLAYER_STYLE = "player_style"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_APP_THEME_VARIANT = "app_theme_variant"
        private const val KEY_PLAYER_THEME = "player_theme"
        private const val KEY_PLAYER_THEME_VARIANT = "player_theme_variant"
        private const val KEY_CUSTOM_ACCENT_COLOR = "custom_accent_color"
        private const val KEY_CUSTOM_THEME_IMAGE_PATH = "custom_theme_image_path"
        const val DEFAULT_ACCENT_COLOR = 0xFF6750A4.toInt() // Royal Violet
    }

    fun hasCompletedArtistOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setArtistOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getPlayerStyle(): PlayerStyle {
        val styleName = prefs.getString(KEY_PLAYER_STYLE, PlayerStyle.FULLSCREEN_ALBUM_ART.name)
        return PlayerStyle.fromString(styleName)
    }

    fun setPlayerStyle(style: PlayerStyle) {
        prefs.edit().putString(KEY_PLAYER_STYLE, style.name).apply()
    }

    fun getAppTheme(): AppTheme {
        val themeName = prefs.getString(KEY_APP_THEME, AppTheme.AMBIENT.name)
        return AppTheme.fromString(themeName)
    }

    fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
    }

    fun getAppThemeVariant(): String {
        return prefs.getString(KEY_APP_THEME_VARIANT, "") ?: ""
    }

    fun setAppThemeVariant(variant: String) {
        prefs.edit().putString(KEY_APP_THEME_VARIANT, variant).apply()
    }

    /**
     * Returns null if "Match App Theme" is selected, or the specific AppTheme override for the player.
     */
    fun getPlayerTheme(): AppTheme? {
        val themeName = prefs.getString(KEY_PLAYER_THEME, null)
        if (themeName == null || themeName == "MATCH_APP") return null
        return AppTheme.fromString(themeName)
    }

    fun setPlayerTheme(theme: AppTheme?) {
        if (theme == null) {
            prefs.edit().putString(KEY_PLAYER_THEME, "MATCH_APP").apply()
        } else {
            prefs.edit().putString(KEY_PLAYER_THEME, theme.name).apply()
        }
    }

    fun getPlayerThemeVariant(): String? {
        return prefs.getString(KEY_PLAYER_THEME_VARIANT, null)
    }

    fun setPlayerThemeVariant(variant: String?) {
        prefs.edit().putString(KEY_PLAYER_THEME_VARIANT, variant).apply()
    }

    fun getCustomAccentColor(): Int {
        return prefs.getInt(KEY_CUSTOM_ACCENT_COLOR, DEFAULT_ACCENT_COLOR)
    }

    fun setCustomAccentColor(colorInt: Int) {
        prefs.edit().putInt(KEY_CUSTOM_ACCENT_COLOR, colorInt).apply()
    }

    fun getCustomThemeImagePath(): String? {
        return prefs.getString(KEY_CUSTOM_THEME_IMAGE_PATH, null)
    }

    fun setCustomThemeImagePath(path: String?) {
        prefs.edit().putString(KEY_CUSTOM_THEME_IMAGE_PATH, path).apply()
    }

    suspend fun saveCustomThemeImage(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "custom_theme_bg_${System.currentTimeMillis()}.jpg"
            val targetFile = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            // Delete old file if exists and different
            val oldPath = getCustomThemeImagePath()
            if (!oldPath.isNullOrBlank() && oldPath != targetFile.absolutePath) {
                try {
                    File(oldPath).delete()
                } catch (_: Exception) {}
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
