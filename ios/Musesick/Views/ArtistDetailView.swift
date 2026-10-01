import SwiftUI

public struct ArtistDetailView: View {
    let artist: Artist
    @ObservedObject var viewModel: MusicViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var showAllSongs: Bool = false
    @State private var showAllAlbums: Bool = false
    @State private var selectedAlbum: Album? = nil

    public init(artist: Artist, viewModel: MusicViewModel) {
        self.artist = artist
        self.viewModel = viewModel
    }

    public var body: some View {
        GeometryReader { geometry in
            let topHeight = max(geometry.size.height / 3.0, 275)

            ZStack(alignment: .topLeading) {
                Color.black.ignoresSafeArea()

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        // MARK: - Top 1/3: Artist Photo + Black Gradient Transition
                        ZStack(alignment: .bottomLeading) {
                            // Hero Artist Image with progressive loading (thumb first -> full size)
                            ArtistPhotoView(artist: heroArtist, isHero: true)
                                .frame(width: geometry.size.width, height: topHeight)
                                .clipped()

                            // Black gradient transition between top 1/3 and bottom 2/3
                            LinearGradient(
                                stops: [
                                    .init(color: .clear, location: 0.0),
                                    .init(color: .black.opacity(0.25), location: 0.35),
                                    .init(color: .black.opacity(0.8), location: 0.75),
                                    .init(color: .black, location: 1.0)
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                            .frame(height: topHeight * 0.8)

                            // Artist Name positioned at the bottom of the gradient transition
                            VStack(alignment: .leading, spacing: 4) {
                                Text(artist.name)
                                    .font(.system(size: 30, weight: .heavy))
                                    .foregroundColor(.white)
                                    .lineLimit(1)
                                    .shadow(color: .black.opacity(0.9), radius: 8, y: 3)
                            }
                            .padding(.horizontal, 20)
                            .padding(.bottom, 14)
                        }
                        .frame(width: geometry.size.width, height: topHeight)

                        // MARK: - Bottom 2/3: Play / Shuffle Play + Songs + Albums
                        VStack(spacing: 20) {
                            // Play and Shuffle Play Buttons
                            HStack(spacing: 14) {
                                Button(action: {
                                    if let songs = viewModel.artistDetail?.songs, !songs.isEmpty {
                                        viewModel.playAll(tracks: songs, shuffle: false)
                                    }
                                }) {
                                    HStack(spacing: 8) {
                                        Image(systemName: "play.fill")
                                        Text("Play")
                                            .fontWeight(.bold)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 13)
                                    .background(Color.white)
                                    .foregroundColor(.black)
                                    .clipShape(Capsule())
                                }

                                Button(action: {
                                    if let songs = viewModel.artistDetail?.songs, !songs.isEmpty {
                                        viewModel.playAll(tracks: songs, shuffle: true)
                                    }
                                }) {
                                    HStack(spacing: 8) {
                                        Image(systemName: "shuffle")
                                        Text("Shuffle Play")
                                            .fontWeight(.bold)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 13)
                                    .background(Color.white.opacity(0.18))
                                    .foregroundColor(.white)
                                    .clipShape(Capsule())
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.top, 4)

                            // Popular Songs Section (Top 7 songs + See all)
                            if let songs = viewModel.artistDetail?.songs, !songs.isEmpty {
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack {
                                        Text("POPULAR SONGS")
                                            .font(.system(size: 13, weight: .bold))
                                            .tracking(1.2)
                                            .foregroundColor(.white.opacity(0.7))

                                        Spacer()

                                        if songs.count > 7 {
                                            Button(action: { showAllSongs = true }) {
                                                Text("See all (\(songs.count))")
                                                    .font(.system(size: 12, weight: .semibold))
                                                    .foregroundColor(.white.opacity(0.85))
                                            }
                                        }
                                    }
                                    .padding(.horizontal, 20)

                                    let top7 = Array(songs.prefix(7))
                                    VStack(spacing: 4) {
                                        ForEach(top7) { song in
                                            songRow(song: song, list: songs)
                                        }
                                    }
                                    .padding(.horizontal, 20)

                                    if songs.count > 7 {
                                        Button(action: { showAllSongs = true }) {
                                            HStack {
                                                Image(systemName: "music.note.list")
                                                    .font(.system(size: 13))
                                                Text("See all \(songs.count) songs")
                                                    .font(.system(size: 13, weight: .semibold))
                                                Spacer()
                                                Image(systemName: "chevron.right")
                                                    .font(.system(size: 12, weight: .semibold))
                                            }
                                            .foregroundColor(.white.opacity(0.9))
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 11)
                                            .background(Color.white.opacity(0.1))
                                            .clipShape(RoundedRectangle(cornerRadius: 10))
                                        }
                                        .buttonStyle(.plain)
                                        .padding(.horizontal, 20)
                                        .padding(.top, 4)
                                    }
                                }
                            } else if viewModel.artistDetail == nil {
                                HStack {
                                    Spacer()
                                    ProgressView()
                                        .tint(.white)
                                        .padding(.vertical, 30)
                                    Spacer()
                                }
                            }

                            // Albums & Releases Section (Top 5 albums + See all)
                            if let albums = viewModel.artistDetail?.albums, !albums.isEmpty {
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack {
                                        Text("ALBUMS & RELEASES")
                                            .font(.system(size: 13, weight: .bold))
                                            .tracking(1.2)
                                            .foregroundColor(.white.opacity(0.7))

                                        Spacer()

                                        if albums.count > 5 {
                                            Button(action: { showAllAlbums = true }) {
                                                Text("See all (\(albums.count))")
                                                    .font(.system(size: 12, weight: .semibold))
                                                    .foregroundColor(.white.opacity(0.85))
                                            }
                                        }
                                    }
                                    .padding(.horizontal, 20)

                                    let top5 = Array(albums.prefix(5))
                                    ScrollView(.horizontal, showsIndicators: false) {
                                        HStack(spacing: 16) {
                                            ForEach(top5) { album in
                                                albumCard(album: album)
                                            }
                                        }
                                        .padding(.horizontal, 20)
                                    }

                                    if albums.count > 5 {
                                        Button(action: { showAllAlbums = true }) {
                                            HStack {
                                                Image(systemName: "square.stack.fill")
                                                    .font(.system(size: 13))
                                                Text("See all \(albums.count) albums & releases")
                                                    .font(.system(size: 13, weight: .semibold))
                                                Spacer()
                                                Image(systemName: "chevron.right")
                                                    .font(.system(size: 12, weight: .semibold))
                                            }
                                            .foregroundColor(.white.opacity(0.9))
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 11)
                                            .background(Color.white.opacity(0.1))
                                            .clipShape(RoundedRectangle(cornerRadius: 10))
                                        }
                                        .buttonStyle(.plain)
                                        .padding(.horizontal, 20)
                                        .padding(.top, 4)
                                    }
                                }
                            }

                            Spacer().frame(height: 90)
                        }
                        .background(Color.black)
                    }
                }
                .ignoresSafeArea(edges: .top)

                // Dismiss Button floating in top-leading corner
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .padding(10)
                        .background(Circle().fill(Color.black.opacity(0.55)))
                        .overlay(Circle().stroke(Color.white.opacity(0.2), lineWidth: 1))
                }
                .padding(.leading, 16)
                .padding(.top, 16)
            }
        }
        .navigationBarHidden(true)
        .toolbarBackground(.hidden, for: .navigationBar)
        .navigationDestination(isPresented: $showAllSongs) {
            if let songs = viewModel.artistDetail?.songs {
                ArtistAllSongsView(artist: artist, songs: songs, viewModel: viewModel)
            }
        }
        .navigationDestination(isPresented: $showAllAlbums) {
            if let albums = viewModel.artistDetail?.albums {
                ArtistAllAlbumsView(artist: artist, albums: albums, viewModel: viewModel)
            }
        }
        .navigationDestination(isPresented: Binding(
            get: { selectedAlbum != nil },
            set: { if !$0 { selectedAlbum = nil } }
        )) {
            if let album = selectedAlbum {
                AlbumDetailView(album: album, viewModel: viewModel)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("MusesickShowAllSongs"))) { _ in
            showAllSongs = true
        }
        .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("MusesickShowAllAlbums"))) { _ in
            showAllAlbums = true
        }
        .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("MusesickOpenAlbum"))) { notif in
            if let album = notif.object as? Album {
                selectedAlbum = album
                viewModel.loadAlbumTracks(album)
            }
        }
    }

    private var heroArtist: Artist {
        var a = artist
        if let hero = viewModel.artistDetail?.heroImageUrl, !hero.isEmpty {
            a.fullImageUrl = hero
        }
        return a
    }

    private func songRow(song: Track, list: [Track]) -> some View {
        Button(action: {
            viewModel.playTrack(song, queue: list)
        }) {
            HStack(spacing: 12) {
                AsyncImage(url: URL(string: song.lowResThumbnailUrl ?? song.thumbnailUrl ?? "")) { phase in
                    if let image = phase.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    } else {
                        Color(white: 0.15)
                    }
                }
                .frame(width: 46, height: 46)
                .clipShape(RoundedRectangle(cornerRadius: 8))

                VStack(alignment: .leading, spacing: 3) {
                    Text(song.title)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.white)
                        .lineLimit(1)

                    Text(song.formattedDuration)
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.55))
                }

                Spacer()

                Image(systemName: "play.circle")
                    .font(.system(size: 20))
                    .foregroundColor(.white.opacity(0.5))
            }
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func albumCard(album: Album) -> some View {
        Button(action: {
            selectedAlbum = album
            viewModel.loadAlbumTracks(album)
        }) {
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
            }
        }
        .buttonStyle(.plain)
    }
}
