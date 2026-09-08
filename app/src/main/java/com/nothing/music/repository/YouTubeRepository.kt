package com.nothing.music.repository

import com.nothing.music.model.AudioFormat
import com.nothing.music.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class YouTubeRepository {

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/search")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                setRequestProperty("Origin", "https://music.youtube.com")
            }

            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("query", query)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (connection.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                parseSearchResponse(responseText)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseSearchResponse(jsonString: String): List<Track> {
        val tracks = mutableListOf<Track>()
        val seenIds = mutableSetOf<String>()

        try {
            val root = JSONObject(jsonString)
            fun walk(obj: Any?) {
                when (obj) {
                    is JSONObject -> {
                        if (obj.has("musicResponsiveListItemRenderer")) {
                            val item = obj.getJSONObject("musicResponsiveListItemRenderer")
                            val track = parseTrackItem(item)
                            if (track != null && !seenIds.contains(track.id)) {
                                seenIds.add(track.id)
                                tracks.add(track)
                            }
                        }
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            walk(obj.get(keys.next()))
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until obj.length()) {
                            walk(obj.get(i))
                        }
                    }
                }
            }

            walk(root)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tracks
    }

    private fun parseTrackItem(item: JSONObject): Track? {
        try {
            var title = ""
            var artist = ""
            var videoId = ""
            var durationMs = 0L

            // Extract videoId
            if (item.has("playlistItemData")) {
                videoId = item.getJSONObject("playlistItemData").optString("videoId")
            }

            // Extract flex columns (title & artist/duration)
            if (item.has("flexColumns")) {
                val flexCols = item.getJSONArray("flexColumns")
                if (flexCols.length() > 0) {
                    val col0 = flexCols.getJSONObject(0)
                        .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    val runs0 = col0?.optJSONObject("text")?.optJSONArray("runs")
                    if (runs0 != null && runs0.length() > 0) {
                        val firstRun = runs0.getJSONObject(0)
                        title = firstRun.optString("text", "")
                        if (videoId.isEmpty() && firstRun.has("navigationEndpoint")) {
                            videoId = firstRun.getJSONObject("navigationEndpoint")
                                .optJSONObject("watchEndpoint")
                                ?.optString("videoId", "") ?: ""
                        }
                    }
                }

                if (flexCols.length() > 1) {
                    val col1 = flexCols.getJSONObject(1)
                        .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    val runs1 = col1?.optJSONObject("text")?.optJSONArray("runs")
                    if (runs1 != null) {
                        val sb = StringBuilder()
                        for (i in 0 until runs1.length()) {
                            val text = runs1.getJSONObject(i).optString("text", "")
                            // Check if text is duration (e.g. "3:45")
                            if (text.matches(Regex("""\d{1,2}:\d{2}"""))) {
                                durationMs = parseDurationText(text)
                            } else {
                                sb.append(text)
                            }
                        }
                        artist = sb.toString().trim().trim('•', ' ', ',')
                    }
                }
            }

            if (videoId.isEmpty() || title.isEmpty()) return null

            // Extract thumbnail
            var thumbUrl: String? = null
            if (item.has("thumbnail")) {
                val thumbs = item.optJSONObject("thumbnail")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                    ?.optJSONArray("thumbnails")
                if (thumbs != null && thumbs.length() > 0) {
                    thumbUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                }
            }

            return Track(
                id = videoId,
                title = title,
                artist = if (artist.isNotEmpty()) artist else "YouTube Music",
                durationMs = durationMs,
                thumbnailUrl = thumbUrl,
                isLocal = false,
                audioFormat = AudioFormat.YOUTUBE,
                bitrate = "HQ AUDIO"
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseDurationText(text: String): Long {
        return try {
            val parts = text.split(":")
            if (parts.size == 2) {
                val min = parts[0].toLong()
                val sec = parts[1].toLong()
                (min * 60 + sec) * 1000L
            } else if (parts.size == 3) {
                val hr = parts[0].toLong()
                val min = parts[1].toLong()
                val sec = parts[2].toLong()
                (hr * 3600 + min * 60 + sec) * 1000L
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        val QUICK_CATEGORIES = listOf(
            "TOP HITS",
            "LOFI BEATS",
            "SYNTHWAVE",
            "CHILL",
            "INDIE",
            "ROCK",
            "AMBIENT",
            "ELECTRONIC"
        )
    }
}

