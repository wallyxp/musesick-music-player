import SwiftUI

public struct StreamView: View {
    @ObservedObject var viewModel: MusicViewModel
    @FocusState private var isSearchFocused: Bool

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
                            trendingSection
                        }

                        Spacer().frame(height: 80)
                    }
                }
            }
            .navigationTitle("Stream")
            .navigationBarTitleDisplayMode(.large)
            .toolbarBackground(Color.black, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
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

    // MARK: - Trending Section

    private var trendingSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("QUICK PICKS")
                    .font(.system(size: 13, weight: .bold))
                    .tracking(1.2)
                    .foregroundColor(.white.opacity(0.7))

                Spacer()

                Button(action: {
                    viewModel.playAll(tracks: viewModel.trendingSongs, shuffle: true)
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

            VStack(spacing: 4) {
                ForEach(viewModel.trendingSongs) { song in
                    songRow(track: song, list: viewModel.trendingSongs)
                }
            }
            .padding(.horizontal, 16)
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
