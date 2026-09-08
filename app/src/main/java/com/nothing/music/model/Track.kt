package com.nothing.music.model

enum class AudioFormat(val label: String) {
    MP3("MP3"),
    M4A("M4A"),
    FLAC("FLAC"),
    YOUTUBE("YT STREAM")
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long = 0L,
    val thumbnailUrl: String? = null,
    val contentUri: String? = null,
    val isLocal: Boolean = false,
    val audioFormat: AudioFormat = if (isLocal) AudioFormat.MP3 else AudioFormat.YOUTUBE,
    val bitrate: String? = null,
    val sizeFormatted: String? = null
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val subtitle: String? = null,
    val browseId: String? = null,
    val isCustom: Boolean = false,
    val isVisible: Boolean = true
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val year: String? = null,
    val browseId: String? = null
)

data class SearchResult(
    val songs: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList()
)
