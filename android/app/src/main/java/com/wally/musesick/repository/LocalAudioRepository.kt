package com.wally.musesick.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class LocalAudioRepository(private val context: Context) {

    suspend fun getLocalAudioTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val duration = c.getLong(durationCol)
                    val data = c.getString(dataCol) ?: ""
                    val mime = c.getString(mimeCol)?.lowercase(Locale.ROOT) ?: ""
                    val size = c.getLong(sizeCol)
                    val albumId = c.getLong(albumIdCol)

                    val format = detectAudioFormat(data, mime)
                    // We only accept mp3, m4a, flac or related supported formats
                    if (format != null) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        val albumArtUri = "content://media/external/audio/albumart/$albumId"

                        tracks.add(
                            Track(
                                id = "local_$id",
                                title = cleanTitle(title, data),
                                artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                                durationMs = duration,
                                thumbnailUrl = albumArtUri,
                                contentUri = contentUri,
                                isLocal = true,
                                audioFormat = format,
                                bitrate = formatBitrateBadge(format),
                                sizeFormatted = formatFileSize(size)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }

    suspend fun getTrackFromUri(uri: Uri): Track? = withContext(Dispatchers.IO) {
        try {
            var displayName = "Unknown Audio"
            var size = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

            var title = displayName
            var artist = "Local File"
            var durationMs = 0L

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank()) artist = metaArtist
                if (!metaDuration.isNullOrBlank()) durationMs = metaDuration.toLongOrNull() ?: 0L
                retriever.release()
            } catch (e: Exception) {
                // Keep fallback title & artist
            }

            val format = detectAudioFormat(displayName, "") ?: AudioFormat.MP3

            Track(
                id = uri.toString(),
                title = cleanTitle(title, displayName),
                artist = artist,
                durationMs = durationMs,
                thumbnailUrl = null,
                contentUri = uri.toString(),
                isLocal = true,
                audioFormat = format,
                bitrate = formatBitrateBadge(format),
                sizeFormatted = formatFileSize(size)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun detectAudioFormat(pathOrName: String, mime: String): AudioFormat? {
        val lower = pathOrName.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".flac") || mime.contains("flac") -> AudioFormat.FLAC
            lower.endsWith(".m4a") || lower.endsWith(".aac") || mime.contains("mp4") || mime.contains("m4a") || mime.contains("aac") -> AudioFormat.M4A
            lower.endsWith(".mp3") || mime.contains("mpeg") || mime.contains("mp3") -> AudioFormat.MP3
            else -> null
        }
    }

    private fun cleanTitle(title: String, filename: String): String {
        val t = title.trim()
        if (t.isNotEmpty() && t != "<unknown>") return t
        return File(filename).nameWithoutExtension.ifEmpty { "Audio File" }
    }

    private fun formatBitrateBadge(format: AudioFormat): String {
        return when (format) {
            AudioFormat.FLAC -> "FLAC LOSSLESS"
            AudioFormat.M4A -> "M4A / AAC"
            AudioFormat.MP3 -> "MP3 AUDIO"
            AudioFormat.YOUTUBE -> "YT STREAM"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.ROOT, "%.1f MB", mb)
    }
}

