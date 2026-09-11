package com.wally.musesick.util

import com.wally.musesick.model.LyricLine

/**
 * Robust LRC string parser utility.
 * Converts synchronized LRC lyrics into an array/list of [LyricLine] objects,
 * where each timestamp is converted into total milliseconds to directly match
 * player playback position state (PlaybackState.currentPositionMs).
 */
object LrcParser {

    private val timestampRegex = Regex("""\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]""")

    /**
     * Parses the raw LRC formatted text.
     *
     * @param lrcContent The raw LRC string returned from LRCLib or local files.
     * @return A list of [LyricLine] sorted ascending by time in milliseconds.
     */
    fun parse(lrcContent: String?): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()

        val parsedLines = mutableListOf<LyricLine>()
        val rawLines = lrcContent.lines()

        for (rawLine in rawLines) {
            val trimmedLine = rawLine.trim()
            if (trimmedLine.isEmpty()) continue

            // Find all timestamps in this line
            val matches = timestampRegex.findAll(trimmedLine).toList()
            if (matches.isEmpty()) {
                // Skips metadata tags like [ar: Artist], [ti: Title], [al: Album], [length: ...], etc.
                continue
            }

            // Remove all timestamps from the line to get the clean lyric text
            val text = trimmedLine.replace(timestampRegex, "").trim()
            if (text.isEmpty()) continue

            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionStr = match.groupValues.getOrNull(3).orEmpty()

                val millis = when {
                    fractionStr.isEmpty() -> 0L
                    fractionStr.length == 1 -> fractionStr.toLong() * 100L
                    fractionStr.length == 2 -> fractionStr.toLong() * 10L
                    fractionStr.length >= 3 -> fractionStr.take(3).toLong()
                    else -> 0L
                }

                val totalTimeMs = (minutes * 60_000L) + (seconds * 1_000L) + millis
                parsedLines.add(LyricLine(time = totalTimeMs, text = text))
            }
        }

        return parsedLines.sortedBy { it.time }
    }
}

