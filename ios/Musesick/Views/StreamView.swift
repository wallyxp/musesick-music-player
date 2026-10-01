import SwiftUI

public struct StreamView: View {
    @ObservedObject var viewModel: MusicViewModel
    @FocusState private var isSearchFocused: Bool
    @State private var showAllRecentlyPlayed = false
    @State private var selectedPlaylist: Playlist?

    public init(viewModel: MusicViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        // Search Field
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .foregroundColor(.white.opacity(0.5))

                            TextField("Search YouTube Music", text: $viewModel.searchQuery)
                                .foregroundColor(.white)
                                .focused($isSearchFocused)
                                .onSubmit {
                                    viewModel.performSearch()
                                }

                            if !viewModel.searchQuery.isEmpty {
                                Button(action: {
                                    viewModel.searchQuery = ""
                                    viewModel.searchResults = SearchResult()
                                }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundColor(.white.opacity(0.5))
                                }
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(Color(white: 0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .padding(.horizontal, 16)
                        .padding(.top, 8)

                        if viewModel.isSearching {
                            HStack {
                                Spacer()
                                ProgressView()
                                    .tint(.white)
                                Spacer()
                            }
                            .padding(.top, 30)
                        } else if !viewModel.searchResults.songs.isEmpty || !viewModel.searchResults.artists.isEmpty || !viewModel.searchResults.albums.isEmpty {
                            searchResultsSection
                        } else {
                            mainStreamSections
                        }

                        Spacer().frame(height: 80)
                    }
                }
            }
            .navigationTitle("Stream")
            .navigationBarTitleDisplayMode(.large)
            .toolbarBackground(Color.black, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .navigationDestination(isPresented: $showAllRecentlyPlayed) {
                RecentlyPlayedView(viewModel: viewModel)
            }
            .navigationDestination(isPresented: Binding(
                get: { selectedPlaylist != nil },
                set: { if !$0 { selectedPlaylist = nil } }
            )) {
                if let playlist = selectedPlaylist {
                    PlaylistDetailView(playlist: playlist, viewModel: viewModel)
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("MusesickOpenPlaylist"))) { notif in
                if let id = notif.object as? String, let match = curatedPlaylists.first(where: { $0.id == id }) {
                    selectedPlaylist = match
                }
            }
        }
    }

    // MARK: - Search Results

    private var searchResultsSection: some View {
        VStack(alignment: .leading, spacing: 20) {
            // Artists
            if !viewModel.searchResults.artists.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text("ARTISTS")
                        .font(.system(size: 13, weight: .bold))
                        .tracking(1.2)
                        .foregroundColor(.white.opacity(0.7))
                        .padding(.horizontal, 16)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 16) {
                            ForEach(viewModel.searchResults.artists) { artist in
                                Button(action: { viewModel.openArtist(artist) }) {
                                    VStack(spacing: 8) {
                                        AsyncImage(url: URL(string: artist.thumbnailUrl ?? "")) { phase in
                                            if let image = phase.image {
                                                image.resizable().aspectRatio(contentMode: .fill)
                                            } else {
                                                Circle().fill(Color(white: 0.2))
                                            }
                                        }
                                        .frame(width: 80, height: 80)
                                        .clipShape(Circle())

                                        Text(artist.name)
                                            .font(.system(size: 12, weight: .medium))
                                            .foregroundColor(.white)
                                            .lineLimit(1)
                                            .frame(width: 86)
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
            }

            // Songs
            if !viewModel.searchResults.songs.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text("SONGS")
                        .font(.system(size: 13, weight: .bold))
                        .tracking(1.2)
                        .foregroundColor(.white.opacity(0.7))
                        .padding(.horizontal, 16)

                    VStack(spacing: 4) {
                        ForEach(viewModel.searchResults.songs) { song in
                            songRow(track: song, list: viewModel.searchResults.songs)
                        }
                    }
                    .padding(.horizontal, 16)
                }
            }

            // Albums
            if !viewModel.searchResults.albums.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text("ALBUMS")
                        .font(.system(size: 13, weight: .bold))
                        .tracking(1.2)
                        .foregroundColor(.white.opacity(0.7))
                        .padding(.horizontal, 16)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 16) {
                            ForEach(viewModel.searchResults.albums) { album in
                                Button(action: { viewModel.openAlbum(album) }) {
                                    VStack(alignment: .leading, spacing: 6) {
                                        AsyncImage(url: URL(string: album.thumbnailUrl ?? "")) { phase in
                                            if let image = phase.image {
                                                image.resizable().aspectRatio(contentMode: .fill)
                                            } else {
                                                RoundedRectangle(cornerRadius: 10).fill(Color(white: 0.2))
                                            }
                                        }
                                        .frame(width: 120, height: 120)
                                        .clipShape(RoundedRectangle(cornerRadius: 10))

                                        Text(album.title)
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundColor(.white)
                                            .lineLimit(1)
                                            .frame(width: 120, alignment: .leading)

                                        Text(album.artist)
                                            .font(.system(size: 11))
                                            .foregroundColor(.white.opacity(0.6))
                                            .lineLimit(1)
                                            .frame(width: 120, alignment: .leading)
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
            }
        }
    }

    // MARK: - Main Stream Sections (3 Core Sections)

    private var mainStreamSections: some View {
        VStack(alignment: .leading, spacing: 26) {
            recentlyPlayedSection
            favouriteArtistsSection
            suggestedForYouSection
        }
    }

    // MARK: - 1. Recently Played (Last 5 played songs + See all)

    private var recentlyPlayedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("RECENTLY PLAYED")
                    .font(.system(size: 13, weight: .bold))
                    .tracking(1.2)
                    .foregroundColor(.white.opacity(0.7))

                Spacer()

                if !viewModel.recentlyPlayed.isEmpty {
                    Button(action: { showAllRecentlyPlayed = true }) {
                        Text("See all (\(viewModel.recentlyPlayed.count))")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.white.opacity(0.85))
                    }
                }
            }
            .padding(.horizontal, 16)

            if viewModel.recentlyPlayed.isEmpty {
                HStack(spacing: 12) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 24))
                        .foregroundColor(.white.opacity(0.3))

                    VStack(alignment: .leading, spacing: 2) {
                        Text("No recently played songs")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.white.opacity(0.85))
                        Text("Songs you play will appear here")
                            .font(.system(size: 12))
                            .foregroundColor(.white.opacity(0.5))
                    }
                    Spacer()
                }
                .padding(14)
                .background(Color(white: 0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(.horizontal, 16)
            } else {
                let last5 = Array(viewModel.recentlyPlayed.prefix(5))
                VStack(spacing: 4) {
                    ForEach(last5) { song in
                        songRow(track: song, list: viewModel.recentlyPlayed)
                    }
                }
                .padding(.horizontal, 16)

                // Option below it for seeing all -> shows all the recently played songs
                Button(action: { showAllRecentlyPlayed = true }) {
                    HStack {
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.system(size: 14))
                        Text("See all recently played (\(viewModel.recentlyPlayed.count))")
                            .font(.system(size: 13, weight: .semibold))
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundColor(.white.opacity(0.9))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(white: 0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 16)
                .padding(.top, 4)
            }
        }
    }

    // MARK: - 2. Favourite Artists

    private var favouriteArtistsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("FAVOURITE ARTISTS")
                    .font(.system(size: 13, weight: .bold))
                    .tracking(1.2)
                    .foregroundColor(.white.opacity(0.7))

                Spacer()

                Button(action: {
                    viewModel.showFavoriteArtistsPrompt = true
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: viewModel.favoriteArtists.isEmpty ? "plus.circle.fill" : "pencil")
                            .font(.system(size: 11))
                        Text(viewModel.favoriteArtists.isEmpty ? "Select" : "Edit")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color.white.opacity(0.18))
                    .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 16)

            if viewModel.favoriteArtists.isEmpty {
                Button(action: {
                    viewModel.showFavoriteArtistsPrompt = true
                }) {
                    HStack(spacing: 12) {
                        Image(systemName: "person.crop.circle.badge.plus")
                            .font(.system(size: 26))
                            .foregroundColor(.white.opacity(0.5))

                        VStack(alignment: .leading, spacing: 2) {
                            Text("Pick your favourite artists")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.white)
                            Text("Personalize your music stream and recommendations")
                                .font(.system(size: 12))
                                .foregroundColor(.white.opacity(0.55))
                        }

                        Spacer()

                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.white.opacity(0.4))
                    }
                    .padding(14)
                    .background(Color(white: 0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal, 16)
                }
                .buttonStyle(.plain)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 16) {
                        ForEach(viewModel.favoriteArtists) { artist in
                            Button(action: { viewModel.openArtist(artist) }) {
                                VStack(spacing: 8) {
                                    ArtistPhotoView(artist: artist)
                                        .frame(width: 76, height: 76)
                                        .clipShape(Circle())
                                        .overlay(Circle().stroke(Color.white.opacity(0.15), lineWidth: 1))

                                    Text(artist.name)
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundColor(.white)
                                        .lineLimit(1)
                                        .frame(width: 80)
                                }
                            }
                            .buttonStyle(.plain)
                        }

                        // Add more button
                        Button(action: {
                            viewModel.showFavoriteArtistsPrompt = true
                        }) {
                            VStack(spacing: 8) {
                                Circle()
                                    .fill(Color(white: 0.14))
                                    .frame(width: 76, height: 76)
                                    .overlay(
                                        Image(systemName: "plus")
                                            .font(.system(size: 22, weight: .semibold))
                                            .foregroundColor(.white.opacity(0.8))
                                    )
                                    .overlay(Circle().stroke(Color.white.opacity(0.15), lineWidth: 1))

                                Text("Add More")
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundColor(.white.opacity(0.7))
                                    .lineLimit(1)
                                    .frame(width: 80)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 16)
                }
            }
        }
    }

    // MARK: - 3. Suggested for You

    private var curatedPlaylists: [Playlist] {
        [
            Playlist(
                id: "from_your_artists",
                title: "From Your Artists",
                subtitle: "Top 7 songs from your favourites",
                trackCount: viewModel.fromYourArtistsTracks.count,
                tracks: viewModel.fromYourArtistsTracks
            ),
            Playlist(
                id: "suggested_genres",
                title: "Suggested For You",
                subtitle: "Top songs from similar genres",
                trackCount: viewModel.genreSuggestedTracks.count,
                tracks: viewModel.genreSuggestedTracks
            ),
            Playlist(
                id: "liked_songs",
                title: "Your Liked Songs",
                subtitle: "\(viewModel.likedSongs.count) favorites saved",
                trackCount: viewModel.likedSongs.count,
                tracks: viewModel.likedSongs
            ),
            Playlist(
                id: "trending_discoveries",
                title: "Trending Hits",
                subtitle: "Popular right now worldwide",
                trackCount: viewModel.trendingSongs.count,
                tracks: viewModel.trendingSongs
            )
        ]
    }

    private var suggestedForYouSection: some View {
        let displayTracks = viewModel.suggestedTracks.isEmpty ? viewModel.trendingSongs : viewModel.suggestedTracks
        return VStack(alignment: .leading, spacing: 18) {
            // Header
            HStack {
                Text("SUGGESTED FOR YOU")
                    .font(.system(size: 13, weight: .bold))
                    .tracking(1.2)
                    .foregroundColor(.white.opacity(0.7))

                Spacer()
            }
            .padding(.horizontal, 16)

            // 4 Curated Playlists Horizontal Scroll
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 14) {
                    ForEach(curatedPlaylists) { playlist in
                        playlistCard(playlist)
                    }
                }
                .padding(.horizontal, 16)
            }

            // Quick Mix / Song list
            if !displayTracks.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("QUICK MIX")
                            .font(.system(size: 13, weight: .bold))
                            .tracking(1.2)
                            .foregroundColor(.white.opacity(0.7))

                        Spacer()

                        Button(action: {
                            viewModel.playAll(tracks: displayTracks, shuffle: true)
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: "shuffle")
                                    .font(.system(size: 12))
                                Text("Shuffle")
                                    .font(.system(size: 12, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.white.opacity(0.18))
                            .clipShape(Capsule())
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 6)

                    VStack(spacing: 4) {
                        ForEach(Array(displayTracks.prefix(15))) { song in
                            songRow(track: song, list: displayTracks)
                        }
                    }
                    .padding(.horizontal, 16)
                }
            } else {
                HStack {
                    Spacer()
                    ProgressView()
                        .tint(.white)
                        .padding(.vertical, 20)
                    Spacer()
                }
            }
        }
    }

    private func playlistCard(_ playlist: Playlist) -> some View {
        Button(action: {
            selectedPlaylist = playlist
        }) {
            VStack(alignment: .leading, spacing: 8) {
                ZStack(alignment: .bottomTrailing) {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(
                            LinearGradient(
                                colors: playlistGradientColors(for: playlist.id),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 154, height: 136)

                    Image(systemName: playlistIcon(for: playlist.id))
                        .font(.system(size: 44, weight: .semibold))
                        .foregroundColor(.white.opacity(0.85))
                        .frame(width: 154, height: 136, alignment: .center)

                    // Quick Play button
                    Button(action: {
                        let tracks = tracksForPlaylist(playlist)
                        if !tracks.isEmpty {
                            viewModel.playAll(tracks: tracks, shuffle: true)
                        }
                    }) {
                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(.white)
                            .shadow(color: .black.opacity(0.4), radius: 4, y: 2)
                    }
                    .padding(8)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(playlist.title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                        .lineLimit(1)

                    let count = tracksForPlaylist(playlist).count
                    Text(count > 0 ? "\(count) songs" : (playlist.subtitle ?? ""))
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.6))
                        .lineLimit(1)
                }
                .frame(width: 154, alignment: .leading)
            }
        }
        .buttonStyle(.plain)
    }

    private func tracksForPlaylist(_ playlist: Playlist) -> [Track] {
        switch playlist.id {
        case "from_your_artists":
            return viewModel.fromYourArtistsTracks
        case "suggested_genres":
            return viewModel.genreSuggestedTracks
        case "liked_songs":
            return viewModel.likedSongs
        case "trending_discoveries":
            return viewModel.trendingSongs
        default:
            return playlist.tracks
        }
    }

    private func playlistGradientColors(for id: String) -> [Color] {
        switch id {
        case "from_your_artists":
            return [Color(red: 0.55, green: 0.2, blue: 0.85), Color(red: 0.2, green: 0.4, blue: 0.9)]
        case "suggested_genres":
            return [Color(red: 0.2, green: 0.35, blue: 0.85), Color(red: 0.1, green: 0.75, blue: 0.8)]
        case "liked_songs":
            return [Color(red: 0.95, green: 0.2, blue: 0.4), Color(red: 0.65, green: 0.1, blue: 0.35)]
        case "trending_discoveries":
            return [Color(red: 1.0, green: 0.45, blue: 0.15), Color(red: 0.9, green: 0.2, blue: 0.45)]
        default:
            return [Color.blue, Color.purple]
        }
    }

    private func playlistIcon(for id: String) -> String {
        switch id {
        case "from_your_artists":
            return "person.2.circle.fill"
        case "suggested_genres":
            return "sparkles"
        case "liked_songs":
            return "heart.fill"
        case "trending_discoveries":
            return "flame.fill"
        default:
            return "music.note.list"
        }
    }

    private func songRow(track: Track, list: [Track]) -> some View {
        Button(action: {
            viewModel.playTrack(track, queue: list)
        }) {
            HStack(spacing: 12) {
                AsyncImage(url: URL(string: track.lowResThumbnailUrl ?? track.thumbnailUrl ?? "")) { phase in
                    if let image = phase.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    } else {
                        Color(white: 0.15)
                    }
                }
                .frame(width: 48, height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 8))

                VStack(alignment: .leading, spacing: 3) {
                    Text(track.title)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.white)
                        .lineLimit(1)

                    Text("\(track.artist) • \(track.audioFormat.label)")
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.6))
                        .lineLimit(1)
                }

                Spacer()

                Text(track.formattedDuration)
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.45))
            }
            .padding(.vertical, 6)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
