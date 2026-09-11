package com.wally.musesick.repository

import com.wally.musesick.model.LyricLine
import com.wally.musesick.util.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class LyricsResult {
    data class Success(
        val lyrics: List<LyricLine>,
        val plainLyrics: String? = null,
        val isInstrumental: Boolean = false
    ) : LyricsResult()

    object Instrumental : LyricsResult()
    data class NotFound(val reason: String = "Lyrics not available") : LyricsResult()
    data class Error(val message: String) : LyricsResult()
}

class LyricsRepository {

    companion object {
        private const val BASE_URL = "https://lrclib.net"
        private const val USER_AGENT = "Musesick/1.1.3 (https://github.com/wallyxp/musesick-music-player)"
        private const val TIMEOUT_MS = 6000
    }

    /**
     * Fetches synchronized lyrics using the free, public LRCLib API (https://lrclib.net).
     *
     * @param trackName The name/title of the track
     * @param artistName The artist of the track
     * @param durationSeconds Duration of the track in total seconds
     * @return [LyricsResult] indicating Success with parsed [LyricLine] list, Instrumental, or clean fallbacks.
     */
    suspend fun fetchLyrics(
        trackName: String,
        artistName: String,
        durationSeconds: Long
    ): LyricsResult = withContext(Dispatchers.IO) {
        if (trackName.isBlank()) {
            return@withContext LyricsResult.NotFound("Lyrics not available")
        }

        val cleanTitle = cleanTrackTitle(trackName)
        val cleanArtist = cleanArtistName(artistName)

        // 1. First attempt: exact query to /api/get with title, artist and duration
        val exactResult = requestApiGet(cleanTitle, cleanArtist, durationSeconds)
        if (exactResult is LyricsResult.Success || exactResult is LyricsResult.Instrumental) {
            return@withContext exactResult
        }

        // 2. Second attempt if title had tags removed: try raw trackName if cleanTitle differed
        if (cleanTitle != trackName) {
            val rawResult = requestApiGet(trackName.trim(), cleanArtist, durationSeconds)
            if (rawResult is LyricsResult.Success || rawResult is LyricsResult.Instrumental) {
                return@withContext rawResult
            }
        }

        // 3. Third attempt: try /api/get without duration restriction (in case YouTube stream duration varies)
        if (durationSeconds > 0) {
            val noDurationResult = requestApiGet(cleanTitle, cleanArtist, 0L)
            if (noDurationResult is LyricsResult.Success || noDurationResult is LyricsResult.Instrumental) {
                return@withContext noDurationResult
            }
        }

        // 4. Fourth fallback: query search API /api/search?track_name=...&artist_name=...
        val searchResult = requestApiSearch(cleanTitle, cleanArtist)
        if (searchResult != null) {
            return@withContext searchResult
        }

        return@withContext LyricsResult.NotFound("Lyrics not available")
    }

    private fun requestApiGet(
        trackName: String,
        artistName: String,
        durationSeconds: Long
    ): LyricsResult {
        return try {
            val encodedTrack = URLEncoder.encode(trackName, "UTF-8")
            val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
            var urlString = "$BASE_URL/api/get?track_name=$encodedTrack&artist_name=$encodedArtist"
            if (durationSeconds > 0) {
                urlString += "&duration=$durationSeconds"
            }

            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                parseLrcLibJsonObject(JSONObject(responseText))
            } else if (responseCode == 404) {
                LyricsResult.NotFound("Lyrics not available")
            } else {
                LyricsResult.Error("Server returned code $responseCode")
            }
        } catch (e: Exception) {
            LyricsResult.Error(e.message ?: "Network error while fetching lyrics")
        }
    }

    private fun requestApiSearch(
        trackName: String,
        artistName: String
    ): LyricsResult? {
        return try {
            val encodedTrack = URLEncoder.encode(trackName, "UTF-8")
            val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
            val urlString = "$BASE_URL/api/search?track_name=$encodedTrack&artist_name=$encodedArtist"

            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonArray = JSONArray(responseText)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val result = parseLrcLibJsonObject(item)
                    if (result is LyricsResult.Success || result is LyricsResult.Instrumental) {
                        return result
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLrcLibJsonObject(json: JSONObject): LyricsResult {
        val isInstrumental = json.optBoolean("instrumental", false)
        if (isInstrumental) {
            return LyricsResult.Instrumental
        }

        val syncedLrc = json.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
        val plainLyrics = json.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }

        if (!syncedLrc.isNullOrBlank()) {
            val lines = LrcParser.parse(syncedLrc)
            if (lines.isNotEmpty()) {
                return LyricsResult.Success(
                    lyrics = lines,
                    plainLyrics = plainLyrics,
                    isInstrumental = false
                )
            }
        }

        // If no synced lyrics but plain lyrics exist, format them with synthesized timings or as instrumental/unavailable
        if (!plainLyrics.isNullOrBlank()) {
            val rawLines = plainLyrics.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (rawLines.isNotEmpty()) {
                // Return as plain lyrics container
                val lines = rawLines.mapIndexed { index, line ->
                    LyricLine(time = index * 3000L, text = line)
                }
                return LyricsResult.Success(
                    lyrics = lines,
                    plainLyrics = plainLyrics,
                    isInstrumental = false
                )
            }
        }

        return LyricsResult.NotFound("Lyrics not available")
    }

    /**
     * Cleans metadata noise from YouTube Music titles, e.g. "(Official Audio)", "[Lyric Video]".
     */
    fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("""(?i)\s*[\(\[](?:official\s*(?:music\s*)?(?:video|audio|visualizer)?|lyric\s*video|music\s*video|audio|lyrics?|visualizer|remastered(?:\s*\d{4})?)[\)\]]"""), "")
            .replace(Regex("""(?i)\s*[\(\[](?:feat|ft)\..*?[\)\]]"""), "")
            .trim()
    }

    /**
     * Cleans artist names (e.g. removes " - Topic" from YouTube generated artist channels).
     */
    fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex("""(?i)\s*-\s*Topic\s*$"""), "")
            .trim()
    }
}
