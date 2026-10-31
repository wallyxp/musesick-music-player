package com.wally.musesick.model

/**
 * UI State for the synchronized lyrics sheet/container.
 */
sealed class LyricsUiState {
    object Idle : LyricsUiState()
    object Loading : LyricsUiState()
    data class Success(
        val lyrics: List<LyricLine>,
        val plainLyrics: String? = null
    ) : LyricsUiState()
    object Instrumental : LyricsUiState()
    data class Empty(
        val message: String = "Lyrics not available"
    ) : LyricsUiState()
}

