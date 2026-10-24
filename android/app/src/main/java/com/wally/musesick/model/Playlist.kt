package com.wally.musesick.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val imageUri: String? = null,
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
data class YouTubePlaylistData(
    val id: String,
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val tracks: List<Track> = emptyList()
)

