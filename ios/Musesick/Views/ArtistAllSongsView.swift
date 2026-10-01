import SwiftUI

public struct ArtistAllSongsView: View {
    let artist: Artist
    let songs: [Track]
    @ObservedObject var viewModel: MusicViewModel
    @Environment(\.dismiss) private var dismiss

    public init(artist: Artist, songs: [Track], viewModel: MusicViewModel) {
        self.artist = artist
        self.songs = songs
        self.viewModel = viewModel
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    // Header Summary & Action Buttons
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(artist.name)
                                    .font(.system(size: 24, weight: .bold))
                                    .foregroundColor(.white)
                                Text("\(songs.count) Songs")
                                    .font(.system(size: 13))
                                    .foregroundColor(.white.opacity(0.6))
                            }
                            Spacer()
                        }

                        // Play All & Shuffle Play
                        HStack(spacing: 12) {
                            Button(action: {
                                if !songs.isEmpty {
                                    viewModel.playAll(tracks: songs, shuffle: false)
                                }
                            }) {
                                HStack(spacing: 8) {
                                    Image(systemName: "play.fill")
                                    Text("Play All")
                                        .fontWeight(.bold)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.white)
                                .foregroundColor(.black)
                                .clipShape(Capsule())
                            }

                            Button(action: {
                                if !songs.isEmpty {
                                    viewModel.playAll(tracks: songs, shuffle: true)
                                }
                            }) {
                                HStack(spacing: 8) {
                                    Image(systemName: "shuffle")
                                    Text("Shuffle")
                                        .fontWeight(.bold)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.white.opacity(0.18))
                                .foregroundColor(.white)
                                .clipShape(Capsule())
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 12)

                    // Song List
                    VStack(spacing: 4) {
                        ForEach(Array(songs.enumerated()), id: \.element.id) { index, song in
                            Button(action: {
                                viewModel.playTrack(song, queue: songs)
                            }) {
                                HStack(spacing: 12) {
                                    Text("\(index + 1)")
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(.white.opacity(0.45))
                                        .frame(width: 26, alignment: .leading)

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

                                    Image(systemName: "play.circle")
                                        .font(.system(size: 20))
                                        .foregroundColor(.white.opacity(0.5))
                                }
                                .padding(.vertical, 4)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 20)

                    Spacer().frame(height: 80)
                }
            }
        }
        .navigationTitle("All Songs")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.black, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }
}
