package com.wally.musesick.repository

import com.wally.musesick.model.Album
import com.wally.musesick.model.Artist
import com.wally.musesick.model.ArtistDetailData
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.SearchResult
import com.wally.musesick.model.Track
import com.wally.musesick.model.YouTubePlaylistData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class YouTubeRepository {

    suspend fun searchAll(query: String): SearchResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = executeInnerTubeRequest(
                "https://music.youtube.com/youtubei/v1/search",
                JSONObject().apply {
                    put("context", createClientContext())
                    put("query", query)
                }
            ) ?: return@withContext SearchResult()

            parseCategorizedSearch(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            SearchResult()
        }
    }

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        searchAll(query).songs
    }

    suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<Artist>()
        val seenNames = mutableSetOf<String>()

        // 1. Query YouTube Music direct search
        val directSearch = searchAll(query)
        for (artist in directSearch.artists) {
            val clean = artist.name.trim()
            if (clean.isNotEmpty() && seenNames.add(clean.lowercase())) {
                results.add(artist.copy(thumbnailUrl = upgradeThumbnailUrl(artist.thumbnailUrl)))
            }
        }

        // 2. If fewer than 6, search specifically for "$query artist"
        if (results.size < 6) {
            val targetedSearch = searchAll("$query artist")
            for (artist in targetedSearch.artists) {
                val clean = artist.name.trim()
                if (clean.isNotEmpty() && seenNames.add(clean.lowercase())) {
                    results.add(artist.copy(thumbnailUrl = upgradeThumbnailUrl(artist.thumbnailUrl)))
                }
            }
        }

        // 3. Match from songs/albums if we still need more candidates
        if (results.size < 6) {
            for (song in directSearch.songs) {
                val name = song.artist.trim()
                if (name.isNotEmpty() && name.contains(query, ignoreCase = true) && seenNames.add(name.lowercase())) {
                    results.add(
                        Artist(
                            id = "artist_$name",
                            name = name,
                            thumbnailUrl = upgradeThumbnailUrl(song.thumbnailUrl),
                            subtitle = "Artist",
                            browseId = null
                        )
                    )
                }
            }
        }

        results
    }

    suspend fun getArtistDetails(artist: Artist): ArtistDetailData = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()
        val songs = mutableListOf<Track>()
        val videos = mutableListOf<Track>()
        var allSongsBrowseId: String? = null
        var allSongsParams: String? = null
        var allVideosBrowseId: String? = null
        var allVideosParams: String? = null

        // 1. If browseId is available, attempt browse
        if (!artist.browseId.isNullOrEmpty()) {
            try {
                val browseJson = executeInnerTubeRequest(
                    "https://music.youtube.com/youtubei/v1/browse",
                    JSONObject().apply {
                        put("context", createClientContext())
                        put("browseId", artist.browseId)
                    }
                )
                if (browseJson != null) {
                    val parsed = parseArtistBrowseSections(browseJson, artist.name, albums, songs, videos)
                    allSongsBrowseId = parsed.first.first
                    allSongsParams = parsed.first.second
                    allVideosBrowseId = parsed.second.first
                    allVideosParams = parsed.second.second
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback / supplementary search to ensure we have enough albums (>= 4), songs (>= 5), and videos (>= 5)
        if (albums.size < 4 || songs.size < 5 || videos.size < 5) {
            try {
                val searchResult = searchAll(artist.name)
                for (a in searchResult.albums) {
                    if (albums.none { it.title.equals(a.title, ignoreCase = true) }) {
                        albums.add(a)
                    }
                }
                for (s in searchResult.songs) {
                    if (s.isVideo) {
                        if (videos.none { it.id == s.id }) videos.add(s)
                    } else {
                        if (songs.none { it.id == s.id }) songs.add(s)
                    }
                }

                // If still need more songs, query specifically for songs
                if (songs.size < 5) {
                    val songSearch = searchAll("${artist.name} songs")
                    for (s in songSearch.songs) {
                        if (!s.isVideo && songs.none { it.id == s.id }) {
                            songs.add(s)
                        }
                    }
                }

                // If still need more videos, query specifically for videos
                if (videos.size < 5) {
                    val videoSearch = searchAll("${artist.name} videos")
                    for (v in videoSearch.songs) {
                        if (videos.none { it.id == v.id }) {
                            videos.add(v.copy(isVideo = true))
                        }
                    }
                }

                // If still need more albums, query specifically for albums
                if (albums.size < 4) {
                    val albumSearch = searchAll("${artist.name} albums")
                    for (a in albumSearch.albums) {
                        if (albums.none { it.title.equals(a.title, ignoreCase = true) }) {
                            albums.add(a)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        ArtistDetailData(
            albums = albums,
            songs = songs,
            videos = videos,
            allSongsBrowseId = allSongsBrowseId,
            allSongsParams = allSongsParams,
            allVideosBrowseId = allVideosBrowseId,
            allVideosParams = allVideosParams
        )
    }

    suspend fun fetchArtistAllTracks(
        browseId: String?,
        params: String?,
        fallbackQuery: String,
        artistName: String,
        isVideo: Boolean
    ): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()

        if (!browseId.isNullOrEmpty()) {
            try {
                val payload = JSONObject().apply {
                    put("context", createClientContext())
                    put("browseId", browseId)
                    if (!params.isNullOrEmpty()) {
                        put("params", params)
                    }
                }
                val jsonStr = executeInnerTubeRequest("https://music.youtube.com/youtubei/v1/browse", payload)
                if (jsonStr != null) {
                    val root = JSONObject(jsonStr)
                    fun scan(obj: Any?) {
                        when (obj) {
                            is JSONObject -> {
                                if (obj.has("musicResponsiveListItemRenderer")) {
                                    val item = obj.getJSONObject("musicResponsiveListItemRenderer")
                                    val track = parseTrackFromResponsiveItem(item, artistName, isVideo = isVideo)
                                    if (track != null && tracks.none { it.id == track.id }) {
                                        tracks.add(track)
                                    }
                                }
                                val keys = obj.keys()
                                while (keys.hasNext()) {
                                    scan(obj.get(keys.next()))
                                }
                            }
                            is JSONArray -> {
                                for (i in 0 until obj.length()) {
                                    scan(obj.get(i))
                                }
                            }
                        }
                    }
                    scan(root)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (tracks.isEmpty()) {
            try {
                val searchRes = searchAll(fallbackQuery)
                for (s in searchRes.songs) {
                    if (isVideo) {
                        if (tracks.none { it.id == s.id }) tracks.add(s.copy(isVideo = true))
                    } else {
                        if (!s.isVideo && tracks.none { it.id == s.id }) tracks.add(s)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        tracks
    }

    suspend fun getAlbumTracks(album: Album): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()

        if (!album.browseId.isNullOrEmpty()) {
            try {
                val browseJson = executeInnerTubeRequest(
                    "https://music.youtube.com/youtubei/v1/browse",
                    JSONObject().apply {
                        put("context", createClientContext())
                        put("browseId", album.browseId)
                    }
                )
                if (browseJson != null) {
                    parseAlbumTracks(browseJson, album, tracks)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (tracks.isEmpty()) {
            val searchRes = searchAll("${album.title} ${album.artist} album")
            tracks.addAll(searchRes.songs)
        }

        tracks
    }

    suspend fun fetchPlaylistFromYouTube(playlistInput: String): YouTubePlaylistData? = withContext(Dispatchers.IO) {
        val playlistId = extractPlaylistId(playlistInput) ?: playlistInput.trim()
        if (playlistId.isEmpty()) return@withContext null

        val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"

        var jsonString = executeInnerTubeRequest(
            "https://music.youtube.com/youtubei/v1/browse",
            JSONObject().apply {
                put("context", createClientContext())
                put("browseId", browseId)
            }
        )

        // Fallback without "VL" prefix if initial browse returned null
        if (jsonString == null && !playlistId.startsWith("VL")) {
            jsonString = executeInnerTubeRequest(
                "https://music.youtube.com/youtubei/v1/browse",
                JSONObject().apply {
                    put("context", createClientContext())
                    put("browseId", playlistId)
                }
            )
        }

        if (jsonString == null) return@withContext null

        parseYouTubePlaylistJson(playlistId, jsonString)
    }

    private fun parseYouTubePlaylistJson(playlistId: String, jsonString: String): YouTubePlaylistData {
        var title = "YouTube Playlist"
        var author: String? = null
        var thumbnailUrl: String? = null
        val tracks = mutableListOf<Track>()
        val seenTrackIds = mutableSetOf<String>()
        var nextContinuation: String? = null

        try {
            val root = JSONObject(jsonString)

            fun scanHeader(obj: JSONObject) {
                val headerObj = when {
                    obj.has("musicResponsiveHeaderRenderer") -> obj.getJSONObject("musicResponsiveHeaderRenderer")
                    obj.has("musicDetailHeaderRenderer") -> obj.getJSONObject("musicDetailHeaderRenderer")
                    obj.has("musicEditablePlaylistDetailHeaderRenderer") -> {
                        val editHeader = obj.getJSONObject("musicEditablePlaylistDetailHeaderRenderer")
                        editHeader.optJSONObject("header")?.optJSONObject("musicResponsiveHeaderRenderer") ?: editHeader
                    }
                    else -> null
                }

                if (headerObj != null) {
                    val titleRuns = headerObj.optJSONObject("title")?.optJSONArray("runs")
                    if (titleRuns != null && titleRuns.length() > 0) {
                        val t = titleRuns.getJSONObject(0).optString("text", "").trim()
                        if (t.isNotEmpty()) title = t
                    }

                    val straplineRuns = headerObj.optJSONObject("straplineTextOne")?.optJSONArray("runs")
                        ?: headerObj.optJSONObject("subtitle")?.optJSONArray("runs")
                    if (straplineRuns != null && straplineRuns.length() > 0) {
                        val a = straplineRuns.getJSONObject(0).optString("text", "").trim()
                        if (a.isNotEmpty() && !a.startsWith("Playlist") && !a.startsWith("Album")) {
                            author = a
                        }
                    }

                    val thumbs = headerObj.optJSONObject("thumbnail")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                    if (thumbs != null && thumbs.length() > 0) {
                        val rawUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                        if (rawUrl.isNotEmpty()) {
                            thumbnailUrl = upgradeThumbnailUrl(rawUrl)
                        }
                    }
                }
            }

            fun walk(obj: Any?) {
                when (obj) {
                    is JSONObject -> {
                        scanHeader(obj)

                        if (obj.has("musicResponsiveListItemRenderer")) {
                            val item = obj.getJSONObject("musicResponsiveListItemRenderer")
                            val track = parseTrackFromResponsiveItem(item, author ?: "YouTube", isVideo = false)
                            if (track != null && seenTrackIds.add(track.id)) {
                                tracks.add(track)
                            }
                        }

                        if (obj.has("nextContinuationData")) {
                            val cont = obj.getJSONObject("nextContinuationData").optString("continuation", "")
                            if (cont.isNotEmpty()) {
                                nextContinuation = cont
                            }
                        }

                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            walk(obj.get(keys.next()))
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until obj.length()) {
                            walk(obj.get(i))
                        }
                    }
                }
            }

            walk(root)

            // Handle continuations (up to 300 tracks max)
            var iterations = 0
            while (!nextContinuation.isNullOrEmpty() && tracks.size < 300 && iterations < 5) {
                iterations++
                val cToken = nextContinuation!!
                nextContinuation = null
                val contJson = executeInnerTubeRequest(
                    "https://music.youtube.com/youtubei/v1/browse?continuation=$cToken&ctoken=$cToken",
                    JSONObject().apply {
                        put("context", createClientContext())
                    }
                ) ?: break

                try {
                    val contRoot = JSONObject(contJson)
                    walk(contRoot)
                } catch (e: Exception) {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (thumbnailUrl.isNullOrEmpty() && tracks.isNotEmpty()) {
            thumbnailUrl = tracks.firstOrNull()?.thumbnailUrl
        }

        return YouTubePlaylistData(
            id = playlistId,
            title = title,
            author = author,
            thumbnailUrl = thumbnailUrl,
            tracks = tracks
        )
    }

    private fun parseCategorizedSearch(jsonString: String): SearchResult {
        val songs = mutableListOf<Track>()
        val artists = mutableListOf<Artist>()
        val albums = mutableListOf<Album>()

        val seenSongIds = mutableSetOf<String>()
        val seenArtistNames = mutableSetOf<String>()
        val seenAlbumTitles = mutableSetOf<String>()

        try {
            val root = JSONObject(jsonString)

            fun walk(obj: Any?) {
                when (obj) {
                    is JSONObject -> {
                        if (obj.has("musicResponsiveListItemRenderer")) {
                            val item = obj.getJSONObject("musicResponsiveListItemRenderer")
                            parseListItem(item, songs, artists, albums, seenSongIds, seenArtistNames, seenAlbumTitles)
                        } else if (obj.has("musicCardShelfRenderer")) {
                            val card = obj.getJSONObject("musicCardShelfRenderer")
                            parseCardShelf(card, artists, albums, seenArtistNames, seenAlbumTitles)
                        } else if (obj.has("musicTwoRowItemRenderer")) {
                            val item = obj.getJSONObject("musicTwoRowItemRenderer")
                            parseTwoRowItem(item, artists, albums, seenArtistNames, seenAlbumTitles)
                        }
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            walk(obj.get(keys.next()))
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until obj.length()) {
                            walk(obj.get(i))
                        }
                    }
                }
            }

            walk(root)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return SearchResult(songs = songs, artists = artists, albums = albums)
    }

    private fun parseListItem(
        item: JSONObject,
        songs: MutableList<Track>,
        artists: MutableList<Artist>,
        albums: MutableList<Album>,
        seenSongIds: MutableSet<String>,
        seenArtistNames: MutableSet<String>,
        seenAlbumTitles: MutableSet<String>
    ) {
        try {
            val flexCols = item.optJSONArray("flexColumns") ?: return
            if (flexCols.length() == 0) return

            var title = ""
            var subtitle = ""
            var videoId = ""
            var browseId: String? = null
            var pageType: String? = null

            // Column 0: Title and Navigation
            val col0 = flexCols.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            val runs0 = col0?.optJSONObject("text")?.optJSONArray("runs")
            if (runs0 != null && runs0.length() > 0) {
                val firstRun = runs0.getJSONObject(0)
                title = firstRun.optString("text", "")
                val nav = firstRun.optJSONObject("navigationEndpoint")
                if (nav != null) {
                    val browseEp = nav.optJSONObject("browseEndpoint")
                    if (browseEp != null) {
                        browseId = browseEp.optString("browseId", "").ifEmpty { null }
                        pageType = browseEp.optJSONObject("browseEndpointContextSupportedConfigs")
                            ?.optJSONObject("browseEndpointContextMusicConfig")
                            ?.optString("pageType", "")?.ifEmpty { null }
                    }
                    val watchEp = nav.optJSONObject("watchEndpoint")
                    if (watchEp != null) {
                        videoId = watchEp.optString("videoId", "")
                    }
                }
            }

            // Column 1: Subtitle (Artist / Album / Type info)
            if (flexCols.length() > 1) {
                val col1 = flexCols.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                val runs1 = col1?.optJSONObject("text")?.optJSONArray("runs")
                if (runs1 != null) {
                    val sb = StringBuilder()
                    for (i in 0 until runs1.length()) {
                        sb.append(runs1.getJSONObject(i).optString("text", ""))
                    }
                    subtitle = sb.toString().trim()
                }
            }

            // Item-level videoId
            if (videoId.isEmpty() && item.has("playlistItemData")) {
                videoId = item.getJSONObject("playlistItemData").optString("videoId", "")
            }

            // Item-level navigation
            if (item.has("navigationEndpoint")) {
                val nav = item.getJSONObject("navigationEndpoint")
                val browseEp = nav.optJSONObject("browseEndpoint")
                if (browseEp != null) {
                    if (browseId == null) browseId = browseEp.optString("browseId", "").ifEmpty { null }
                    if (pageType == null) {
                        pageType = browseEp.optJSONObject("browseEndpointContextSupportedConfigs")
                            ?.optJSONObject("browseEndpointContextMusicConfig")
                            ?.optString("pageType", "")?.ifEmpty { null }
                    }
                }
            }

            // Extract thumbnail
            var thumbUrl: String? = null
            val thumbs = item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            if (thumbs != null && thumbs.length() > 0) {
                thumbUrl = upgradeThumbnailUrl(thumbs.getJSONObject(thumbs.length() - 1).optString("url"))
            }

            if (title.isEmpty()) return

            val subLower = subtitle.lowercase()

            // Classify into Artist, Album, or Song
            if (pageType == "MUSIC_PAGE_TYPE_ARTIST" || subLower.startsWith("artist") || subLower.contains("• artist")) {
                val cleanName = title.trim()
                if (!seenArtistNames.contains(cleanName)) {
                    seenArtistNames.add(cleanName)
                    artists.add(
                        Artist(
                            id = browseId ?: "artist_$cleanName",
                            name = cleanName,
                            thumbnailUrl = thumbUrl,
                            subtitle = subtitle.ifEmpty { "Artist" },
                            browseId = browseId
                        )
                    )
                }
            } else if (pageType == "MUSIC_PAGE_TYPE_ALBUM" || subLower.startsWith("album") || subLower.contains("album •") || subLower.contains("ep •")) {
                val cleanTitle = title.trim()
                if (!seenAlbumTitles.contains(cleanTitle)) {
                    seenAlbumTitles.add(cleanTitle)
                    albums.add(
                        Album(
                            id = browseId ?: "album_$cleanTitle",
                            title = cleanTitle,
                            artist = extractArtistFromSubtitle(subtitle),
                            thumbnailUrl = thumbUrl,
                            year = extractYearFromSubtitle(subtitle),
                            browseId = browseId
                        )
                    )
                }
            } else if (videoId.isNotEmpty()) {
                if (!seenSongIds.contains(videoId)) {
                    seenSongIds.add(videoId)
                    val isVid = subLower.startsWith("video") || subLower.contains("video •") || subLower.contains("• video") || pageType == "MUSIC_PAGE_TYPE_MUSIC_VIDEO"
                    songs.add(
                        Track(
                            id = videoId,
                            title = title,
                            artist = subtitle.ifEmpty { "YouTube Music" },
                            durationMs = 0L,
                            thumbnailUrl = thumbUrl,
                            isLocal = false,
                            audioFormat = AudioFormat.YOUTUBE,
                            isVideo = isVid
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseArtistBrowseSections(
        jsonString: String,
        artistName: String,
        albums: MutableList<Album>,
        songs: MutableList<Track>,
        videos: MutableList<Track>
    ): Pair<Pair<String?, String?>, Pair<String?, String?>> {
        var songsBrowseId: String? = null
        var songsParams: String? = null
        var videosBrowseId: String? = null
        var videosParams: String? = null

        try {
            val root = JSONObject(jsonString)

            fun scan(obj: Any?) {
                when (obj) {
                    is JSONObject -> {
                        if (obj.has("musicShelfRenderer")) {
                            val shelf = obj.getJSONObject("musicShelfRenderer")
                            val title = shelf.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "")?.lowercase() ?: ""
                            val btn = shelf.optJSONObject("bottomEndpoint")?.optJSONObject("browseEndpoint")
                                ?: shelf.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")

                            if (title.contains("song")) {
                                if (btn != null) {
                                    songsBrowseId = btn.optString("browseId", "").ifEmpty { null }
                                    songsParams = btn.optString("params", "").ifEmpty { null }
                                }
                                val contents = shelf.optJSONArray("contents") ?: JSONArray()
                                for (i in 0 until contents.length()) {
                                    val item = contents.getJSONObject(i).optJSONObject("musicResponsiveListItemRenderer") ?: continue
                                    val track = parseTrackFromResponsiveItem(item, artistName, isVideo = false)
                                    if (track != null && songs.none { it.id == track.id }) {
                                        songs.add(track)
                                    }
                                }
                            }
                        }

                        if (obj.has("musicCarouselShelfRenderer")) {
                            val c = obj.getJSONObject("musicCarouselShelfRenderer")
                            val header = c.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                            val title = header?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "")?.lowercase() ?: ""
                            val btn = header?.optJSONObject("moreContentButton")?.optJSONObject("buttonRenderer")?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                                ?: header?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")

                            if (title.contains("video")) {
                                if (btn != null) {
                                    videosBrowseId = btn.optString("browseId", "").ifEmpty { null }
                                    videosParams = btn.optString("params", "").ifEmpty { null }
                                }
                                val contents = c.optJSONArray("contents") ?: JSONArray()
                                for (i in 0 until contents.length()) {
                                    val itemObj = contents.getJSONObject(i)
                                    val twoRow = itemObj.optJSONObject("musicTwoRowItemRenderer")
                                    val respItem = itemObj.optJSONObject("musicResponsiveListItemRenderer")
                                    if (twoRow != null) {
                                        val track = parseTrackFromTwoRowItem(twoRow, artistName, isVideo = true)
                                        if (track != null && videos.none { it.id == track.id }) {
                                            videos.add(track)
                                        }
                                    } else if (respItem != null) {
                                        val track = parseTrackFromResponsiveItem(respItem, artistName, isVideo = true)
                                        if (track != null && videos.none { it.id == track.id }) {
                                            videos.add(track)
                                        }
                                    }
                                }
                            } else if (title.contains("live") || title.contains("performance")) {
                                val contents = c.optJSONArray("contents") ?: JSONArray()
                                for (i in 0 until contents.length()) {
                                    val itemObj = contents.getJSONObject(i)
                                    val twoRow = itemObj.optJSONObject("musicTwoRowItemRenderer")
                                    if (twoRow != null) {
                                        val track = parseTrackFromTwoRowItem(twoRow, artistName, isVideo = true)
                                        if (track != null && videos.none { it.id == track.id }) {
                                            videos.add(track)
                                        }
                                    }
                                }
                            } else if (title.contains("album") || title.contains("single") || title.contains("ep")) {
                                val contents = c.optJSONArray("contents") ?: JSONArray()
                                for (i in 0 until contents.length()) {
                                    val itemObj = contents.getJSONObject(i)
                                    val twoRow = itemObj.optJSONObject("musicTwoRowItemRenderer") ?: continue
                                    val albumTitle = twoRow.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                                    val sub = twoRow.optJSONObject("subtitle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                                    val nav = twoRow.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                                    val bId = nav?.optString("browseId")
                                    val thumbs = twoRow.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                                        ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                    val thumb = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url")

                                    if (albumTitle.isNotEmpty() && albums.none { it.title.equals(albumTitle, ignoreCase = true) }) {
                                        albums.add(
                                            Album(
                                                id = bId ?: "album_$albumTitle",
                                                title = albumTitle,
                                                artist = artistName,
                                                thumbnailUrl = upgradeThumbnailUrl(thumb),
                                                year = sub,
                                                browseId = bId
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            scan(obj.get(keys.next()))
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until obj.length()) {
                            scan(obj.get(i))
                        }
                    }
                }
            }

            scan(root)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(Pair(songsBrowseId, songsParams), Pair(videosBrowseId, videosParams))
    }

    private fun parseTrackFromResponsiveItem(
        item: JSONObject,
        defaultArtist: String,
        isVideo: Boolean
    ): Track? {
        try {
            val flexCols = item.optJSONArray("flexColumns") ?: return null
            if (flexCols.length() == 0) return null

            val col0 = flexCols.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            val run0 = col0?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)
            val title = run0?.optString("text", "") ?: return null
            var vId = run0.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId", "") ?: ""
            if (vId.isEmpty() && item.has("playlistItemData")) {
                vId = item.getJSONObject("playlistItemData").optString("videoId", "")
            }
            if (title.isEmpty() || vId.isEmpty()) return null

            var artist = defaultArtist
            var durationMs = 0L

            if (flexCols.length() > 1) {
                val col1 = flexCols.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                val runs1 = col1?.optJSONObject("text")?.optJSONArray("runs")
                if (runs1 != null && runs1.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until runs1.length()) {
                        val text = runs1.getJSONObject(i).optString("text", "")
                        sb.append(text)
                        if (text.contains(":") && durationMs == 0L) {
                            durationMs = parseDurationMs(text)
                        }
                    }
                    val sText = sb.toString().trim()
                    if (sText.isNotEmpty() && !sText.startsWith("Song") && !sText.startsWith("Video")) {
                        artist = sText
                    }
                }
            }

            val thumbs = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            val thumb = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url")

            return Track(
                id = vId,
                title = title,
                artist = artist,
                durationMs = durationMs,
                thumbnailUrl = upgradeThumbnailUrl(thumb),
                isLocal = false,
                audioFormat = AudioFormat.YOUTUBE,
                isVideo = isVideo
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseTrackFromTwoRowItem(
        item: JSONObject,
        defaultArtist: String,
        isVideo: Boolean
    ): Track? {
        try {
            val title = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: return null
            val subRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
            var subtitle = ""
            if (subRuns != null) {
                val sb = StringBuilder()
                for (i in 0 until subRuns.length()) {
                    sb.append(subRuns.getJSONObject(i).optString("text", ""))
                }
                subtitle = sb.toString().trim()
            }
            val nav = item.optJSONObject("navigationEndpoint")
            val vId = nav?.optJSONObject("watchEndpoint")?.optString("videoId", "") ?: ""
            if (title.isEmpty() || vId.isEmpty()) return null

            val thumbs = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            val thumb = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url")

            return Track(
                id = vId,
                title = title,
                artist = if (subtitle.isNotEmpty()) subtitle else defaultArtist,
                durationMs = 0L,
                thumbnailUrl = upgradeThumbnailUrl(thumb),
                isLocal = false,
                audioFormat = AudioFormat.YOUTUBE,
                isVideo = isVideo
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseDurationMs(text: String): Long {
        val clean = text.trim()
        val parts = clean.split(":")
        return when (parts.size) {
            2 -> {
                val m = parts[0].toLongOrNull() ?: return 0L
                val s = parts[1].toLongOrNull() ?: return 0L
                (m * 60 + s) * 1000L
            }
            3 -> {
                val h = parts[0].toLongOrNull() ?: return 0L
                val m = parts[1].toLongOrNull() ?: return 0L
                val s = parts[2].toLongOrNull() ?: return 0L
                (h * 3600 + m * 60 + s) * 1000L
            }
            else -> 0L
        }
    }

    private fun parseAlbumTracks(jsonString: String, album: Album, tracks: MutableList<Track>) {
        try {
            val root = JSONObject(jsonString)

            fun walk(obj: Any?) {
                when (obj) {
                    is JSONObject -> {
                        if (obj.has("musicResponsiveListItemRenderer")) {
                            val item = obj.getJSONObject("musicResponsiveListItemRenderer")
                            val flexCols = item.optJSONArray("flexColumns")
                            if (flexCols != null && flexCols.length() > 0) {
                                val col0 = flexCols.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                val run0 = col0?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)
                                val title = run0?.optString("text", "") ?: ""
                                var vId = run0?.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId", "") ?: ""
                                if (vId.isEmpty() && item.has("playlistItemData")) {
                                    vId = item.getJSONObject("playlistItemData").optString("videoId", "")
                                }
                                if (title.isNotEmpty() && vId.isNotEmpty()) {
                                    tracks.add(
                                        Track(
                                            id = vId,
                                            title = title,
                                            artist = album.artist,
                                            durationMs = 0L,
                                            thumbnailUrl = album.thumbnailUrl,
                                            isLocal = false,
                                            audioFormat = AudioFormat.YOUTUBE
                                        )
                                    )
                                }
                            }
                        }
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            walk(obj.get(keys.next()))
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until obj.length()) {
                            walk(obj.get(i))
                        }
                    }
                }
            }

            walk(root)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun executeInnerTubeRequest(urlStr: String, payload: JSONObject): String? {
        val url = URL(urlStr)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            setRequestProperty("Origin", "https://music.youtube.com")
        }

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        return if (connection.responseCode == 200) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } else {
            null
        }
    }

    private fun createClientContext(): JSONObject {
        return JSONObject().apply {
            put("client", JSONObject().apply {
                put("clientName", "WEB_REMIX")
                put("clientVersion", "1.20240101.01.00")
                put("hl", "en")
                put("gl", "US")
            })
        }
    }

    private fun parseCardShelf(
        card: JSONObject,
        artists: MutableList<Artist>,
        albums: MutableList<Album>,
        seenArtistNames: MutableSet<String>,
        seenAlbumTitles: MutableSet<String>
    ) {
        try {
            val titleRuns = card.optJSONObject("title")?.optJSONArray("runs") ?: return
            if (titleRuns.length() == 0) return
            val title = titleRuns.getJSONObject(0).optString("text", "").trim()
            if (title.isEmpty()) return

            val subRuns = card.optJSONObject("subtitle")?.optJSONArray("runs")
            val subtitleSb = StringBuilder()
            if (subRuns != null) {
                for (i in 0 until subRuns.length()) {
                    subtitleSb.append(subRuns.getJSONObject(i).optString("text", ""))
                }
            }
            val subtitle = subtitleSb.toString().trim()

            var thumbUrl: String? = null
            val thumbs = card.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            if (thumbs != null && thumbs.length() > 0) {
                thumbUrl = upgradeThumbnailUrl(thumbs.getJSONObject(thumbs.length() - 1).optString("url"))
            }

            var browseId: String? = null
            var pageType: String? = null
            val nav = titleRuns.getJSONObject(0).optJSONObject("navigationEndpoint")
            if (nav != null) {
                val browseEp = nav.optJSONObject("browseEndpoint")
                if (browseEp != null) {
                    browseId = browseEp.optString("browseId", "").ifEmpty { null }
                    pageType = browseEp.optJSONObject("browseEndpointContextSupportedConfigs")
                        ?.optJSONObject("browseEndpointContextMusicConfig")
                        ?.optString("pageType", "")?.ifEmpty { null }
                }
            }

            val subLower = subtitle.lowercase()
            if (pageType == "MUSIC_PAGE_TYPE_ARTIST" || subLower.contains("artist") || browseId?.startsWith("UC") == true) {
                if (!seenArtistNames.contains(title)) {
                    seenArtistNames.add(title)
                    artists.add(0,
                        Artist(
                            id = browseId ?: "artist_$title",
                            name = title,
                            thumbnailUrl = thumbUrl,
                            subtitle = subtitle.ifEmpty { "Artist" },
                            browseId = browseId
                        )
                    )
                }
            } else if (pageType == "MUSIC_PAGE_TYPE_ALBUM" || subLower.contains("album") || subLower.contains("ep")) {
                if (!seenAlbumTitles.contains(title)) {
                    seenAlbumTitles.add(title)
                    albums.add(0,
                        Album(
                            id = browseId ?: "album_$title",
                            title = title,
                            artist = extractArtistFromSubtitle(subtitle),
                            thumbnailUrl = thumbUrl,
                            browseId = browseId,
                            year = extractYearFromSubtitle(subtitle)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseTwoRowItem(
        item: JSONObject,
        artists: MutableList<Artist>,
        albums: MutableList<Album>,
        seenArtistNames: MutableSet<String>,
        seenAlbumTitles: MutableSet<String>
    ) {
        try {
            val titleRuns = item.optJSONObject("title")?.optJSONArray("runs") ?: return
            if (titleRuns.length() == 0) return
            val title = titleRuns.getJSONObject(0).optString("text", "").trim()
            if (title.isEmpty()) return

            val subRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
            val subtitleSb = StringBuilder()
            if (subRuns != null) {
                for (i in 0 until subRuns.length()) {
                    subtitleSb.append(subRuns.getJSONObject(i).optString("text", ""))
                }
            }
            val subtitle = subtitleSb.toString().trim()

            var thumbUrl: String? = null
            val thumbs = item.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            if (thumbs != null && thumbs.length() > 0) {
                thumbUrl = upgradeThumbnailUrl(thumbs.getJSONObject(thumbs.length() - 1).optString("url"))
            }

            var browseId: String? = null
            var pageType: String? = null
            val nav = item.optJSONObject("navigationEndpoint")
            if (nav != null) {
                val browseEp = nav.optJSONObject("browseEndpoint")
                if (browseEp != null) {
                    browseId = browseEp.optString("browseId", "").ifEmpty { null }
                    pageType = browseEp.optJSONObject("browseEndpointContextSupportedConfigs")
                        ?.optJSONObject("browseEndpointContextMusicConfig")
                        ?.optString("pageType", "")?.ifEmpty { null }
                }
            }

            val subLower = subtitle.lowercase()
            if (pageType == "MUSIC_PAGE_TYPE_ARTIST" || subLower.contains("artist") || browseId?.startsWith("UC") == true) {
                if (!seenArtistNames.contains(title)) {
                    seenArtistNames.add(title)
                    artists.add(
                        Artist(
                            id = browseId ?: "artist_$title",
                            name = title,
                            thumbnailUrl = thumbUrl,
                            subtitle = subtitle.ifEmpty { "Artist" },
                            browseId = browseId
                        )
                    )
                }
            } else if (pageType == "MUSIC_PAGE_TYPE_ALBUM" || subLower.contains("album") || subLower.contains("ep")) {
                if (!seenAlbumTitles.contains(title)) {
                    seenAlbumTitles.add(title)
                    albums.add(
                        Album(
                            id = browseId ?: "album_$title",
                            title = title,
                            artist = extractArtistFromSubtitle(subtitle),
                            thumbnailUrl = thumbUrl,
                            browseId = browseId,
                            year = extractYearFromSubtitle(subtitle)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractArtistFromSubtitle(subtitle: String): String {
        val parts = subtitle.split("•")
        return if (parts.size > 1) parts[1].trim() else "Artist"
    }

    private fun extractYearFromSubtitle(subtitle: String): String? {
        val match = Regex("""\b(19\d\d|20\d\d)\b""").find(subtitle)
        return match?.value
    }

    suspend fun fetchArtistInfo(artistName: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val res = searchAll(artistName)
            val match = res.artists.firstOrNull { it.name.equals(artistName, ignoreCase = true) }
                ?: res.artists.firstOrNull()
            if (match != null) {
                Pair(upgradeThumbnailUrl(match.thumbnailUrl), match.browseId)
            } else {
                val thumb = res.albums.firstOrNull()?.thumbnailUrl ?: res.songs.firstOrNull()?.thumbnailUrl
                Pair(upgradeThumbnailUrl(thumb), null)
            }
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    suspend fun fetchArtistThumbnail(artistName: String): String? = fetchArtistInfo(artistName).first

    companion object {
        fun extractPlaylistId(input: String): String? {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return null

            // 1. Matches ?list=..., &list=...
            val listParamRegex = Regex("[?&]list=([a-zA-Z0-9_-]+)")
            val listMatch = listParamRegex.find(trimmed)
            if (listMatch != null) {
                return listMatch.groupValues[1]
            }

            // 2. Matches /browse/VL... or /browse/PL...
            val browseRegex = Regex("/browse/([a-zA-Z0-9_-]+)")
            val browseMatch = browseRegex.find(trimmed)
            if (browseMatch != null) {
                return browseMatch.groupValues[1]
            }

            // 3. Raw playlist IDs like PL..., VL..., OLAK..., RDCLAK...
            if (trimmed.matches(Regex("^(PL|VL|OLAK|RDCLAK)[a-zA-Z0-9_-]+$"))) {
                return trimmed
            }

            // 4. Any alphanumeric ID with length >= 12
            if (trimmed.length >= 12 && trimmed.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                return trimmed
            }

            return null
        }

        fun upgradeThumbnailUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            var upgraded = url
            if (upgraded.contains("googleusercontent.com") || upgraded.contains("ggpht.com")) {
                // Upgrade any =w...-h... or =s... to high resolution 800x800
                upgraded = upgraded.replace(Regex("=w\\d+-h\\d+[^\"\\s]*"), "=w800-h800-p-l90-rj")
                upgraded = upgraded.replace(Regex("=s\\d+[^\"\\s]*"), "=s800-c")
                if (!upgraded.contains("=w") && !upgraded.contains("=s")) {
                    upgraded = "$upgraded=w800-h800-p-l90-rj"
                }
            } else if (upgraded.contains("i.ytimg.com") || upgraded.contains("img.youtube.com")) {
                if (upgraded.contains("default.jpg") && !upgraded.contains("maxresdefault.jpg") && !upgraded.contains("hq720.jpg")) {
                    upgraded = upgraded.replace("default.jpg", "hqdefault.jpg")
                }
            }
            return upgraded
        }
        val DEFAULT_INITIAL_ARTISTS = listOf(
            Artist(
                id = "polyphia",
                name = "Polyphia",
                subtitle = "Progressive Instrumental Rock",
                thumbnailUrl = "https://yt3.googleusercontent.com/A-gcfY6ugVzt0jFkHKsV2TkGVnLInxAMEMh3_jyb7oXoZwbbK9xy1EL0emXdNeEPnbc9kpIAgqVq10I=w544-h544-p-l90-rj",
                browseId = "UCZhtK40axOw7MGLTfwQ1yVA"
            ),
            Artist(
                id = "unprocessed",
                name = "Unprocessed",
                subtitle = "Modern Progressive Metal",
                thumbnailUrl = "https://lh3.googleusercontent.com/djPOSMCgU-Qnx1VJnVHcfy9BqYNfhgAtzIVBEoeGIoch8xm0MI5lYbh6Zr9UBS3-nWugnTz-r4o4xYo=w544-h544-p-l90-rj",
                browseId = "UCeVxlm-6OLIxVCI4IUwKbDQ"
            ),
            Artist(
                id = "animals_as_leaders",
                name = "Animals as Leaders",
                subtitle = "Instrumental Djent / Jazz Fusion",
                thumbnailUrl = "https://lh3.googleusercontent.com/pi60YOpqEzVtsnOqjAeur_OUhfJ__ZNrxHOcaMiAarFew29kawbRuWVFotZ-1OqOEmOv2l1HmnRq8g=w544-h544-p-l90-rj",
                browseId = "UCh8E95ps6PJjgdS5XKzDEfg"
            ),
            Artist(
                id = "syncatto",
                name = "Syncatto",
                subtitle = "Flamenco Fusion / Prog Rock",
                thumbnailUrl = "https://yt3.googleusercontent.com/gE_48RfhPtSxPtci1E-Ln96T0BwZUCx-cTeZ7ChPMXnOdvpRs9Pj_ogw2JzUce35eTx0gAgw9q_CS1Q6=w544-h544-l90-rj",
                browseId = "UCs6hz1aQNlSBwAkxEGhx2-g"
            ),
            Artist(
                id = "sleep_token",
                name = "Sleep Token",
                subtitle = "Alternative Metal / Post-Rock",
                thumbnailUrl = "https://yt3.googleusercontent.com/xgIQtqQHmx9_KgfZW9Px0JFJsRAbV9ZO0r424yxAUgqyH1RTlD_xPfLZE-BZGjcrfaytG8vteRU=w544-h544-l90-rj",
                browseId = "UCjS9WiKfJgltFPDecfIHtDg"
            ),
            Artist(
                id = "bad_omens",
                name = "Bad Omens",
                subtitle = "Metalcore / Alternative Rock",
                thumbnailUrl = "https://lh3.googleusercontent.com/ls0eOZYTrMDaS44I6JU1Axd2iC8qiPj0YUw7T6IFnXrY_sAL8LuwAvnC_TDnwG_qP19dVjUe8ns2YA=w544-h544-p-l90-rj",
                browseId = "UCSvdCVniqK4gto1-Kg04Dmg"
            ),
            Artist(
                id = "guns_and_roses",
                name = "Guns and Roses",
                subtitle = "Hard Rock Classics",
                thumbnailUrl = "https://lh3.googleusercontent.com/vLoywVkkAeb-JQsJsURx1HCP4ihEnFtzI8RXxCkc5r3_q2Z-demzHSjzrOFB9T5_46Tm6kxXPfbJlAo=w544-h544-p-l90-rj",
                browseId = "UCSLbbBoUqpin6BE34whSOvA"
            ),
            Artist(
                id = "ac_dc",
                name = "AC/DC",
                subtitle = "Hard Rock / Heavy Metal",
                thumbnailUrl = "https://lh3.googleusercontent.com/IlNp_o9GKakp7qtaDAKaDxpW29qtP8sjqQXPcBQ9uAdOUU3AnJdB85xLRiYIGT3FlCmobm6oiMQX4GU=w544-h544-p-l90-rj",
                browseId = "UCVm4YdI3hobkwsHTTOMVJKg"
            ),
            Artist(
                id = "intervals",
                name = "Intervals",
                subtitle = "Progressive Instrumental Rock",
                thumbnailUrl = "https://lh3.googleusercontent.com/jvPd_D41-AQJYCf0FjtImAtU2sqq5l7Z2apiwjvqo84IuWE23fS6dDDyxhar9XEfpJuk352BfIIhVyM=w544-h544-p-l90-rj",
                browseId = "UCnMq5nACWDzdJAf1cn_dmfQ"
            ),
            Artist(
                id = "plini",
                name = "Plini",
                subtitle = "Progressive Rock / Ambient Fusion",
                thumbnailUrl = "https://yt3.googleusercontent.com/ALsRfwBVjF6GH0jAxOdft40I394oFy51b15BMVm2Usq-_5AG3e8eIEFothOg1z9v--SFQejU=w544-h544-l90-rj",
                browseId = "UCHHr_xQWzwLBYDQS-i_UoLQ"
            ),
            Artist(
                id = "chon",
                name = "CHON",
                subtitle = "Math Rock / Progressive Instrumental",
                thumbnailUrl = "https://yt3.ggpht.com/ytc/AIdro_ly3O9l85XHBrTj8qqkxDFDvcPVucKmcKs8bhjxbtM=w544-h544-l90-rj",
                browseId = "UC1KEavl3odS1A1BRh7tqgQg"
            ),
            Artist(
                id = "ichika_nito",
                name = "Ichika Nito",
                subtitle = "Fingerstyle & Prog Guitar",
                thumbnailUrl = "https://lh3.googleusercontent.com/694ULRnr7zFTgqkvbQ-1iIfZmEoJgstnC_B9GR3ZiaLnsJuvxbkasucQi1Iy2RX_GuMnbrGCOg4vkk4=w544-h544-p-l90-rj",
                browseId = "UCdE7j7GnH57x20N2A3otvgw"
            ),
            Artist(
                id = "linkin_park",
                name = "Linkin Park",
                subtitle = "Nu-Metal / Alternative Rock",
                thumbnailUrl = "https://lh3.googleusercontent.com/uE72emEZFH3TVtCZoIFYKmnf7vsb42RYQxb4X-lonyqPQuS_mLtKpLfBQ5JdHwUijfQo06BtSB7LoQ=w544-h544-p-l90-rj",
                browseId = "UCxgN32UVVztKAQd2HkXzBtw"
            )
        )
    }
}
