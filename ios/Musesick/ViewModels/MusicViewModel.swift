import Foundation
import Combine
import SwiftUI

@MainActor
public final class MusicViewModel: ObservableObject {
    @Published public var searchQuery: String = ""
    @Published public var isSearching: Bool = false
    @Published public var searchResults = SearchResult()
    @Published public var trendingSongs: [Track] = []
    @Published public var localTracks: [Track] = []
    @Published public var userPlaylists: [Playlist] = []
    @Published public var selectedArtist: Artist?
    @Published public var artistDetail: ArtistDetailData?
    @Published public var selectedAlbum: Album?
    @Published public var albumTracks: [Track] = []
    @Published public var showFullPlayer: Bool = false

    // New Stream features
    @Published public var favoriteArtists: [Artist] = []
    @Published public var recentlyPlayed: [Track] = []
    @Published public var suggestedTracks: [Track] = []
    @Published public var likedSongs: [Track] = []
    @Published public var fromYourArtistsTracks: [Track] = []
    @Published public var genreSuggestedTracks: [Track] = []
    @Published public var showFavoriteArtistsPrompt: Bool = false
    @Published public var isSearchingArtists: Bool = false
    @Published public var searchedArtists: [Artist] = []

    public let playerManager = PlayerManager.shared
    public let appVersion = "1.1.5"

    public init() {
        loadFavoriteArtists()
        loadRecentlyPlayed()
        loadLikedSongs()
        setupTrackPlaybackObserver()
        loadInitialData()
    }

    private func setupTrackPlaybackObserver() {
        NotificationCenter.default.addObserver(
            forName: Notification.Name("MusesickTrackPlayed"),
            object: nil,
            queue: .main
        ) { [weak self] notification in
            if let track = notification.object as? Track {
                self?.addToRecentlyPlayed(track)
            }
        }
    }

    public func loadInitialData() {
        // Trigger onboarding prompt if user has never selected favorite artists
        let hasSelectedFavorites = UserDefaults.standard.bool(forKey: "musesick_has_selected_favorites")
        if !hasSelectedFavorites && favoriteArtists.isEmpty {
            showFavoriteArtistsPrompt = true
        }

        preloadFavoriteArtistFullImages()

        Task {
            // Load trending / recommended YouTube Music tracks
            let search = await YouTubeService.shared.searchAll(query: "Top Hits 2026")
            self.trendingSongs = search.songs

            loadSuggestions()

            // Load local tracks
            let locals = await LocalMusicService.shared.fetchLocalTracks()
            self.localTracks = locals

            // Sample initial playlists
            if self.userPlaylists.isEmpty {
                self.userPlaylists = [
                    Playlist(id: "favs", title: "Favorite Songs", trackCount: 0, tracks: [], isUserCreated: true)
                ]
            }
        }
    }

    // MARK: - Recently Played

    public func addToRecentlyPlayed(_ track: Track) {
        recentlyPlayed.removeAll(where: { $0.id == track.id })
        recentlyPlayed.insert(track, at: 0)
        if recentlyPlayed.count > 50 {
            recentlyPlayed = Array(recentlyPlayed.prefix(50))
        }
        saveRecentlyPlayed()
    }

    public func clearRecentlyPlayed() {
        recentlyPlayed.removeAll()
        saveRecentlyPlayed()
    }

    public func removeRecentlyPlayed(trackId: String) {
        recentlyPlayed.removeAll(where: { $0.id == trackId })
        saveRecentlyPlayed()
    }

    private func saveRecentlyPlayed() {
        if let data = try? JSONEncoder().encode(recentlyPlayed) {
            UserDefaults.standard.set(data, forKey: "musesick_recently_played")
        }
    }

    private func loadRecentlyPlayed() {
        if let data = UserDefaults.standard.data(forKey: "musesick_recently_played") {
            do {
                let list = try JSONDecoder().decode([Track].self, from: data)
                self.recentlyPlayed = list
                print("Musesick: Successfully loaded recently played: \(list.count)")
            } catch {
                print("Musesick: Failed to decode recently played: \(error)")
            }
        } else {
            print("Musesick: No data found for musesick_recently_played")
        }
    }

    // MARK: - Favourite Artists

    public func saveFavoriteArtists(_ artists: [Artist]) {
        self.favoriteArtists = artists
        if let data = try? JSONEncoder().encode(artists) {
            UserDefaults.standard.set(data, forKey: "musesick_favorite_artists")
        }
        UserDefaults.standard.set(true, forKey: "musesick_has_selected_favorites")
        loadSuggestions()
        preloadFavoriteArtistFullImages()
    }

    public func toggleFavoriteArtist(_ artist: Artist) {
        if let idx = favoriteArtists.firstIndex(where: { $0.name.lowercased() == artist.name.lowercased() || ($0.id == artist.id && !$0.id.isEmpty) }) {
            favoriteArtists.remove(at: idx)
        } else {
            favoriteArtists.append(artist)
        }
        saveFavoriteArtists(favoriteArtists)
    }

    private func loadFavoriteArtists() {
        if let data = UserDefaults.standard.data(forKey: "musesick_favorite_artists"),
           let list = try? JSONDecoder().decode([Artist].self, from: data) {
            var upgraded = list
            var changed = false
            for idx in 0..<upgraded.count {
                if let match = Artist.popularArtists.first(where: { $0.name.lowercased() == upgraded[idx].name.lowercased() }) {
                    if upgraded[idx].thumbnailUrl == nil || upgraded[idx].thumbnailUrl?.contains("lh3.googleusercontent.com/occfWn") == true || upgraded[idx].thumbnailUrl?.isEmpty == true {
                        upgraded[idx].thumbnailUrl = match.thumbnailUrl
                        upgraded[idx].fullImageUrl = match.fullImageUrl
                        upgraded[idx].browseId = match.browseId
                        changed = true
                    }
                }
            }
            self.favoriteArtists = upgraded
            if changed {
                if let encoded = try? JSONEncoder().encode(upgraded) {
                    UserDefaults.standard.set(encoded, forKey: "musesick_favorite_artists")
                }
            }
        }
    }

    public func preloadFavoriteArtistFullImages() {
        Task {
            var updated = false
            for i in 0..<favoriteArtists.count {
                let artist = favoriteArtists[i]

                // Check popularArtists first for instant high quality images
                if let match = Artist.popularArtists.first(where: { $0.name.lowercased() == artist.name.lowercased() }) {
                    if favoriteArtists[i].thumbnailUrl == nil || favoriteArtists[i].thumbnailUrl?.contains("lh3.googleusercontent.com/occfWn") == true || favoriteArtists[i].thumbnailUrl?.isEmpty == true {
                        favoriteArtists[i].thumbnailUrl = match.thumbnailUrl
                        favoriteArtists[i].fullImageUrl = match.fullImageUrl
                        favoriteArtists[i].browseId = match.browseId
                        updated = true
                    }
                } else if artist.thumbnailUrl == nil || artist.thumbnailUrl?.contains("lh3.googleusercontent.com/occfWn") == true {
                    let fresh = await YouTubeService.shared.fetchArtistImage(artistName: artist.name)
                    if let newThumb = fresh.thumbnailUrl {
                        favoriteArtists[i].thumbnailUrl = newThumb
                        updated = true
                    }
                    if let newFull = fresh.fullImageUrl {
                        favoriteArtists[i].fullImageUrl = newFull
                        updated = true
                    }
                    if let bId = fresh.browseId {
                        favoriteArtists[i].browseId = bId
                        updated = true
                    }
                }

                let cur = favoriteArtists[i]

                // 1. Preload thumbnail
                if let thumbStr = cur.thumbnailUrl, !thumbStr.isEmpty,
                   ArtistImageCache.shared.image(for: thumbStr) == nil,
                   let thumbUrl = URL(string: thumbStr) {
                    if let (data, _) = try? await URLSession.shared.data(from: thumbUrl),
                       let img = UIImage(data: data) {
                        ArtistImageCache.shared.insertImage(img, for: thumbStr)
                    }
                }

                // 2. Preload full-size image on opening the app
                let fullStr = cur.highResImageUrl ?? cur.fullImageUrl ?? ""
                if !fullStr.isEmpty,
                   ArtistImageCache.shared.image(for: fullStr) == nil,
                   let fullUrl = URL(string: fullStr) {
                    if let (data, _) = try? await URLSession.shared.data(from: fullUrl),
                       let img = UIImage(data: data) {
                        ArtistImageCache.shared.insertImage(img, for: fullStr)
                    }
                }
            }

            if updated {
                if let data = try? JSONEncoder().encode(favoriteArtists) {
                    UserDefaults.standard.set(data, forKey: "musesick_favorite_artists")
                }
            }
        }
    }

    public func searchArtists(query: String) async {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else {
            searchedArtists = []
            return
        }
        isSearchingArtists = true
        let results = await YouTubeService.shared.searchArtists(query: q)
        searchedArtists = results
        isSearchingArtists = false
    }

    // MARK: - Liked Songs

    public func isLiked(track: Track) -> Bool {
        likedSongs.contains(where: { $0.id == track.id })
    }

    public func toggleLike(track: Track) {
        if let idx = likedSongs.firstIndex(where: { $0.id == track.id }) {
            likedSongs.remove(at: idx)
        } else {
            likedSongs.insert(track, at: 0)
        }
        saveLikedSongs()
    }

    private func saveLikedSongs() {
        if let data = try? JSONEncoder().encode(likedSongs) {
            UserDefaults.standard.set(data, forKey: "musesick_liked_songs")
        }
    }

    private func loadLikedSongs() {
        if let data = UserDefaults.standard.data(forKey: "musesick_liked_songs"),
           let list = try? JSONDecoder().decode([Track].self, from: data) {
            self.likedSongs = list
        }
    }

    // MARK: - Suggested Tracks & Playlists

    public func loadSuggestions() {
        Task {
            // 1. From Your Artists: contains top 7 songs from each favourite artist (capped at 100)
            var artistSongs: [Track] = []
            for artist in favoriteArtists {
                let details = await YouTubeService.shared.getArtistDetails(artist: artist)
                let top7 = details.songs.prefix(7)
                for s in top7 {
                    if !artistSongs.contains(where: { $0.id == s.id }) {
                        artistSongs.append(s)
                        if artistSongs.count >= 100 { break }
                    }
                }
                if artistSongs.count >= 100 { break }
            }
            self.fromYourArtistsTracks = artistSongs

            // 2. Suggested For You: top songs from similar genres
            var genreTracks: [Track] = []
            if !favoriteArtists.isEmpty {
                for artist in favoriteArtists.prefix(4) {
                    let search = await YouTubeService.shared.searchAll(query: "\(artist.name) mix")
                    for s in search.songs {
                        if !genreTracks.contains(where: { $0.id == s.id }) && !artistSongs.contains(where: { $0.id == s.id }) {
                            genreTracks.append(s)
                        }
                    }
                }
            }
            if genreTracks.count < 15 {
                let genreSearch = await YouTubeService.shared.searchAll(query: "Top Hits 2026")
                for s in genreSearch.songs {
                    if !genreTracks.contains(where: { $0.id == s.id }) {
                        genreTracks.append(s)
                    }
                }
            }
            self.genreSuggestedTracks = genreTracks.shuffled()

            // 3. Fallback suggestedTracks
            if !self.fromYourArtistsTracks.isEmpty {
                self.suggestedTracks = self.fromYourArtistsTracks
            } else if !self.genreSuggestedTracks.isEmpty {
                self.suggestedTracks = self.genreSuggestedTracks
            } else {
                self.suggestedTracks = self.trendingSongs
            }
        }
    }

    public func performSearch() {
        let q = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else {
            searchResults = SearchResult()
            return
        }

        isSearching = true
        Task {
            let res = await YouTubeService.shared.searchAll(query: q)
            self.searchResults = res
            self.isSearching = false
        }
    }

    public func playTrack(_ track: Track, queue: [Track]? = nil) {
        addToRecentlyPlayed(track)
        playerManager.play(track: track, newQueue: queue)
        showFullPlayer = true
    }

    public func openArtist(_ artist: Artist) {
        selectedArtist = artist
        artistDetail = nil
        Task {
            let details = await YouTubeService.shared.getArtistDetails(artist: artist)
            self.artistDetail = details
        }
    }

    public func loadAlbumTracks(_ album: Album) {
        albumTracks = []
        Task {
            if let bId = album.browseId {
                let tracks = await YouTubeService.shared.getAlbumTracks(browseId: bId)
                self.albumTracks = tracks
            } else {
                let s = await YouTubeService.shared.searchAll(query: "\(album.title) \(album.artist)")
                self.albumTracks = s.songs
            }
        }
    }

    public func openAlbum(_ album: Album) {
        selectedAlbum = album
        loadAlbumTracks(album)
    }

    public func playAll(tracks: [Track], shuffle: Bool = false) {
        guard let first = (shuffle ? tracks.shuffled().first : tracks.first) else { return }
        let queue = shuffle ? tracks.shuffled() : tracks
        playTrack(first, queue: queue)
    }

    public func handleIncomingURL(_ url: URL) {
        if url.scheme == "musesick" && url.host == "artist" {
            let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
            if let name = components?.queryItems?.first(where: { $0.name == "name" })?.value {
                let match = favoriteArtists.first(where: { $0.name.lowercased() == name.lowercased() })
                    ?? Artist.popularArtists.first(where: { $0.name.lowercased() == name.lowercased() })
                    ?? Artist(id: name, name: name)
                openArtist(match)
                return
            }
        }
        if url.scheme == "musesick" && url.host == "toggle_like" {
            if let track = playerManager.state.currentTrack {
                toggleLike(track: track)
            }
            return
        }
        if url.scheme == "musesick" && url.host == "lyrics" {
            showFullPlayer = true
            NotificationCenter.default.post(name: NSNotification.Name("MusesickShowLyrics"), object: nil)
            return
        }
        if url.scheme == "musesick" && url.host == "show_all_songs" {
            NotificationCenter.default.post(name: NSNotification.Name("MusesickShowAllSongs"), object: nil)
            return
        }
        if url.scheme == "musesick" && url.host == "show_all_albums" {
            NotificationCenter.default.post(name: NSNotification.Name("MusesickShowAllAlbums"), object: nil)
            return
        }
        if url.scheme == "musesick" && url.host == "open_album" {
            let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
            let title = components?.queryItems?.first(where: { $0.name == "title" })?.value ?? "Album"
            let artist = components?.queryItems?.first(where: { $0.name == "artist" })?.value ?? "Artist"
            let album = Album(id: "album_test", title: title, artist: artist)
            NotificationCenter.default.post(name: NSNotification.Name("MusesickOpenAlbum"), object: album)
            return
        }
        if url.scheme == "musesick" && url.host == "open_playlist" {
            let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
            let id = components?.queryItems?.first(where: { $0.name == "id" })?.value ?? "from_your_artists"
            NotificationCenter.default.post(name: NSNotification.Name("MusesickOpenPlaylist"), object: id)
            return
        }

        var videoId: String?
        if let components = URLComponents(url: url, resolvingAgainstBaseURL: false) {
            if let vItem = components.queryItems?.first(where: { $0.name == "v" }) {
                videoId = vItem.value
            } else if url.host == "youtu.be" {
                videoId = url.pathComponents.dropFirst().first
            } else if url.pathComponents.contains("channel") {
                if let idx = url.pathComponents.firstIndex(of: "channel"), idx + 1 < url.pathComponents.count {
                    let browseId = url.pathComponents[idx + 1]
                    let a = Artist(id: browseId, name: "Artist", browseId: browseId)
                    openArtist(a)
                    return
                }
            }
        }
        guard let id = videoId, !id.isEmpty else { return }
        let track = Track(
            id: id,
            title: "Streaming Track",
            artist: "YouTube",
            album: "",
            duration: "0:00",
            durationMs: 0,
            thumbnailUrl: "https://i.ytimg.com/vi/\(id)/hqdefault.jpg",
            isLocal: false
        )
        playTrack(track)
    }
}
