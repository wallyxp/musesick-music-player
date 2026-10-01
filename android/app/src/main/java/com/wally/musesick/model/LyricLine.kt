package com.wally.musesick.model

/**
 * Represents a single parsed synchronized lyric line.
 *
 * @param time Timestamp in milliseconds matching the player's [PlaybackState.currentPositionMs].
 * @param text The lyric line text.
 */
data class LyricLine(
    val time: Long,
    val text: String
)

