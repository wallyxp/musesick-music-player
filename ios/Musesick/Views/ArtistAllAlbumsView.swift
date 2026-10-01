import SwiftUI

public struct ArtistAllAlbumsView: View {
    let artist: Artist
    let albums: [Album]
    @ObservedObject var viewModel: MusicViewModel
    @State private var selectedAlbum: Album? = nil

    private let columns = [
        GridItem(.flexible(), spacing: 16),
        GridItem(.flexible(), spacing: 16)
    ]

    public init(artist: Artist, albums: [Album], viewModel: MusicViewModel) {
        self.artist = artist
        self.albums = albums
        self.viewModel = viewModel
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(artist.name)
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                        Text("\(albums.count) Albums & Releases")
                            .font(.system(size: 13))
                            .foregroundColor(.white.opacity(0.6))
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 12)

                    LazyVGrid(columns: columns, spacing: 20) {
                        ForEach(albums) { album in
                            Button(action: {
                                selectedAlbum = album
                                viewModel.loadAlbumTracks(album)
                            }) {
                                VStack(alignment: .leading, spacing: 8) {
                                    AsyncImage(url: URL(string: album.highResThumbnailUrl ?? album.thumbnailUrl ?? "")) { phase in
                                        if let image = phase.image {
                                            image
                                                .resizable()
                                                .aspectRatio(contentMode: .fill)
                                        } else {
                                            RoundedRectangle(cornerRadius: 12).fill(Color(white: 0.18))
                                        }
                                    }
                                    .frame(width: (UIScreen.main.bounds.width - 56) / 2, height: (UIScreen.main.bounds.width - 56) / 2)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                    .shadow(color: .black.opacity(0.5), radius: 8, y: 4)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(album.title)
                                            .font(.system(size: 14, weight: .semibold))
                                            .foregroundColor(.white)
                                            .lineLimit(1)

                                        Text(album.artist)
                                            .font(.system(size: 12))
                                            .foregroundColor(.white.opacity(0.6))
                                            .lineLimit(1)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 20)

                    Spacer().frame(height: 80)
                }
            }
        }
        .navigationTitle("All Albums")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.black, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .navigationDestination(isPresented: Binding(
            get: { selectedAlbum != nil },
            set: { if !$0 { selectedAlbum = nil } }
        )) {
            if let album = selectedAlbum {
                AlbumDetailView(album: album, viewModel: viewModel)
            }
        }
    }
}
