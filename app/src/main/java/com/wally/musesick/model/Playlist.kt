package com.wally.musesick.model

import java.util.UUID

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val imageUri: String? = null,
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class YouTubePlaylistData(
    val id: String,
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val tracks: List<Track> = emptyList()
)

