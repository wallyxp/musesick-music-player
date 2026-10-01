import SwiftUI

public struct PlaylistDetailView: View {
    let playlist: Playlist
    @ObservedObject var viewModel: MusicViewModel
    @Environment(\.dismiss) private var dismiss

    public init(playlist: Playlist, viewModel: MusicViewModel) {
        self.playlist = playlist
        self.viewModel = viewModel
    }

    private var currentTracks: [Track] {
        if playlist.id == "from_your_artists" {
            return viewModel.fromYourArtistsTracks
        } else if playlist.id == "suggested_genres" {
            return viewModel.genreSuggestedTracks
        } else if playlist.id == "liked_songs" {
            return viewModel.likedSongs
        } else if playlist.id == "trending_discoveries" {
            return viewModel.trendingSongs
        }
        return playlist.tracks
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    // Header Artwork / Icon
                    ZStack {
                        RoundedRectangle(cornerRadius: 18)
                            .fill(
                                LinearGradient(
                                    colors: playlistGradientColors(for: playlist.id),
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 180, height: 180)
                            .shadow(color: .black.opacity(0.6), radius: 12, y: 6)

                        Image(systemName: playlistIcon(for: playlist.id))
                            .font(.system(size: 64, weight: .semibold))
                            .foregroundColor(.white.opacity(0.9))
                    }
                    .padding(.top, 16)

                    // Title & Description
                    VStack(spacing: 6) {
                        Text(playlist.title)
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)

                        if let subtitle = playlist.subtitle, !subtitle.isEmpty {
                            Text(subtitle)
                                .font(.system(size: 14))
                                .foregroundColor(.white.opacity(0.7))
                                .multilineTextAlignment(.center)
                        }

                        Text("\(currentTracks.count) songs")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(.white.opacity(0.5))
                    }
                    .padding(.horizontal, 24)

                    // Play & Shuffle Action Buttons
                    HStack(spacing: 12) {
                        Button(action: {
                            if !currentTracks.isEmpty {
                                viewModel.playAll(tracks: currentTracks, shuffle: false)
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
                            if !currentTracks.isEmpty {
                                viewModel.playAll(tracks: currentTracks, shuffle: true)
                            }
                        }) {
                            HStack(spacing: 8) {
                                Image(systemName: "shuffle")
                                Text("Shuffle")
                                    .fontWeight(.bold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 13)
                            .background(Color.white.opacity(0.2))
                            .foregroundColor(.white)
                            .clipShape(Capsule())
                        }
                    }
                    .padding(.horizontal, 20)

                    // Track List
                    if currentTracks.isEmpty {
                        VStack(spacing: 10) {
                            Image(systemName: "music.note")
                                .font(.system(size: 32))
                                .foregroundColor(.white.opacity(0.3))
                                .padding(.top, 30)
                            Text("No songs in this playlist yet")
                                .font(.system(size: 15))
                                .foregroundColor(.white.opacity(0.5))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 20)
                    } else {
                        VStack(spacing: 4) {
                            ForEach(Array(currentTracks.enumerated()), id: \.element.id) { index, track in
                                Button(action: {
                                    viewModel.playTrack(track, queue: currentTracks)
                                }) {
                                    HStack(spacing: 12) {
                                        Text("\(index + 1)")
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundColor(.white.opacity(0.45))
                                            .frame(width: 24, alignment: .leading)

                                        AsyncImage(url: URL(string: track.lowResThumbnailUrl ?? track.thumbnailUrl ?? "")) { phase in
                                            if let image = phase.image {
                                                image.resizable().aspectRatio(contentMode: .fill)
                                            } else {
                                                Color(white: 0.15)
                                            }
                                        }
                                        .frame(width: 44, height: 44)
                                        .clipShape(RoundedRectangle(cornerRadius: 8))

                                        VStack(alignment: .leading, spacing: 3) {
                                            Text(track.title)
                                                .font(.system(size: 14, weight: .medium))
                                                .foregroundColor(.white)
                                                .lineLimit(1)

                                            Text(track.artist)
                                                .font(.system(size: 12))
                                                .foregroundColor(.white.opacity(0.6))
                                                .lineLimit(1)
                                        }

                                        Spacer()

                                        Text(track.formattedDuration)
                                            .font(.system(size: 12))
                                            .foregroundColor(.white.opacity(0.5))
                                    }
                                    .padding(.vertical, 4)
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    Spacer().frame(height: 80)
                }
            }
        }
        .navigationTitle(playlist.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.black, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    private func playlistGradientColors(for id: String) -> [Color] {
        switch id {
        case "from_your_artists":
            return [Color.purple, Color.blue]
        case "suggested_genres":
            return [Color.indigo, Color.cyan]
        case "liked_songs":
            return [Color.pink, Color.red]
        case "trending_discoveries":
            return [Color.orange, Color.pink]
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
}
