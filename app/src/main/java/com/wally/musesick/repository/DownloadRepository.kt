package com.wally.musesick.repository

import android.content.Context
import android.util.Log
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class DownloadedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val localAudioPath: String,
    val localThumbnailPath: String?,
    val fileSizeBytes: Long,
    val format: String = "M4A",
    val bitrate: String? = null,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    val fileSizeFormatted: String
        get() = formatFileSize(fileSizeBytes)

    fun toTrack(): Track {
        return Track(
            id = id,
            title = title,
            artist = artist,
            durationMs = durationMs,
            thumbnailUrl = localThumbnailPath ?: thumbnailUrl,
            contentUri = localAudioPath,
            isLocal = true,
            audioFormat = AudioFormat.M4A,
            bitrate = bitrate,
            sizeFormatted = fileSizeFormatted
        )
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var size = bytes.toDouble()
            var unitIndex = 0
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            return String.format(Locale.US, "%.1f %s", size, units[unitIndex])
        }

        fun fromJson(json: JSONObject): DownloadedTrack {
            return DownloadedTrack(
                id = json.getString("id"),
                title = json.optString("title", "Unknown Track"),
                artist = json.optString("artist", "Unknown Artist"),
                durationMs = json.optLong("durationMs", 0L),
                thumbnailUrl = json.optString("thumbnailUrl").takeIf { !it.isNullOrBlank() },
                localAudioPath = json.getString("localAudioPath"),
                localThumbnailPath = json.optString("localThumbnailPath").takeIf { !it.isNullOrBlank() },
                fileSizeBytes = json.optLong("fileSizeBytes", 0L),
                format = json.optString("format", "M4A"),
                bitrate = json.optString("bitrate").takeIf { !it.isNullOrBlank() },
                downloadedAt = json.optLong("downloadedAt", System.currentTimeMillis())
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("artist", artist)
            put("durationMs", durationMs)
            put("thumbnailUrl", thumbnailUrl)
            put("localAudioPath", localAudioPath)
            put("localThumbnailPath", localThumbnailPath)
            put("fileSizeBytes", fileSizeBytes)
            put("format", format)
            put("bitrate", bitrate)
            put("downloadedAt", downloadedAt)
        }
    }
}

class DownloadRepository private constructor(
    private val context: Context,
    private val ytRepository: YouTubeRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val downloadsDir: File = File(context.filesDir, "downloads").apply {
        if (!exists()) mkdirs()
    }
    private val manifestFile: File = File(downloadsDir, "downloads_manifest.json")

    private val _downloads = MutableStateFlow<Map<String, DownloadedTrack>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadedTrack>> = _downloads.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, Float>> = _activeDownloads.asStateFlow()

    val totalStorageFormatted: StateFlow<String> = _downloads.map { map ->
        val totalBytes = map.values.sumOf { it.fileSizeBytes }
        DownloadedTrack.formatFileSize(totalBytes)
    }.stateIn(scope, SharingStarted.Eagerly, "0 MB")

    init {
        loadManifest()
    }

    private fun loadManifest() {
        try {
            if (!manifestFile.exists()) {
                _downloads.value = emptyMap()
                return
            }
            val text = manifestFile.readText()
            if (text.isBlank()) return
            val arr = JSONArray(text)
            val map = mutableMapOf<String, DownloadedTrack>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val track = DownloadedTrack.fromJson(obj)
                // Verify the audio file exists on disk
                if (File(track.localAudioPath).exists()) {
                    map[track.id] = track
                }
            }
            _downloads.value = map
        } catch (e: Exception) {
            Log.e(TAG, "Error loading downloads manifest", e)
        }
    }

    private fun saveManifest() {
        try {
            val arr = JSONArray()
            _downloads.value.values.forEach { arr.put(it.toJson()) }
            val tempFile = File(downloadsDir, "manifest.tmp")
            tempFile.writeText(arr.toString())
            if (tempFile.exists()) {
                tempFile.renameTo(manifestFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving downloads manifest", e)
        }
    }

    fun isDownloaded(trackId: String): Boolean {
        val dt = _downloads.value[trackId] ?: return false
        return File(dt.localAudioPath).exists()
    }

    fun getDownloadedTrack(trackId: String): DownloadedTrack? {
        val dt = _downloads.value[trackId] ?: return null
        return if (File(dt.localAudioPath).exists()) dt else null
    }

    fun getAllDownloadedTracks(): List<Track> {
        return _downloads.value.values
            .filter { File(it.localAudioPath).exists() }
            .sortedByDescending { it.downloadedAt }
            .map { it.toTrack() }
    }

    suspend fun downloadTrack(track: Track): Result<DownloadedTrack> = withContext(Dispatchers.IO) {
        val trackId = track.id
        if (trackId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid track id"))
        }

        // Already downloaded
        val existing = getDownloadedTrack(trackId)
        if (existing != null) {
            return@withContext Result.success(existing)
        }

        // Already in progress
        if (_activeDownloads.value.containsKey(trackId)) {
            return@withContext Result.failure(IllegalStateException("Download already in progress"))
        }

        _activeDownloads.value = _activeDownloads.value + (trackId to 0.05f)

        val tmpAudioFile = File(downloadsDir, "$trackId.tmp")
        val targetArtworkFile = File(downloadsDir, "$trackId.jpg")

        try {
            // 1. Resolve Audio Stream URL via multi-tier InnerTube clients
            val streamInfo = ytRepository.extractAudioStreamUrl(trackId)
                ?: throw IllegalStateException("Audio stream could not be resolved for offline download")

            _activeDownloads.value = _activeDownloads.value + (trackId to 0.15f)

            val ext = when {
                streamInfo.mimeType.contains("webm") -> "webm"
                streamInfo.mimeType.contains("audio/mp4") -> "m4a"
                streamInfo.mimeType.contains("video/mp4") -> "mp4"
                else -> "m4a"
            }
            val targetAudioFile = File(downloadsDir, "$trackId.$ext")

            // 2. Download Audio Stream
            val url = URL(streamInfo.url)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("User-Agent", "com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw IOException("Server returned HTTP $responseCode: $errorMsg")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: streamInfo.contentLength
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tmpAudioFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var lastReportedPercent = 15

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        if (totalBytes > 0) {
                            val percent = (15 + ((downloadedBytes.toDouble() / totalBytes) * 75)).toInt()
                            if (percent - lastReportedPercent >= 3) {
                                lastReportedPercent = percent
                                _activeDownloads.value = _activeDownloads.value + (trackId to (percent / 100f))
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (!tmpAudioFile.exists() || tmpAudioFile.length() == 0L) {
                throw IllegalStateException("Downloaded file is empty")
            }

            // Rename tmp file to target m4a
            if (targetAudioFile.exists()) targetAudioFile.delete()
            tmpAudioFile.renameTo(targetAudioFile)

            _activeDownloads.value = _activeDownloads.value + (trackId to 0.92f)

            // 3. Download Artwork Thumbnail (high-res if available)
            val artworkUrl = track.highResThumbnailUrl ?: track.thumbnailUrl
            var localArtworkPath: String? = null
            if (!artworkUrl.isNullOrBlank()) {
                try {
                    downloadFile(artworkUrl, targetArtworkFile)
                    if (targetArtworkFile.exists() && targetArtworkFile.length() > 0) {
                        localArtworkPath = targetArtworkFile.absolutePath
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to download artwork for $trackId", e)
                }
            }

            _activeDownloads.value = _activeDownloads.value + (trackId to 1.0f)

            val formatStr = when {
                streamInfo.mimeType.contains("webm") -> "WEBM"
                streamInfo.mimeType.contains("audio/mp4") -> "M4A"
                streamInfo.mimeType.contains("video/mp4") -> "MP4"
                else -> "M4A"
            }

            // 4. Create metadata entry
            val downloaded = DownloadedTrack(
                id = trackId,
                title = track.title,
                artist = track.artist,
                durationMs = if (track.durationMs > 0) track.durationMs else streamInfo.durationMs,
                thumbnailUrl = track.thumbnailUrl,
                localAudioPath = targetAudioFile.absolutePath,
                localThumbnailPath = localArtworkPath,
                fileSizeBytes = targetAudioFile.length(),
                format = formatStr,
                bitrate = "${streamInfo.bitrate / 1000} kbps",
                downloadedAt = System.currentTimeMillis()
            )

            _downloads.value = _downloads.value + (trackId to downloaded)
            saveManifest()

            Result.success(downloaded)
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading track $trackId", e)
            if (tmpAudioFile.exists()) tmpAudioFile.delete()
            Result.failure(e)
        } finally {
            _activeDownloads.value = _activeDownloads.value - trackId
        }
    }

    private fun downloadFile(urlStr: String, destination: File) {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        conn.inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
    }

    fun removeDownload(trackId: String): Boolean {
        try {
            val track = _downloads.value[trackId]
            if (track != null) {
                val audioFile = File(track.localAudioPath)
                if (audioFile.exists()) audioFile.delete()
                track.localThumbnailPath?.let {
                    val artFile = File(it)
                    if (artFile.exists()) artFile.delete()
                }
            } else {
                downloadsDir.listFiles()?.filter { it.name.startsWith("$trackId.") }?.forEach { it.delete() }
            }
            _downloads.value = _downloads.value - trackId
            saveManifest()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing download $trackId", e)
            return false
        }
    }

    fun removeAllDownloads(): Boolean {
        return try {
            downloadsDir.listFiles()?.forEach { it.delete() }
            _downloads.value = emptyMap()
            saveManifest()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing all downloads", e)
            false
        }
    }

    companion object {
        private const val TAG = "DownloadRepository"

        @Volatile
        private var instance: DownloadRepository? = null

        fun getInstance(
            context: Context,
            ytRepository: YouTubeRepository = YouTubeRepository()
        ): DownloadRepository {
            return instance ?: synchronized(this) {
                instance ?: DownloadRepository(context.applicationContext, ytRepository).also {
                    instance = it
                }
            }
        }
    }
}

