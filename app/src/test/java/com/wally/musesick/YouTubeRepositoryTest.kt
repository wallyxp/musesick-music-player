package com.wally.musesick

import com.wally.musesick.repository.YouTubeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeRepositoryTest {

    @Test
    fun testExtractPlaylistId_standardYtMusicUrl() {
        val url = "https://music.youtube.com/playlist?list=PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P"
        val id = YouTubeRepository.extractPlaylistId(url)
        assertEquals("PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P", id)
    }

    @Test
    fun testExtractPlaylistId_standardYouTubeUrl() {
        val url = "https://www.youtube.com/playlist?list=PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P"
        val id = YouTubeRepository.extractPlaylistId(url)
        assertEquals("PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P", id)
    }

    @Test
    fun testExtractPlaylistId_watchUrlWithList() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P&index=2"
        val id = YouTubeRepository.extractPlaylistId(url)
        assertEquals("PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P", id)
    }

    @Test
    fun testExtractPlaylistId_shortUrlWithList() {
        val url = "https://youtu.be/dQw4w9WgXcQ?list=PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P"
        val id = YouTubeRepository.extractPlaylistId(url)
        assertEquals("PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P", id)
    }

    @Test
    fun testExtractPlaylistId_browseUrl() {
        val url = "https://music.youtube.com/browse/VLPL4fGSIzQ845UqYvXw8c-tM8S1vGj3P"
        val id = YouTubeRepository.extractPlaylistId(url)
        assertEquals("VLPL4fGSIzQ845UqYvXw8c-tM8S1vGj3P", id)
    }

    @Test
    fun testExtractPlaylistId_rawIds() {
        assertEquals("PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P", YouTubeRepository.extractPlaylistId("PL4fGSIzQ845UqYvXw8c-tM8S1vGj3P"))
        assertEquals("OLAK5uy_kXfU5rL9z6X1xY1w5e4", YouTubeRepository.extractPlaylistId("OLAK5uy_kXfU5rL9z6X1xY1w5e4"))
        assertEquals("RDCLAK5uy_kXfU5rL9z6X1xY1w5e4", YouTubeRepository.extractPlaylistId("RDCLAK5uy_kXfU5rL9z6X1xY1w5e4"))
    }

    @Test
    fun testExtractPlaylistId_invalidInputs() {
        assertNull(YouTubeRepository.extractPlaylistId(""))
        assertNull(YouTubeRepository.extractPlaylistId("   "))
        assertNull(YouTubeRepository.extractPlaylistId("invalid"))
    }
}

