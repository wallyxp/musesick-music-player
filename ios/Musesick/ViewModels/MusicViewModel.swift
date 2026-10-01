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

    public let playerManager = PlayerManager.shared
    public let appVersion = "1.1.5"

    public init() {
        loadInitialData()
    }

    public func loadInitialData() {
        Task {
            // Load trending / recommended YouTube Music tracks
            let search = await YouTubeService.shared.searchAll(query: "Top Hits 2026")
            self.trendingSongs = search.songs

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

    public func openAlbum(_ album: Album) {
        selectedAlbum = album
        albumTracks = []
        Task {
            if let bId = album.browseId {
                let tracks = await YouTubeService.shared.getAlbumTracks(browseId: bId)
                self.albumTracks = tracks
            }
        }
    }

    public func playAll(tracks: [Track], shuffle: Bool = false) {
        guard let first = (shuffle ? tracks.shuffled().first : tracks.first) else { return }
        let queue = shuffle ? tracks.shuffled() : tracks
        playTrack(first, queue: queue)
    }

    public func handleIncomingURL(_ url: URL) {
        var videoId: String?
        if let components = URLComponents(url: url, resolvingAgainstBaseURL: false) {
            if let vItem = components.queryItems?.first(where: { $0.name == "v" }) {
                videoId = vItem.value
            } else if url.host == "youtu.be" {
                videoId = url.pathComponents.dropFirst().first
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
