package com.wally.musesick.repository

import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class ImportedSpotifyPlaylist(
    val title: String,
    val coverUrl: String?,
    val tracks: List<Track>
)

class SpotifyRepository {

    companion object {
        fun extractPlaylistId(input: String): String? {
            val u = input.trim()
            if (u.contains("spotify:playlist:")) {
                return u.substringAfter("spotify:playlist:").substringBefore("?").substringBefore("/").trim().takeIf { it.isNotBlank() }
            }
            if (u.contains("/playlist/")) {
                return u.substringAfter("/playlist/").substringBefore("?").substringBefore("/").trim().takeIf { it.isNotBlank() }
            }
            return null
        }
    }

    suspend fun fetchPlaylistFromEmbed(
        playlistUrlOrId: String,
        ytRepo: YouTubeRepository
    ): ImportedSpotifyPlaylist? = withContext(Dispatchers.IO) {
        val playlistId = extractPlaylistId(playlistUrlOrId) ?: playlistUrlOrId.trim()
        if (playlistId.isEmpty()) return@withContext null

        try {
            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val html = executeGet(embedUrl) ?: return@withContext null

            val scriptMarker = "<script id=\"__NEXT_DATA__\" type=\"application/json\">"
            val startIndex = html.indexOf(scriptMarker)
            if (startIndex == -1) return@withContext null

            val jsonStart = startIndex + scriptMarker.length
            val jsonEnd = html.indexOf("</script>", jsonStart)
            if (jsonEnd == -1) return@withContext null

            val jsonString = html.substring(jsonStart, jsonEnd).trim()
            val root = JSONObject(jsonString)
            val entity = root.getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("state")
                .getJSONObject("data")
                .getJSONObject("entity")

            val title = entity.optString("title", entity.optString("name", "Spotify Playlist")).trim()
            val coverSources = entity.optJSONObject("coverArt")?.optJSONArray("sources")
            val coverUrl = coverSources?.let {
                if (it.length() > 0) it.getJSONObject(0).optString("url").takeIf { u -> u.isNotBlank() } else null
            }

            val trackListArray = entity.optJSONArray("trackList") ?: JSONArray()
            val rawTracks = mutableListOf<Triple<String, String, Long>>()

            for (i in 0 until trackListArray.length()) {
                val tObj = trackListArray.getJSONObject(i)
                val trackTitle = tObj.optString("title").trim()
                if (trackTitle.isBlank()) continue
                val artist = tObj.optString("subtitle", "Unknown Artist").trim()
                val durationMs = tObj.optLong("duration", 0L)
                rawTracks.add(Triple(trackTitle, artist, durationMs))
            }

            // Match tracks to YouTube streams in parallel (max 5 concurrent calls)
            val semaphore = Semaphore(5)
            val tracks = coroutineScope {
                rawTracks.mapIndexed { index, (tTitle, tArtist, durationMs) ->
                    async(Dispatchers.IO) {
                        try {
                            if (index < 40) {
                                semaphore.withPermit {
                                    val matched = ytRepo.searchTracks("$tTitle $tArtist").firstOrNull()
                                    if (matched != null && matched.id.isNotBlank()) {
                                        Track(
                                            id = matched.id,
                                            title = tTitle,
                                            artist = tArtist,
                                            durationMs = if (durationMs > 0) durationMs else matched.durationMs,
                                            thumbnailUrl = matched.thumbnailUrl ?: coverUrl,
                                            audioFormat = AudioFormat.YOUTUBE
                                        )
                                    } else {
                                        Track(
                                            id = "spotify_${UUID.randomUUID().toString().take(10)}",
                                            title = tTitle,
                                            artist = tArtist,
                                            durationMs = durationMs,
                                            thumbnailUrl = coverUrl,
                                            audioFormat = AudioFormat.YOUTUBE
                                        )
                                    }
                                }
                            } else {
                                Track(
                                    id = "spotify_${UUID.randomUUID().toString().take(10)}",
                                    title = tTitle,
                                    artist = tArtist,
                                    durationMs = durationMs,
                                    thumbnailUrl = coverUrl,
                                    audioFormat = AudioFormat.YOUTUBE
                                )
                            }
                        } catch (_: Exception) {
                            Track(
                                id = "spotify_${UUID.randomUUID().toString().take(10)}",
                                title = tTitle,
                                artist = tArtist,
                                durationMs = durationMs,
                                thumbnailUrl = coverUrl,
                                audioFormat = AudioFormat.YOUTUBE
                            )
                        }
                    }
                }.awaitAll()
            }

            ImportedSpotifyPlaylist(
                title = title,
                coverUrl = coverUrl,
                tracks = tracks
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun executeGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            conn.connectTimeout = 10000
            conn.readTimeout = 15000

            val code = conn.responseCode
            if (code in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn?.disconnect()
        }
    }
}
