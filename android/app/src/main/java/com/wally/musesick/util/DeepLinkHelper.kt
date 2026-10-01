package com.wally.musesick.util

import android.net.Uri
import java.net.URI

sealed class DeepLinkTarget {
    data class Song(val videoId: String, val listId: String? = null) : DeepLinkTarget()
    data class Playlist(val playlistId: String) : DeepLinkTarget()
    data class Album(val browseId: String) : DeepLinkTarget()
    data class Artist(val channelOrBrowseId: String) : DeepLinkTarget()
}

object DeepLinkHelper {

    fun parseUri(uri: Uri?): DeepLinkTarget? {
        if (uri == null) return null
        return parseUrl(uri.toString())
    }

    fun parseUrl(urlStr: String?): DeepLinkTarget? {
        if (urlStr.isNullOrBlank()) return null
        val trimmed = urlStr.trim()
        val parsedUri = try {
            URI(trimmed)
        } catch (e: Exception) {
            null
        }

        val host = (parsedUri?.host ?: extractHost(trimmed))?.lowercase() ?: return null
        val isYouTubeHost = host == "music.youtube.com" ||
                host == "youtube.com" ||
                host == "www.youtube.com" ||
                host == "m.youtube.com" ||
                host == "youtu.be"

        if (!isYouTubeHost) return null

        val path = parsedUri?.path ?: extractPath(trimmed)

        // 1. youtu.be/VIDEO_ID
        if (host == "youtu.be") {
            val videoId = path.trim('/').split('/').firstOrNull()?.trim()
            if (!videoId.isNullOrEmpty() && isValidVideoId(videoId)) {
                val list = extractQueryParam(trimmed, "list")
                return DeepLinkTarget.Song(videoId, list)
            }
            return null
        }

        // 2. /watch?v=VIDEO_ID
        if (path.startsWith("/watch")) {
            val videoId = extractQueryParam(trimmed, "v")
            val list = extractQueryParam(trimmed, "list")
            if (!videoId.isNullOrEmpty() && isValidVideoId(videoId)) {
                return DeepLinkTarget.Song(videoId, list)
            }
        }

        // 3. /playlist?list=PLAYLIST_ID
        if (path.startsWith("/playlist")) {
            val list = extractQueryParam(trimmed, "list")
            if (!list.isNullOrEmpty()) {
                return if (list.startsWith("OLAK") || list.startsWith("RDCLAK")) {
                    DeepLinkTarget.Album(list)
                } else {
                    DeepLinkTarget.Playlist(list)
                }
            }
        }

        // 4. /browse/ID
        if (path.startsWith("/browse")) {
            val segments = path.trim('/').split('/')
            val browseId = segments.getOrNull(1)?.trim() ?: extractQueryParam(trimmed, "id")
            if (!browseId.isNullOrEmpty()) {
                return when {
                    browseId.startsWith("UC") -> DeepLinkTarget.Artist(browseId)
                    browseId.startsWith("MPREb_") || browseId.startsWith("OLAK") -> DeepLinkTarget.Album(browseId)
                    browseId.startsWith("VL") || browseId.startsWith("PL") -> DeepLinkTarget.Playlist(browseId.removePrefix("VL"))
                    else -> DeepLinkTarget.Album(browseId)
                }
            }
        }

        // 5. /channel/CHANNEL_ID
        if (path.startsWith("/channel")) {
            val segments = path.trim('/').split('/')
            val channelId = segments.getOrNull(1)?.trim()
            if (!channelId.isNullOrEmpty()) {
                return DeepLinkTarget.Artist(channelId)
            }
        }

        // 6. /shorts/VIDEO_ID or /embed/VIDEO_ID
        if (path.startsWith("/shorts/") || path.startsWith("/embed/")) {
            val segments = path.trim('/').split('/')
            val videoId = segments.getOrNull(1)?.trim()
            if (!videoId.isNullOrEmpty() && isValidVideoId(videoId)) {
                return DeepLinkTarget.Song(videoId)
            }
        }

        return null
    }

    private fun extractQueryParam(url: String, key: String): String? {
        val regex = Regex("[?&]${Regex.escape(key)}=([^&#]+)")
        return regex.find(url)?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { null }
    }

    private fun extractHost(url: String): String? {
        val noScheme = url.substringAfter("://")
        return noScheme.substringBefore('/').substringBefore(':').ifEmpty { null }
    }

    private fun extractPath(url: String): String {
        val noScheme = url.substringAfter("://")
        val slashIdx = noScheme.indexOf('/')
        if (slashIdx == -1) return "/"
        val pathAndQuery = noScheme.substring(slashIdx)
        return pathAndQuery.substringBefore('?').substringBefore('#')
    }

    private fun isValidVideoId(id: String): Boolean {
        return id.length in 6..32 && id.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }
}
