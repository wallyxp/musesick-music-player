package com.nothing.music.repository

import com.nothing.music.model.Album
import com.nothing.music.model.Artist
import com.nothing.music.model.AudioFormat
import com.nothing.music.model.SearchResult
import com.nothing.music.model.Track
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

    suspend fun getArtistDetails(artist: Artist): Pair<List<Album>, List<Track>> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()
        val songs = mutableListOf<Track>()

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
                    parseArtistBrowse(browseJson, artist.name, albums, songs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback / supplementary search to ensure we have enough albums (>= 4) and songs (>= 15)
        if (albums.size < 4 || songs.size < 15) {
            try {
                val searchResult = searchAll(artist.name)
                for (a in searchResult.albums) {
                    if (albums.none { it.title.equals(a.title, ignoreCase = true) }) {
                        albums.add(a)
                    }
                }
                for (s in searchResult.songs) {
                    if (songs.none { it.id == s.id }) {
                        songs.add(s)
                    }
                }

                // If still need more songs, query specifically for songs
                if (songs.size < 15) {
                    val songSearch = searchAll("${artist.name} songs")
                    for (s in songSearch.songs) {
                        if (songs.none { it.id == s.id }) {
                            songs.add(s)
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

        Pair(albums, songs)
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
                thumbUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url")
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
                    songs.add(
                        Track(
                            id = videoId,
                            title = title,
                            artist = subtitle.ifEmpty { "YouTube Music" },
                            durationMs = 0L,
                            thumbnailUrl = thumbUrl,
                            isLocal = false,
                            audioFormat = AudioFormat.YOUTUBE
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseArtistBrowse(
        jsonString: String,
        artistName: String,
        albums: MutableList<Album>,
        songs: MutableList<Track>
    ) {
        try {
            val root = JSONObject(jsonString)

            fun walk(obj: Any?) {
                when (obj) {
                    is JSONObject -> {
                        // TwoRowItemRenderer (often used for albums & singles in artist page)
                        if (obj.has("musicTwoRowItemRenderer")) {
                            val item = obj.getJSONObject("musicTwoRowItemRenderer")
                            val title = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                            val sub = item.optJSONObject("subtitle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                            val nav = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                            val bId = nav?.optString("browseId")
                            val thumbs = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val thumb = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url")

                            if (title.isNotEmpty()) {
                                albums.add(
                                    Album(
                                        id = bId ?: "album_$title",
                                        title = title,
                                        artist = artistName,
                                        thumbnailUrl = thumb,
                                        year = sub,
                                        browseId = bId
                                    )
                                )
                            }
                        }

                        // ResponsiveListItemRenderer (top songs)
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
                                val thumbs = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                                    ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                val thumb = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url")

                                if (title.isNotEmpty() && vId.isNotEmpty()) {
                                    songs.add(
                                        Track(
                                            id = vId,
                                            title = title,
                                            artist = artistName,
                                            durationMs = 0L,
                                            thumbnailUrl = thumb,
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
                thumbUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url")
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
                thumbUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url")
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
                Pair(match.thumbnailUrl, match.browseId)
            } else {
                val thumb = res.albums.firstOrNull()?.thumbnailUrl ?: res.songs.firstOrNull()?.thumbnailUrl
                Pair(thumb, null)
            }
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    suspend fun fetchArtistThumbnail(artistName: String): String? = fetchArtistInfo(artistName).first

    companion object {
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
