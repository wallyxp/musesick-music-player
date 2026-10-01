package com.wally.musesick

import com.wally.musesick.util.DeepLinkHelper
import com.wally.musesick.util.DeepLinkTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkHelperTest {

    @Test
    fun parseYouTubeMusicWatchUrl() {
        val url = "https://music.youtube.com/watch?v=dQw4w9WgXcQ"
        val result = DeepLinkHelper.parseUrl(url)
        assertNotNull(result)
        assertTrue(result is DeepLinkTarget.Song)
        assertEquals("dQw4w9WgXcQ", (result as DeepLinkTarget.Song).videoId)
        assertNull(result.listId)
    }

    @Test
    fun parseYouTubeMusicWatchUrlWithPlaylist() {
        val url = "https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVMdQw4w9WgXcQ"
        val result = DeepLinkHelper.parseUrl(url)
        assertNotNull(result)
        assertTrue(result is DeepLinkTarget.Song)
        assertEquals("dQw4w9WgXcQ", (result as DeepLinkTarget.Song).videoId)
        assertEquals("RDAMVMdQw4w9WgXcQ", result.listId)
    }

    @Test
    fun parseStandardYouTubeWatchUrl() {
        val url = "https://www.youtube.com/watch?v=9bZkp7q19f0"
        val result = DeepLinkHelper.parseUrl(url)
        assertNotNull(result)
        assertTrue(result is DeepLinkTarget.Song)
        assertEquals("9bZkp7q19f0", (result as DeepLinkTarget.Song).videoId)
    }

    @Test
    fun parseShortUrl() {
        val url = "https://youtu.be/dQw4w9WgXcQ"
        val result = DeepLinkHelper.parseUrl(url)
        assertNotNull(result)
        assertTrue(result is DeepLinkTarget.Song)
        assertEquals("dQw4w9WgXcQ", (result as DeepLinkTarget.Song).videoId)
    }

    @Test
    fun parsePlaylistUrl() {
        val url = "https://music.youtube.com/playlist?list=PLrAlXlIdk7l2_eXWkKzJmHw6e3qg7g8h"
        val result = DeepLinkHelper.parseUrl(url)
        assertNotNull(result)
        assertTrue(result is DeepLinkTarget.Playlist)
        assertEquals("PLrAlXlIdk7l2_eXWkKzJmHw6e3qg7g8h", (result as DeepLinkTarget.Playlist).playlistId)
    }

    @Test
    fun parseAlbumPlaylistUrl() {
        val url = "https://music.youtube.com/playlist?list=OLAK5uy_kXz9v"
        val result = DeepLinkHelper.parseUrl(url)
        assertNotNull(result)
        assertTrue(result is DeepLinkTarget.Album)
        assertEquals("OLAK5uy_kXz9v", (result as DeepLinkTarget.Album).browseId)
    }

    @Test
    fun parseAlbumBrowseUrl() {
        val url = "https://music.youtube.com/browse/MPREb_0X0k2uC6R"
        val result = DeepLinkHelper.parseUrl(url)
        assertNotNull(result)
        assertTrue(result is DeepLinkTarget.Album)
        assertEquals("MPREb_0X0k2uC6R", (result as DeepLinkTarget.Album).browseId)
    }

    @Test
    fun parseArtistChannelUrl() {
        val url = "https://music.youtube.com/channel/UCuAXFkgsw1L7xaCfnd5JJOw"
        val result = DeepLinkHelper.parseUrl(url)
        assertNotNull(result)
        assertTrue(result is DeepLinkTarget.Artist)
        assertEquals("UCuAXFkgsw1L7xaCfnd5JJOw", (result as DeepLinkTarget.Artist).channelOrBrowseId)
    }

    @Test
    fun parseArtistBrowseUrl() {
        val url = "https://music.youtube.com/browse/UCnMq5nACWDzdJAf1cn_dmfQ"
        val result = DeepLinkHelper.parseUrl(url)
        assertNotNull(result)
        assertTrue(result is DeepLinkTarget.Artist)
        assertEquals("UCnMq5nACWDzdJAf1cn_dmfQ", (result as DeepLinkTarget.Artist).channelOrBrowseId)
    }

    @Test
    fun ignoreNonYouTubeUrls() {
        assertNull(DeepLinkHelper.parseUrl("https://spotify.com/track/12345"))
        assertNull(DeepLinkHelper.parseUrl("https://google.com"))
        assertNull(DeepLinkHelper.parseUrl(null))
        assertNull(DeepLinkHelper.parseUrl(""))
    }
}
