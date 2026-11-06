package com.wally.musesick.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.PlayerStyle
import com.wally.musesick.model.SpotifyAccountInfo
import com.wally.musesick.model.UserProfile
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
        private const val KEY_YT_MUSIC_COOKIE = "yt_music_cookie"
        private const val KEY_YT_USER_NAME = "yt_user_name"
        private const val KEY_YT_USER_HANDLE = "yt_user_handle"
        private const val KEY_YT_USER_AVATAR_URL = "yt_user_avatar_url"
        private const val KEY_AUTO_SYNC_INTERVAL = "yt_auto_sync_interval"
        private const val KEY_LAST_PLAYLIST_SYNC_TIME = "yt_last_playlist_sync_time"
        // Offline User Profile keys
        private const val KEY_USER_PROFILE_NAME = "user_profile_name"
        private const val KEY_USER_PROFILE_AVATAR_PATH = "user_profile_avatar_path"
        // Spotify Account keys
        private const val KEY_SPOTIFY_ACCESS_TOKEN = "spotify_access_token"
        private const val KEY_SPOTIFY_TOKEN_EXPIRATION = "spotify_token_expiration"
        private const val KEY_SPOTIFY_COOKIE = "spotify_cookie"
        private const val KEY_SPOTIFY_USER_ID = "spotify_user_id"
        private const val KEY_SPOTIFY_USER_NAME = "spotify_user_name"
        private const val KEY_SPOTIFY_USER_EMAIL = "spotify_user_email"
        private const val KEY_SPOTIFY_USER_AVATAR_URL = "spotify_user_avatar_url"
        const val DEFAULT_ACCENT_COLOR = 0xFF6750A4.toInt() // Royal Violet
    }

    enum class AutoSyncInterval(val label: String, val subtitle: String, val durationMs: Long?) {
        DAILY("Daily", "Sync playlists automatically every 24 hours", 24L * 60L * 60L * 1000L),
        WEEKLY("Weekly", "Sync playlists automatically once a week", 7L * 24L * 60L * 60L * 1000L),
        MONTHLY("Monthly", "Sync playlists automatically once a month", 30L * 24L * 60L * 60L * 1000L),
        MANUAL("When I Choose to Sync", "Only sync when you tap Sync Now or edit playlists", null);

        companion object {
            fun fromString(value: String?): AutoSyncInterval {
                return values().find { it.name == value } ?: DAILY
            }
        }
    }

    fun getAutoSyncInterval(): AutoSyncInterval {
        return AutoSyncInterval.fromString(prefs.getString(KEY_AUTO_SYNC_INTERVAL, AutoSyncInterval.DAILY.name))
    }

    fun setAutoSyncInterval(interval: AutoSyncInterval) {
        prefs.edit().putString(KEY_AUTO_SYNC_INTERVAL, interval.name).apply()
    }

    fun getLastPlaylistSyncTime(): Long {
        return prefs.getLong(KEY_LAST_PLAYLIST_SYNC_TIME, 0L)
    }

    fun setLastPlaylistSyncTime(timestampMs: Long) {
        prefs.edit().putLong(KEY_LAST_PLAYLIST_SYNC_TIME, timestampMs).apply()
    }

    fun getYtMusicCookie(): String? {
        return prefs.getString(KEY_YT_MUSIC_COOKIE, null)?.takeIf { it.isNotBlank() }
    }

    fun setYtMusicCookie(cookie: String?) {
        prefs.edit().putString(KEY_YT_MUSIC_COOKIE, cookie).apply()
    }

    fun getYtUserName(): String? {
        return prefs.getString(KEY_YT_USER_NAME, null)?.takeIf { it.isNotBlank() }
    }

    fun setYtUserName(name: String?) {
        prefs.edit().putString(KEY_YT_USER_NAME, name).apply()
    }

    fun getYtUserHandle(): String? {
        return prefs.getString(KEY_YT_USER_HANDLE, null)?.takeIf { it.isNotBlank() }
    }

    fun setYtUserHandle(handle: String?) {
        prefs.edit().putString(KEY_YT_USER_HANDLE, handle).apply()
    }

    fun getYtUserAvatarUrl(): String? {
        return prefs.getString(KEY_YT_USER_AVATAR_URL, null)?.takeIf { it.isNotBlank() }
    }

    fun setYtUserAvatarUrl(url: String?) {
        prefs.edit().putString(KEY_YT_USER_AVATAR_URL, url).apply()
    }

    fun clearYtAccount() {
        prefs.edit()
            .remove(KEY_YT_MUSIC_COOKIE)
            .remove(KEY_YT_USER_NAME)
            .remove(KEY_YT_USER_HANDLE)
            .remove(KEY_YT_USER_AVATAR_URL)
            .remove(KEY_LAST_PLAYLIST_SYNC_TIME)
            .apply()
    }

    // --- Offline User Profile (completely decoupled from online logins) ---
    fun getUserProfile(): UserProfile {
        val name = prefs.getString(KEY_USER_PROFILE_NAME, "Guest")?.ifBlank { "Guest" } ?: "Guest"
        val path = prefs.getString(KEY_USER_PROFILE_AVATAR_PATH, null)?.takeIf { it.isNotBlank() }
        val validAvatar = if (path != null && File(path).exists()) path else null
        return UserProfile(name = name, avatarPath = validAvatar)
    }

    fun saveUserProfile(name: String, avatarPath: String?) {
        val cleanName = name.trim().ifBlank { "Guest" }
        prefs.edit()
            .putString(KEY_USER_PROFILE_NAME, cleanName)
            .putString(KEY_USER_PROFILE_AVATAR_PATH, avatarPath)
            .apply()
    }

    suspend fun saveProfileAvatar(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "profile_avatar_${System.currentTimeMillis()}.jpg"
            val targetFile = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            val oldAvatar = prefs.getString(KEY_USER_PROFILE_AVATAR_PATH, null)
            if (!oldAvatar.isNullOrBlank() && oldAvatar != targetFile.absolutePath) {
                try { File(oldAvatar).delete() } catch (_: Exception) {}
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun removeProfileAvatar() {
        val oldAvatar = prefs.getString(KEY_USER_PROFILE_AVATAR_PATH, null)
        if (!oldAvatar.isNullOrBlank()) {
            try { File(oldAvatar).delete() } catch (_: Exception) {}
        }
        prefs.edit().remove(KEY_USER_PROFILE_AVATAR_PATH).apply()
    }

    // --- Spotify Account & Auth ---
    fun getSpotifyAccessToken(): String? {
        return prefs.getString(KEY_SPOTIFY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    fun setSpotifyAccessToken(token: String?, expirationMs: Long = 0L) {
        prefs.edit()
            .putString(KEY_SPOTIFY_ACCESS_TOKEN, token)
            .putLong(KEY_SPOTIFY_TOKEN_EXPIRATION, expirationMs)
            .apply()
    }

    fun getSpotifyTokenExpiration(): Long {
        return prefs.getLong(KEY_SPOTIFY_TOKEN_EXPIRATION, 0L)
    }

    fun getSpotifyCookie(): String? {
        return prefs.getString(KEY_SPOTIFY_COOKIE, null)?.takeIf { it.isNotBlank() }
    }

    fun setSpotifyCookie(cookie: String?) {
        prefs.edit().putString(KEY_SPOTIFY_COOKIE, cookie).apply()
    }

    fun getSpotifyAccountInfo(): SpotifyAccountInfo? {
        val id = prefs.getString(KEY_SPOTIFY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_SPOTIFY_USER_NAME, "Spotify User") ?: "Spotify User"
        val email = prefs.getString(KEY_SPOTIFY_USER_EMAIL, null)
        val avatar = prefs.getString(KEY_SPOTIFY_USER_AVATAR_URL, null)
        return SpotifyAccountInfo(id = id, displayName = name, email = email, avatarUrl = avatar)
    }

    fun setSpotifyAccountInfo(info: SpotifyAccountInfo?) {
        val editor = prefs.edit()
        if (info == null) {
            editor.remove(KEY_SPOTIFY_USER_ID)
                .remove(KEY_SPOTIFY_USER_NAME)
                .remove(KEY_SPOTIFY_USER_EMAIL)
                .remove(KEY_SPOTIFY_USER_AVATAR_URL)
        } else {
            editor.putString(KEY_SPOTIFY_USER_ID, info.id)
                .putString(KEY_SPOTIFY_USER_NAME, info.displayName)
                .putString(KEY_SPOTIFY_USER_EMAIL, info.email)
                .putString(KEY_SPOTIFY_USER_AVATAR_URL, info.avatarUrl)
        }
        editor.apply()
    }

    fun clearSpotifyAccount() {
        prefs.edit()
            .remove(KEY_SPOTIFY_ACCESS_TOKEN)
            .remove(KEY_SPOTIFY_TOKEN_EXPIRATION)
            .remove(KEY_SPOTIFY_COOKIE)
            .remove(KEY_SPOTIFY_USER_ID)
            .remove(KEY_SPOTIFY_USER_NAME)
            .remove(KEY_SPOTIFY_USER_EMAIL)
            .remove(KEY_SPOTIFY_USER_AVATAR_URL)
            .apply()
    }

    fun hasCompletedArtistOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setArtistOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getPlayerStyle(): PlayerStyle {
        return PlayerStyle.FULLSCREEN_ALBUM_ART
    }

    fun setPlayerStyle(style: PlayerStyle) {
        prefs.edit().putString(KEY_PLAYER_STYLE, PlayerStyle.FULLSCREEN_ALBUM_ART.name).apply()
    }

    fun getAppTheme(): AppTheme {
        val themeName = prefs.getString(KEY_APP_THEME, AppTheme.DEFAULT.name)
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
