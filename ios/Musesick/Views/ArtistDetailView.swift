import SwiftUI

public struct ArtistDetailView: View {
    let artist: Artist
    @ObservedObject var viewModel: MusicViewModel
    @Environment(\.dismiss) private var dismiss

    public init(artist: Artist, viewModel: MusicViewModel) {
        self.artist = artist
        self.viewModel = viewModel
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 18) {
                    // Artist Photo
                    AsyncImage(url: URL(string: artist.thumbnailUrl ?? "")) { phase in
                        if let image = phase.image {
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        } else {
                            Circle().fill(Color(white: 0.2))
                        }
                    }
                    .frame(width: 170, height: 170)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.white.opacity(0.15), lineWidth: 2))
                    .shadow(color: .black.opacity(0.5), radius: 12, y: 6)
                    .padding(.top, 16)

                    // Artist Name
                    Text(artist.name)
                        .font(.system(size: 26, weight: .bold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)

                    // Two Buttons below Artist Photo: Play and Shuffle Play
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
                            .padding(.vertical, 12)
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
                            .padding(.vertical, 12)
                            .background(Color.white.opacity(0.2))
                            .foregroundColor(.white)
                            .clipShape(Capsule())
                        }
                    }
                    .padding(.horizontal, 24)

                    // Songs section
                    if let songs = viewModel.artistDetail?.songs, !songs.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("POPULAR SONGS")
                                .font(.system(size: 13, weight: .bold))
                                .tracking(1.2)
                                .foregroundColor(.white.opacity(0.7))
                                .padding(.horizontal, 16)

                            VStack(spacing: 4) {
                                ForEach(songs) { song in
                                    Button(action: {
                                        viewModel.playTrack(song, queue: songs)
                                    }) {
                                        HStack(spacing: 12) {
                                            AsyncImage(url: URL(string: song.lowResThumbnailUrl ?? song.thumbnailUrl ?? "")) { phase in
                                                if let image = phase.image {
                                                    image.resizable().aspectRatio(contentMode: .fill)
                                                } else {
                                                    Color(white: 0.15)
                                                }
                                            }
                                            .frame(width: 44, height: 44)
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
                                        }
                                        .padding(.vertical, 4)
                                        .contentShape(Rectangle())
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.horizontal, 16)
                        }
                    }

                    // Albums section
                    if let albums = viewModel.artistDetail?.albums, !albums.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("ALBUMS & RELEASES")
                                .font(.system(size: 13, weight: .bold))
                                .tracking(1.2)
                                .foregroundColor(.white.opacity(0.7))
                                .padding(.horizontal, 16)

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 16) {
                                    ForEach(albums) { album in
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
                                            }
                                        }
                                    }
                                }
                                .padding(.horizontal, 16)
                            }
                        }
                    }

                    Spacer().frame(height: 80)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.black, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }
}
