package com.wally.musesick

import com.wally.musesick.repository.LyricsRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsRepositoryTest {

    private val repository = LyricsRepository()

    @Test
    fun testCleanTrackTitle_removesSuffixes() {
        assertEquals("Blinding Lights", repository.cleanTrackTitle("Blinding Lights (Official Video)"))
        assertEquals("Starboy", repository.cleanTrackTitle("Starboy [Official Audio]"))
        assertEquals("Save Your Tears", repository.cleanTrackTitle("Save Your Tears (Official Music Video)"))
        assertEquals("In Your Eyes", repository.cleanTrackTitle("In Your Eyes (Lyric Video)"))
        assertEquals("After Hours", repository.cleanTrackTitle("After Hours (Audio)"))
        assertEquals("Faith", repository.cleanTrackTitle("Faith (Visualizer)"))
        assertEquals("Yesterday", repository.cleanTrackTitle("Yesterday (Remastered 2009)"))
        assertEquals("Levitating", repository.cleanTrackTitle("Levitating (feat. DaBaby)"))
        assertEquals("Levitating", repository.cleanTrackTitle("Levitating [feat. DaBaby]"))
    }

    @Test
    fun testCleanArtistName_removesTopic() {
        assertEquals("The Weeknd", repository.cleanArtistName("The Weeknd - Topic"))
        assertEquals("Dua Lipa", repository.cleanArtistName("Dua Lipa"))
    }
}

