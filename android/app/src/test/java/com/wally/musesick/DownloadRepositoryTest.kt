package com.wally.musesick

import com.wally.musesick.model.AudioFormat
import com.wally.musesick.repository.DownloadedTrack
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadRepositoryTest {

    @Test
    fun testDownloadedTrackSerialization() {
        val original = DownloadedTrack(
            id = "test_vid_123",
            title = "Starboy",
            artist = "The Weeknd",
            durationMs = 230000L,
            thumbnailUrl = "https://example.com/thumb.jpg",
            localAudioPath = "/data/user/0/com.wally.musesick/files/downloads/test_vid_123.m4a",
            localThumbnailPath = "/data/user/0/com.wally.musesick/files/downloads/test_vid_123.jpg",
            fileSizeBytes = 4500000L,
            format = "M4A",
            bitrate = "128 kbps",
            downloadedAt = 1700000000000L
        )

        val json = original.toJson()
        val parsed = DownloadedTrack.fromJson(json)

        assertEquals(original.id, parsed.id)
        assertEquals(original.title, parsed.title)
        assertEquals(original.artist, parsed.artist)
        assertEquals(original.durationMs, parsed.durationMs)
        assertEquals(original.thumbnailUrl, parsed.thumbnailUrl)
        assertEquals(original.localAudioPath, parsed.localAudioPath)
        assertEquals(original.localThumbnailPath, parsed.localThumbnailPath)
        assertEquals(original.fileSizeBytes, parsed.fileSizeBytes)
        assertEquals(original.format, parsed.format)
        assertEquals(original.bitrate, parsed.bitrate)
        assertEquals(original.downloadedAt, parsed.downloadedAt)
    }

    @Test
    fun testToTrackConversion() {
        val downloaded = DownloadedTrack(
            id = "vid_999",
            title = "Blinding Lights",
            artist = "The Weeknd",
            durationMs = 200000L,
            thumbnailUrl = "https://example.com/remote.jpg",
            localAudioPath = "/storage/downloads/vid_999.m4a",
            localThumbnailPath = "/storage/downloads/vid_999.jpg",
            fileSizeBytes = 5242880L, // 5 MB
            format = "M4A",
            bitrate = "128 kbps"
        )

        val track = downloaded.toTrack()

        assertEquals("vid_999", track.id)
        assertEquals("Blinding Lights", track.title)
        assertEquals("The Weeknd", track.artist)
        assertEquals(200000L, track.durationMs)
        assertTrue(track.isLocal)
        assertEquals(AudioFormat.M4A, track.audioFormat)
        assertEquals("/storage/downloads/vid_999.m4a", track.contentUri)
        assertEquals("/storage/downloads/vid_999.jpg", track.thumbnailUrl)
        assertEquals("5.0 MB", track.sizeFormatted)
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", DownloadedTrack.formatFileSize(0))
        assertEquals("500.0 B", DownloadedTrack.formatFileSize(500))
        assertEquals("1.0 KB", DownloadedTrack.formatFileSize(1024))
        assertEquals("1.5 MB", DownloadedTrack.formatFileSize((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.4 GB", DownloadedTrack.formatFileSize((2.4 * 1024 * 1024 * 1024).toLong()))
    }
}

