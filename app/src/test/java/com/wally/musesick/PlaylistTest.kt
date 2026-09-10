package com.wally.musesick

import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.Playlist
import com.wally.musesick.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class PlaylistTest {

    @Test
    fun testPlaylistEditCopy() {
        val original = Playlist(
            id = "test-pl-1",
            title = "My Favorite Rock",
            imageUri = "/data/user/0/com.wally.musesick/files/playlist_images/img1.jpg",
            tracks = listOf(
                Track(id = "track-1", title = "Song A", artist = "Artist A", audioFormat = AudioFormat.FLAC),
                Track(id = "track-2", title = "Song B", artist = "Artist B", audioFormat = AudioFormat.MP3)
            ),
            createdAt = 123456789L
        )

        // 1. Edit title only
        val updatedTitle = original.copy(title = "Heavy Rock Classics")
        assertEquals("Heavy Rock Classics", updatedTitle.title)
        assertEquals(original.imageUri, updatedTitle.imageUri)
        assertEquals(2, updatedTitle.tracks.size)
        assertEquals(original.id, updatedTitle.id)
        assertEquals(original.createdAt, updatedTitle.createdAt)

        // 2. Edit photo
        val updatedPhoto = original.copy(imageUri = "/data/user/0/com.wally.musesick/files/playlist_images/img2.jpg")
        assertEquals(original.title, updatedPhoto.title)
        assertEquals("/data/user/0/com.wally.musesick/files/playlist_images/img2.jpg", updatedPhoto.imageUri)
        assertEquals(original.tracks, updatedPhoto.tracks)

        // 3. Remove photo
        val clearedPhoto = original.copy(imageUri = null)
        assertNull(clearedPhoto.imageUri)
        assertEquals(original.title, clearedPhoto.title)
        assertEquals(original.tracks, clearedPhoto.tracks)
    }

    @Test
    fun testPlaylistTracksIntegrity() {
        val original = Playlist(
            id = "pl-2",
            title = "Test Playlist",
            imageUri = "content://media/images/1",
            tracks = listOf(
                Track(id = "1", title = "Track 1", artist = "Artist 1"),
                Track(id = "2", title = "Track 2", artist = "Artist 2"),
                Track(id = "3", title = "Track 3", artist = "Artist 3")
            )
        )

        // Title and photo update must not affect track ordering or content
        val edited = original.copy(
            title = "Renamed Playlist",
            imageUri = "content://media/images/2"
        )

        assertEquals("Renamed Playlist", edited.title)
        assertEquals("content://media/images/2", edited.imageUri)
        assertEquals(3, edited.tracks.size)
        assertEquals("Track 1", edited.tracks[0].title)
        assertEquals("Track 2", edited.tracks[1].title)
        assertEquals("Track 3", edited.tracks[2].title)
    }

    @Test
    fun testPlaylistReorderTracks() {
        val tracks = listOf(
            Track(id = "1", title = "A", artist = "Artist"),
            Track(id = "2", title = "B", artist = "Artist"),
            Track(id = "3", title = "C", artist = "Artist")
        )
        val playlist = Playlist(title = "Reorder Test", tracks = tracks)

        // Move item at 0 to 2
        val mutable = playlist.tracks.toMutableList()
        val moved = mutable.removeAt(0)
        mutable.add(2, moved)
        val updated = playlist.copy(tracks = mutable)

        assertEquals("B", updated.tracks[0].title)
        assertEquals("C", updated.tracks[1].title)
        assertEquals("A", updated.tracks[2].title)
    }
}
