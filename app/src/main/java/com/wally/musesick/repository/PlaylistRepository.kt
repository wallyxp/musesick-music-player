package com.wally.musesick.repository

import android.content.Context
import android.net.Uri
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.Playlist
import com.wally.musesick.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class PlaylistRepository(private val context: Context) {

    private val playlistsFile: File
        get() = File(context.filesDir, "playlists.json")

    private val imagesDir: File
        get() = File(context.filesDir, "playlist_images").apply {
            if (!exists()) mkdirs()
        }

    suspend fun getPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        if (!playlistsFile.exists()) return@withContext emptyList()
        try {
            val jsonString = playlistsFile.readText()
            val array = JSONArray(jsonString)
            val result = mutableListOf<Playlist>()
            for (i in 0 until array.length()) {
                val pObj = array.getJSONObject(i)
                val tracksArray = pObj.optJSONArray("tracks") ?: JSONArray()
                val tracks = mutableListOf<Track>()
                for (j in 0 until tracksArray.length()) {
                    val tObj = tracksArray.getJSONObject(j)
                    val formatStr = tObj.optString("audioFormat", "YOUTUBE")
                    val format = try {
                        AudioFormat.valueOf(formatStr)
                    } catch (e: Exception) {
                        AudioFormat.YOUTUBE
                    }
                    tracks.add(
                        Track(
                            id = tObj.getString("id"),
                            title = tObj.getString("title"),
                            artist = tObj.getString("artist"),
                            durationMs = tObj.optLong("durationMs", 0L),
                            thumbnailUrl = tObj.optString("thumbnailUrl").ifEmpty { null },
                            contentUri = tObj.optString("contentUri").ifEmpty { null },
                            isLocal = tObj.optBoolean("isLocal", false),
                            audioFormat = format,
                            bitrate = tObj.optString("bitrate").ifEmpty { null },
                            sizeFormatted = tObj.optString("sizeFormatted").ifEmpty { null }
                        )
                    )
                }
                result.add(
                    Playlist(
                        id = pObj.getString("id"),
                        title = pObj.getString("title"),
                        imageUri = pObj.optString("imageUri").ifEmpty { null },
                        tracks = tracks,
                        createdAt = pObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun savePlaylists(playlists: List<Playlist>) = withContext(Dispatchers.IO) {
        try {
            val array = JSONArray()
            for (p in playlists) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("title", p.title)
                    put("imageUri", p.imageUri ?: "")
                    put("createdAt", p.createdAt)
                    val tArray = JSONArray()
                    for (t in p.tracks) {
                        tArray.put(
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
                    put("tracks", tArray)
                }
                array.put(pObj)
            }
            playlistsFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createPlaylist(title: String, imageUri: String?, initialTrack: Track? = null): Playlist {
        val validTrack = initialTrack?.let {
            if (it.id.isBlank()) it.copy(id = UUID.randomUUID().toString()) else it
        }
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            imageUri = imageUri,
            tracks = if (validTrack != null) listOf(validTrack) else emptyList(),
            createdAt = System.currentTimeMillis()
        )
        val existing = getPlaylists()
        savePlaylists(listOf(newPlaylist) + existing)
        return newPlaylist
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: Track): List<Playlist> {
        val validTrack = if (track.id.isBlank()) track.copy(id = UUID.randomUUID().toString()) else track
        val current = getPlaylists()
        val updated = current.map { playlist ->
            if (playlist.id == playlistId) {
                if (playlist.tracks.none { it.id == validTrack.id }) {
                    playlist.copy(tracks = playlist.tracks + validTrack)
                } else {
                    playlist
                }
            } else {
                playlist
            }
        }
        savePlaylists(updated)
        return updated
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): List<Playlist> {
        val current = getPlaylists()
        val updated = current.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(tracks = playlist.tracks.filter { it.id != trackId })
            } else {
                playlist
            }
        }
        savePlaylists(updated)
        return updated
    }

    suspend fun deletePlaylist(playlistId: String): List<Playlist> {
        val current = getPlaylists()
        val playlist = current.find { it.id == playlistId }
        playlist?.imageUri?.let { uriStr ->
            try {
                val f = File(uriStr)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                // ignore
            }
        }
        val updated = current.filter { it.id != playlistId }
        savePlaylists(updated)
        return updated
    }

    suspend fun updatePlaylist(playlistId: String, newTitle: String, newImageUri: String?): List<Playlist> {
        val current = getPlaylists()
        val oldPlaylist = current.find { it.id == playlistId }
        if (oldPlaylist != null && oldPlaylist.imageUri != newImageUri && oldPlaylist.imageUri != null) {
            try {
                val f = File(oldPlaylist.imageUri)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                // ignore
            }
        }
        val updated = current.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(
                    title = newTitle.trim(),
                    imageUri = newImageUri
                )
            } else {
                playlist
            }
        }
        savePlaylists(updated)
        return updated
    }

    suspend fun copyImageToInternalStorage(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val targetFile = File(imagesDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadImageToInternalStorage(imageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "yt_pl_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val targetFile = File(imagesDir, fileName)
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createPlaylistWithTracks(
        title: String,
        imagePath: String?,
        tracks: List<Track>
    ): Playlist = withContext(Dispatchers.IO) {
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            imageUri = imagePath,
            tracks = tracks,
            createdAt = System.currentTimeMillis()
        )
        val existing = getPlaylists()
        savePlaylists(listOf(newPlaylist) + existing)
        newPlaylist
    }

    suspend fun reorderTracksInPlaylist(playlistId: String, from: Int, to: Int): List<Playlist> {
        val current = getPlaylists()
        val updated = current.map { playlist ->
            if (playlist.id == playlistId) {
                val tracks = playlist.tracks.toMutableList()
                if (from in tracks.indices && to in tracks.indices) {
                    val item = tracks.removeAt(from)
                    tracks.add(to, item)
                }
                playlist.copy(tracks = tracks)
            } else {
                playlist
            }
        }
        savePlaylists(updated)
        return updated
    }

    fun exportPlaylistToJson(playlist: Playlist): String {
        val pObj = JSONObject().apply {
            put("id", playlist.id)
            put("title", playlist.title)
            put("imageUri", playlist.imageUri ?: "")
            put("createdAt", playlist.createdAt)
            val tArray = JSONArray()
            for (t in playlist.tracks) {
                tArray.put(
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
            put("tracks", tArray)
        }
        return pObj.toString(2)
    }

    fun importPlaylistFromJson(jsonString: String): Playlist? {
        return try {
            val pObj = JSONObject(jsonString)
            val tracksArray = pObj.optJSONArray("tracks") ?: JSONArray()
            val tracks = mutableListOf<Track>()
            for (j in 0 until tracksArray.length()) {
                val tObj = tracksArray.getJSONObject(j)
                val formatStr = tObj.optString("audioFormat", "YOUTUBE")
                val format = try { AudioFormat.valueOf(formatStr) } catch (e: Exception) { AudioFormat.YOUTUBE }
                tracks.add(
                    Track(
                        id = tObj.optString("id").ifEmpty { UUID.randomUUID().toString() },
                        title = tObj.getString("title"),
                        artist = tObj.getString("artist"),
                        durationMs = tObj.optLong("durationMs", 0L),
                        thumbnailUrl = tObj.optString("thumbnailUrl").ifEmpty { null },
                        contentUri = tObj.optString("contentUri").ifEmpty { null },
                        isLocal = tObj.optBoolean("isLocal", false),
                        audioFormat = format,
                        bitrate = tObj.optString("bitrate").ifEmpty { null },
                        sizeFormatted = tObj.optString("sizeFormatted").ifEmpty { null }
                    )
                )
            }
            Playlist(
                id = UUID.randomUUID().toString(),
                title = pObj.optString("title", "Imported Playlist"),
                imageUri = pObj.optString("imageUri").ifEmpty { null },
                tracks = tracks,
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveImportedPlaylist(playlist: Playlist): List<Playlist> {
        val existing = getPlaylists()
        val all = listOf(playlist) + existing
        savePlaylists(all)
        return all
    }
}

