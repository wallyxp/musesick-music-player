package com.wally.musesick.repository

import android.content.Context
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class RecentlyPlayedRepository(private val context: Context) {

    private val historyFile: File
        get() = File(context.filesDir, "recently_played.json")

    private val maxHistorySize = 50

    suspend fun getRecentlyPlayed(): List<Track> = withContext(Dispatchers.IO) {
        if (!historyFile.exists()) return@withContext emptyList()
        try {
            val jsonString = historyFile.readText()
            val array = JSONArray(jsonString)
            val result = mutableListOf<Track>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val formatStr = obj.optString("audioFormat", "YOUTUBE")
                val format = try {
                    AudioFormat.valueOf(formatStr)
                } catch (e: Exception) {
                    AudioFormat.YOUTUBE
                }
                result.add(
                    Track(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        durationMs = obj.optLong("durationMs", 0L),
                        thumbnailUrl = obj.optString("thumbnailUrl").ifEmpty { null },
                        contentUri = obj.optString("contentUri").ifEmpty { null },
                        isLocal = obj.optBoolean("isLocal", false),
                        audioFormat = format,
                        bitrate = obj.optString("bitrate").ifEmpty { null },
                        sizeFormatted = obj.optString("sizeFormatted").ifEmpty { null }
                    )
                )
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun saveTracks(tracks: List<Track>) = withContext(Dispatchers.IO) {
        try {
            val array = JSONArray()
            for (t in tracks) {
                array.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("artist", t.artist)
                        put("durationMs", t.durationMs)
                        put("thumbnailUrl", t.thumbnailUrl ?: "")
                        put("contentUri", t.contentUri ?: "")
                        put("isLocal", t.isLocal)
                        put("audioFormat", t.audioFormat.name)
                        put("bitrate", t.bitrate ?: "")
                        put("sizeFormatted", t.sizeFormatted ?: "")
                    }
                )
            }
            historyFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addTrack(track: Track): List<Track> = withContext(Dispatchers.IO) {
        val current = getRecentlyPlayed().toMutableList()
        // Deduplicate: remove existing entry with same id
        current.removeAll { it.id == track.id }
        // Insert at the front
        current.add(0, track)
        // Trim to max history size
        val trimmed = if (current.size > maxHistorySize) current.take(maxHistorySize) else current
        saveTracks(trimmed)
        trimmed
    }

    suspend fun clearAll(): List<Track> = withContext(Dispatchers.IO) {
        try {
            if (historyFile.exists()) {
                historyFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }
}

