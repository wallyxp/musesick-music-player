package com.wally.musesick.repository

import android.content.Context
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PlayRecord(
    val trackId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val audioFormat: AudioFormat,
    val playCount: Int,
    val lastPlayedAt: Long
)

class PlayCountRepository(private val context: Context) {

    private val playCountsFile: File
        get() = File(context.filesDir, "play_counts.json")

    private val mutex = Mutex()

    suspend fun recordPlay(track: Track): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val records = loadRecordsInternal().toMutableList()
            val index = records.indexOfFirst { it.trackId == track.id }
            val now = System.currentTimeMillis()

            if (index >= 0) {
                val existing = records[index]
                records[index] = existing.copy(
                    title = track.title,
                    artist = track.artist,
                    durationMs = if (track.durationMs > 0) track.durationMs else existing.durationMs,
                    thumbnailUrl = track.thumbnailUrl ?: existing.thumbnailUrl,
                    audioFormat = track.audioFormat,
                    playCount = existing.playCount + 1,
                    lastPlayedAt = now
                )
            } else {
                records.add(
                    PlayRecord(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        durationMs = track.durationMs,
                        thumbnailUrl = track.thumbnailUrl,
                        audioFormat = track.audioFormat,
                        playCount = 1,
                        lastPlayedAt = now
                    )
                )
            }

            saveRecordsInternal(records)
        }
    }

    suspend fun getMostPlayedTracks(limit: Int = 30): List<Track> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val records = loadRecordsInternal()
            records
                .sortedWith(compareByDescending<PlayRecord> { it.playCount }.thenByDescending { it.lastPlayedAt })
                .take(limit)
                .map { record ->
                    Track(
                        id = record.trackId,
                        title = record.title,
                        artist = record.artist,
                        durationMs = record.durationMs,
                        thumbnailUrl = record.thumbnailUrl,
                        audioFormat = record.audioFormat
                    )
                }
        }
    }

    suspend fun clearCounts(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (playCountsFile.exists()) {
                playCountsFile.delete()
            }
        }
    }

    private fun loadRecordsInternal(): List<PlayRecord> {
        if (!playCountsFile.exists()) return emptyList()
        return try {
            val content = playCountsFile.readText()
            val array = JSONArray(content)
            val result = mutableListOf<PlayRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    PlayRecord(
                        trackId = obj.getString("trackId"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        durationMs = obj.optLong("durationMs", 0L),
                        thumbnailUrl = obj.optString("thumbnailUrl").ifEmpty { null },
                        audioFormat = try {
                            AudioFormat.valueOf(obj.optString("audioFormat", AudioFormat.YOUTUBE.name))
                        } catch (e: Exception) {
                            AudioFormat.YOUTUBE
                        },
                        playCount = obj.optInt("playCount", 1),
                        lastPlayedAt = obj.optLong("lastPlayedAt", 0L)
                    )
                )
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveRecordsInternal(records: List<PlayRecord>) {
        try {
            val array = JSONArray()
            for (r in records) {
                val obj = JSONObject().apply {
                    put("trackId", r.trackId)
                    put("title", r.title)
                    put("artist", r.artist)
                    put("durationMs", r.durationMs)
                    put("thumbnailUrl", r.thumbnailUrl ?: "")
                    put("audioFormat", r.audioFormat.name)
                    put("playCount", r.playCount)
                    put("lastPlayedAt", r.lastPlayedAt)
                }
                array.put(obj)
            }
            playCountsFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

