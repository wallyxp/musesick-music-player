import Foundation

public struct SearchResult {
    public var songs: [Track] = []
    public var artists: [Artist] = []
    public var albums: [Album] = []
    public var playlists: [Playlist] = []

    public init(
        songs: [Track] = [],
        artists: [Artist] = [],
        albums: [Album] = [],
        playlists: [Playlist] = []
    ) {
        self.songs = songs
        self.artists = artists
        self.albums = albums
        self.playlists = playlists
    }
}

public actor YouTubeService {
    public static let shared = YouTubeService()

    private let userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    private func createClientContext() -> [String: Any] {
        return [
            "client": [
                "clientName": "WEB_REMIX",
                "clientVersion": "1.20240901.01.00",
                "hl": "en",
                "gl": "US"
            ]
        ]
    }

    public func searchAll(query: String) async -> SearchResult {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return SearchResult()
        }

        let url = URL(string: "https://music.youtube.com/youtubei/v1/search")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("https://music.youtube.com", forHTTPHeaderField: "Origin")
        request.setValue("https://music.youtube.com", forHTTPHeaderField: "Referer")

        let body: [String: Any] = [
            "context": createClientContext(),
            "query": query
        ]

        guard let httpBody = try? JSONSerialization.data(withJSONObject: body) else {
            return SearchResult()
        }
        request.httpBody = httpBody

        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
            return SearchResult()
        }

        return parseSearchJson(json)
    }

    public func searchTracks(query: String) async -> [Track] {
        await searchAll(query: query).songs
    }

    public func searchArtists(query: String) async -> [Artist] {
        await searchAll(query: query).artists
    }

    public func searchAlbums(query: String) async -> [Album] {
        await searchAll(query: query).albums
    }

    public func getAlbumTracks(browseId: String) async -> [Track] {
        let url = URL(string: "https://music.youtube.com/youtubei/v1/browse")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("https://music.youtube.com", forHTTPHeaderField: "Origin")

        let body: [String: Any] = [
            "context": createClientContext(),
            "browseId": browseId
        ]

        guard let httpBody = try? JSONSerialization.data(withJSONObject: body) else { return [] }
        request.httpBody = httpBody

        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
            return []
        }

        var tracks: [Track] = []
        parseAlbumTracksFromBrowse(json, into: &tracks)
        return tracks
    }

    public func getArtistDetails(artist: Artist) async -> ArtistDetailData {
        var albums: [Album] = []
        var songs: [Track] = []
        var heroImageUrl: String? = nil

        if let browseId = artist.browseId, !browseId.isEmpty {
            let url = URL(string: "https://music.youtube.com/youtubei/v1/browse")!
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
            request.setValue("https://music.youtube.com", forHTTPHeaderField: "Origin")

            let body: [String: Any] = [
                "context": createClientContext(),
                "browseId": browseId
            ]

            if let httpBody = try? JSONSerialization.data(withJSONObject: body) {
                request.httpBody = httpBody
                if let (data, response) = try? await URLSession.shared.data(for: request),
                   (response as? HTTPURLResponse)?.statusCode == 200,
                   let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] {
                    parseArtistBrowse(json, artistName: artist.name, albums: &albums, songs: &songs)

                    // Extract hero/header image
                    if let header = json["header"] as? [String: Any] {
                        let headerRenderer = (header["musicImmersiveHeaderRenderer"] as? [String: Any]) ?? (header["musicVisualHeaderRenderer"] as? [String: Any])
                        if let hr = headerRenderer {
                            let thumbDict = (hr["thumbnail"] as? [String: Any]) ?? (hr["foregroundThumbnail"] as? [String: Any])
                            if let mt = thumbDict?["musicThumbnailRenderer"] as? [String: Any],
                               let tObj = mt["thumbnail"] as? [String: Any],
                               let thumbs = tObj["thumbnails"] as? [[String: Any]],
                               let best = thumbs.last {
                                heroImageUrl = best["url"] as? String
                            }
                        }
                    }
                }
            }
        }

        // Fallback hero image to artist high-res image
        if heroImageUrl == nil {
            heroImageUrl = artist.highResImageUrl ?? artist.thumbnailUrl
        }

        // Fallback search if albums or songs are sparse
        if albums.count < 3 || songs.count < 3 {
            let searchRes = await searchAll(query: artist.name)
            for s in searchRes.songs where !songs.contains(where: { $0.id == s.id }) {
                songs.append(s)
            }
            for a in searchRes.albums where !albums.contains(where: { $0.id == a.id }) {
                albums.append(a)
            }
            if heroImageUrl == nil, let found = searchRes.artists.first(where: { $0.name.lowercased() == artist.name.lowercased() }) {
                heroImageUrl = found.highResImageUrl ?? found.thumbnailUrl
            }
        }

        return ArtistDetailData(albums: albums, songs: songs, heroImageUrl: heroImageUrl)
    }

    // MARK: - JSON Parsing Helpers

    private func parseSearchJson(_ root: [String: Any]) -> SearchResult {
        var result = SearchResult()

        func scan(_ obj: Any) {
            if let dict = obj as? [String: Any] {
                if let card = dict["musicCardShelfRenderer"] as? [String: Any] {
                    parseCardShelf(card, into: &result)
                }
                if let renderer = dict["musicResponsiveListItemRenderer"] as? [String: Any] {
                    parseResponsiveItem(renderer, into: &result)
                }
                for (_, value) in dict {
                    scan(value)
                }
            } else if let array = obj as? [Any] {
                for item in array {
                    scan(item)
                }
            }
        }

        scan(root)
        return result
    }

    private func parseCardShelf(_ card: [String: Any], into result: inout SearchResult) {
        var title = ""
        var browseId: String? = nil
        if let titleObj = card["title"] as? [String: Any],
           let runs = titleObj["runs"] as? [[String: Any]],
           let first = runs.first {
            title = first["text"] as? String ?? ""
            if let nav = first["navigationEndpoint"] as? [String: Any],
               let bEndpoint = nav["browseEndpoint"] as? [String: Any] {
                browseId = bEndpoint["browseId"] as? String
            }
        }

        var thumbUrl: String? = nil
        if let thumbDict = card["thumbnail"] as? [String: Any],
           let mt = thumbDict["musicThumbnailRenderer"] as? [String: Any],
           let tObj = mt["thumbnail"] as? [String: Any],
           let thumbs = tObj["thumbnails"] as? [[String: Any]],
           let best = thumbs.last {
            thumbUrl = best["url"] as? String
        }

        if !title.isEmpty, let bId = browseId {
            let artist = Artist(
                id: bId,
                name: title,
                thumbnailUrl: thumbUrl,
                fullImageUrl: thumbUrl?.replacingOccurrences(of: "=w120-h120", with: "=w1024-h1024"),
                subtitle: "Artist",
                browseId: bId
            )
            if !result.artists.contains(where: { $0.id == artist.id }) {
                result.artists.insert(artist, at: 0)
            }
        }
    }

    public func fetchArtistImage(artistName: String) async -> (thumbnailUrl: String?, fullImageUrl: String?, browseId: String?) {
        let res = await searchAll(query: artistName)
        if let match = res.artists.first(where: { $0.name.lowercased() == artistName.lowercased() }) ?? res.artists.first {
            return (match.thumbnailUrl, match.highResImageUrl, match.browseId)
        }
        return (nil, nil, nil)
    }

    private func parseResponsiveItem(_ item: [String: Any], into result: inout SearchResult) {
        let flexColumns = item["flexColumns"] as? [[String: Any]] ?? []
        guard !flexColumns.isEmpty else { return }

        // Title
        var title = ""
        var videoId: String? = nil
        var browseId: String? = nil

        if let firstCol = flexColumns.first,
           let textRuns = (firstCol["musicResponsiveListItemFlexColumnRenderer"] as? [String: Any])?["text"] as? [String: Any],
           let runs = textRuns["runs"] as? [[String: Any]],
           let firstRun = runs.first {
            title = firstRun["text"] as? String ?? ""
            if let navEndpoint = firstRun["navigationEndpoint"] as? [String: Any] {
                if let watchEndpoint = navEndpoint["watchEndpoint"] as? [String: Any] {
                    videoId = watchEndpoint["videoId"] as? String
                }
                if let bEndpoint = navEndpoint["browseEndpoint"] as? [String: Any] {
                    browseId = bEndpoint["browseId"] as? String
                }
            }
        }

        if browseId == nil {
            if let navEndpoint = item["navigationEndpoint"] as? [String: Any],
               let bEndpoint = navEndpoint["browseEndpoint"] as? [String: Any] {
                browseId = bEndpoint["browseId"] as? String
            }
        }

        // Artist, Duration, Subtitle
        var artistName = "Unknown Artist"
        var duration = "0:00"
        var isArtistItem = false
        var isAlbumItem = false

        if flexColumns.count > 1,
           let secondCol = flexColumns.dropFirst().first,
           let textRuns = (secondCol["musicResponsiveListItemFlexColumnRenderer"] as? [String: Any])?["text"] as? [String: Any],
           let runs = textRuns["runs"] as? [[String: Any]] {
            var parts: [String] = []
            for run in runs {
                if let t = run["text"] as? String, t != " • " && t != "•" {
                    parts.append(t)
                }
            }

            if parts.contains(where: { $0.lowercased() == "artist" }) {
                isArtistItem = true
            } else if parts.contains(where: { $0.lowercased() == "album" || $0.lowercased() == "ep" || $0.lowercased() == "single" }) {
                isAlbumItem = true
            }

            if let firstPart = parts.first {
                artistName = firstPart
            }
            if let lastPart = parts.last, lastPart.contains(":") {
                duration = lastPart
            }
        }

        // Thumbnail
        var thumbUrl: String? = nil
        if let thumbDict = item["thumbnail"] as? [String: Any],
           let musicThumbRenderer = thumbDict["musicThumbnailRenderer"] as? [String: Any],
           let thumbObj = musicThumbRenderer["thumbnail"] as? [String: Any],
           let thumbnails = thumbObj["thumbnails"] as? [[String: Any]],
           let best = thumbnails.last {
            thumbUrl = best["url"] as? String
        }

        if isArtistItem, let bId = browseId {
            let artist = Artist(
                id: bId,
                name: title,
                thumbnailUrl: thumbUrl,
                fullImageUrl: thumbUrl?.replacingOccurrences(of: "=w120-h120", with: "=w1024-h1024"),
                subtitle: "Artist",
                browseId: bId
            )
            if !result.artists.contains(where: { $0.id == artist.id }) {
                result.artists.append(artist)
            }
        } else if isAlbumItem, let bId = browseId {
            let album = Album(
                id: bId,
                title: title,
                artist: artistName,
                thumbnailUrl: thumbUrl,
                browseId: bId
            )
            if !result.albums.contains(where: { $0.id == album.id }) {
                result.albums.append(album)
            }
        } else if let vId = videoId, !title.isEmpty {
            let track = Track(
                id: vId,
                title: title,
                artist: artistName,
                duration: duration,
                thumbnailUrl: thumbUrl,
                highResThumbnailUrl: thumbUrl?.replacingOccurrences(of: "w120-h120", with: "w544-h544"),
                lowResThumbnailUrl: thumbUrl
            )
            if !result.songs.contains(where: { $0.id == track.id }) {
                result.songs.append(track)
            }
        }
    }

    private func parseAlbumTracksFromBrowse(_ root: [String: Any], into tracks: inout [Track]) {
        var tempResult = SearchResult()
        func scan(_ obj: Any) {
            if let dict = obj as? [String: Any] {
                if let renderer = dict["musicResponsiveListItemRenderer"] as? [String: Any] {
                    parseResponsiveItem(renderer, into: &tempResult)
                }
                for (_, value) in dict {
                    scan(value)
                }
            } else if let array = obj as? [Any] {
                for item in array {
                    scan(item)
                }
            }
        }
        scan(root)
        tracks.append(contentsOf: tempResult.songs)
    }

    private func parseArtistBrowse(_ root: [String: Any], artistName: String, albums: inout [Album], songs: inout [Track]) {
        var tempResult = SearchResult()
        func scan(_ obj: Any) {
            if let dict = obj as? [String: Any] {
                if let renderer = dict["musicResponsiveListItemRenderer"] as? [String: Any] {
                    parseResponsiveItem(renderer, into: &tempResult)
                }
                for (_, value) in dict {
                    scan(value)
                }
            } else if let array = obj as? [Any] {
                for item in array {
                    scan(item)
                }
            }
        }
        scan(root)
        albums.append(contentsOf: tempResult.albums)
        songs.append(contentsOf: tempResult.songs)
    }
}
