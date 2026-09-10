package com.wally.musesick.repository

import android.content.Context
import com.wally.musesick.model.Artist
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.Playlist
import com.wally.musesick.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SuggestedPlaylistsRepository(
    private val context: Context,
    private val youtubeRepository: YouTubeRepository,
    private val playCountRepository: PlayCountRepository
) {

    private val playlistsFile: File
        get() = File(context.filesDir, "suggested_playlists.json")

    private val mutex = Mutex()

    companion object {
        const val PLAYLIST_TOP_SONGS = "suggested_top_songs"
        const val PLAYLIST_NEW_RELEASES = "suggested_new_releases"
        const val PLAYLIST_GENRE_DISCOVERIES = "suggested_genre_discoveries"
        const val PLAYLIST_MOST_PLAYED = "suggested_most_played"
        const val PLAYLIST_RELATED_ARTISTS = "suggested_related_artists"

        private const val REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private fun computeFingerprint(artists: List<Artist>): String {
        return artists.map { it.id }.sorted().joinToString(",")
    }

    suspend fun getCachedPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadPlaylistsInternal().first
        }
    }

    suspend fun shouldRefresh(favoriteArtists: List<Artist>): Boolean = withContext(Dispatchers.IO) {
        if (favoriteArtists.isEmpty()) return@withContext false
        mutex.withLock {
            val (cached, lastRefresh, fingerprint) = loadPlaylistsInternal()
            if (cached.isEmpty() || cached.size < 5) return@withLock true
            val now = System.currentTimeMillis()
            if (now - lastRefresh > REFRESH_INTERVAL_MS) return@withLock true
            if (fingerprint != computeFingerprint(favoriteArtists)) return@withLock true
            false
        }
    }

    suspend fun generateOrRefreshPlaylists(favoriteArtists: List<Artist>): List<Playlist> = withContext(Dispatchers.IO) {
        if (favoriteArtists.isEmpty()) return@withContext emptyList()

        val fingerprint = computeFingerprint(favoriteArtists)
        val now = System.currentTimeMillis()

        // 1. Top Songs Mix (interleaved top songs of favorite artists)
        val topSongs = youtubeRepository.fetchTopSongsForArtists(favoriteArtists, maxPerArtist = 5)
        val topSongsCover = topSongs.firstOrNull()?.thumbnailUrl ?: favoriteArtists.firstOrNull()?.thumbnailUrl
        val playlistTopSongs = Playlist(
            id = PLAYLIST_TOP_SONGS,
            title = "Top Songs Mix",
            imageUri = topSongsCover,
            tracks = topSongs,
            createdAt = now
        )

        // 2. Recent Releases
        val recentTracks = youtubeRepository.fetchRecentReleasesForArtists(favoriteArtists, maxTracks = 25)
        val recentCover = recentTracks.firstOrNull()?.thumbnailUrl ?: favoriteArtists.getOrNull(1)?.thumbnailUrl ?: topSongsCover
        val playlistNewReleases = Playlist(
            id = PLAYLIST_NEW_RELEASES,
            title = "Recent Releases",
            imageUri = recentCover,
            tracks = recentTracks.ifEmpty { topSongs.shuffled() },
            createdAt = now
        )

        // 3. Genre Discoveries
        val genreTracks = youtubeRepository.fetchGenrePopularSongs(favoriteArtists, maxTracks = 25)
        val genreCover = genreTracks.firstOrNull()?.thumbnailUrl ?: favoriteArtists.getOrNull(2)?.thumbnailUrl ?: topSongsCover
        val playlistGenreDiscoveries = Playlist(
            id = PLAYLIST_GENRE_DISCOVERIES,
            title = "Genre Discoveries",
            imageUri = genreCover,
            tracks = genreTracks.ifEmpty { topSongs },
            createdAt = now
        )

        // 4. Most Played (from internal JSON play counts)
        val mostPlayedTracks = playCountRepository.getMostPlayedTracks(limit = 30)
        val mostPlayedList = if (mostPlayedTracks.isNotEmpty()) {
            mostPlayedTracks
        } else {
            // Fallback for new installs: top favorite songs so playlist is not empty
            topSongs.take(15)
        }
        val mostPlayedCover = mostPlayedList.firstOrNull()?.thumbnailUrl ?: topSongsCover
        val playlistMostPlayed = Playlist(
            id = PLAYLIST_MOST_PLAYED,
            title = "Most Played",
            imageUri = mostPlayedCover,
            tracks = mostPlayedList,
            createdAt = now
        )

        // 5. Related Artists Mix
        val relatedTracks = youtubeRepository.fetchRelatedArtistsSongs(favoriteArtists, maxTracks = 25)
        val relatedCover = relatedTracks.firstOrNull()?.thumbnailUrl ?: favoriteArtists.lastOrNull()?.thumbnailUrl ?: topSongsCover
        val playlistRelatedArtists = Playlist(
            id = PLAYLIST_RELATED_ARTISTS,
            title = "Related Artists Mix",
            imageUri = relatedCover,
            tracks = relatedTracks.ifEmpty { topSongs.reversed() },
            createdAt = now
        )

        val result = listOf(
            playlistTopSongs,
            playlistNewReleases,
            playlistGenreDiscoveries,
            playlistMostPlayed,
            playlistRelatedArtists
        )

        mutex.withLock {
            savePlaylistsInternal(result, now, fingerprint)
        }

        result
    }

    private fun loadPlaylistsInternal(): Triple<List<Playlist>, Long, String> {
        if (!playlistsFile.exists()) return Triple(emptyList(), 0L, "")
        return try {
            val root = JSONObject(playlistsFile.readText())
            val lastRefresh = root.optLong("lastRefreshTimestamp", 0L)
            val fingerprint = root.optString("artistsFingerprint", "")
            val array = root.optJSONArray("playlists") ?: JSONArray()
            val list = mutableListOf<Playlist>()
            for (i in 0 until array.length()) {
                val pObj = array.getJSONObject(i)
                val tracksArray = pObj.optJSONArray("tracks") ?: JSONArray()
                val tracks = mutableListOf<Track>()
                for (j in 0 until tracksArray.length()) {
                    val tObj = tracksArray.getJSONObject(j)
                    tracks.add(
                        Track(
                            id = tObj.getString("id"),
                            title = tObj.getString("title"),
                            artist = tObj.getString("artist"),
                            durationMs = tObj.optLong("durationMs", 0L),
                            thumbnailUrl = tObj.optString("thumbnailUrl").ifEmpty { null },
                            audioFormat = try {
                                AudioFormat.valueOf(tObj.optString("audioFormat", AudioFormat.YOUTUBE.name))
                            } catch (e: Exception) {
                                AudioFormat.YOUTUBE
                            }
                        )
                    )
                }
                list.add(
                    Playlist(
                        id = pObj.getString("id"),
                        title = pObj.getString("title"),
                        imageUri = pObj.optString("imageUri").ifEmpty { null },
                        tracks = tracks,
                        createdAt = pObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            Triple(list, lastRefresh, fingerprint)
        } catch (e: Exception) {
            e.printStackTrace()
            Triple(emptyList(), 0L, "")
        }
    }

    private fun savePlaylistsInternal(playlists: List<Playlist>, timestamp: Long, fingerprint: String) {
        try {
            val root = JSONObject().apply {
                put("lastRefreshTimestamp", timestamp)
                put("artistsFingerprint", fingerprint)
                val array = JSONArray()
                for (p in playlists) {
                    val pObj = JSONObject().apply {
                        put("id", p.id)
                        put("title", p.title)
                        put("imageUri", p.imageUri ?: "")
                        put("createdAt", p.createdAt)
                        val tracksArray = JSONArray()
                        for (t in p.tracks) {
                            tracksArray.put(
                                JSONObject().apply {
                                    put("id", t.id)
                                    put("title", t.title)
                                    put("artist", t.artist)
                                    put("durationMs", t.durationMs)
                                    put("thumbnailUrl", t.thumbnailUrl ?: "")
                                    put("audioFormat", t.audioFormat.name)
                                }
                            )
                        }
                        put("tracks", tracksArray)
                    }
                    array.put(pObj)
                }
                put("playlists", array)
            }
            playlistsFile.writeText(root.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

