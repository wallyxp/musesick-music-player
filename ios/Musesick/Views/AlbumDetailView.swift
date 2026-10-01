import SwiftUI

public struct AlbumDetailView: View {
    let album: Album
    @ObservedObject var viewModel: MusicViewModel

    public init(album: Album, viewModel: MusicViewModel) {
        self.album = album
        self.viewModel = viewModel
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    // Album Cover
                    AsyncImage(url: URL(string: album.highResThumbnailUrl ?? album.thumbnailUrl ?? "")) { phase in
                        if let image = phase.image {
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        } else {
                            RoundedRectangle(cornerRadius: 16).fill(Color(white: 0.18))
                        }
                    }
                    .frame(width: 200, height: 200)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .shadow(color: .black.opacity(0.6), radius: 14, y: 8)
                    .padding(.top, 16)

                    // Title & Artist
                    VStack(spacing: 4) {
                        Text(album.title)
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)

                        Text(album.artist)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.white.opacity(0.75))
                    }
                    .padding(.horizontal, 24)

                    // Play, Shuffle, and Share beside shuffle button
                    HStack(spacing: 12) {
                        Button(action: {
                            if !viewModel.albumTracks.isEmpty {
                                viewModel.playAll(tracks: viewModel.albumTracks, shuffle: false)
                            }
                        }) {
                            HStack(spacing: 6) {
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
                            if !viewModel.albumTracks.isEmpty {
                                viewModel.playAll(tracks: viewModel.albumTracks, shuffle: true)
                            }
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "shuffle")
                                Text("Shuffle")
                                    .fontWeight(.bold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color.white.opacity(0.2))
                            .foregroundColor(.white)
                            .clipShape(Capsule())
                        }

                        // Share button beside the shuffle button
                        if let shareUrl = URL(string: "https://music.youtube.com/browse/\(album.browseId ?? album.id)") {
                            ShareLink(item: shareUrl, message: Text("Check out \(album.title) by \(album.artist) on Musesick")) {
                                Image(systemName: "square.and.arrow.up")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(.white)
                                    .frame(width: 44, height: 44)
                                    .background(Color.white.opacity(0.2))
                                    .clipShape(Circle())
                            }
                        }
                    }
                    .padding(.horizontal, 20)

                    // Album Track List
                    VStack(spacing: 4) {
                        ForEach(Array(viewModel.albumTracks.enumerated()), id: \.element.id) { index, track in
                            Button(action: {
                                viewModel.playTrack(track, queue: viewModel.albumTracks)
                            }) {
                                HStack(spacing: 14) {
                                    Text("\(index + 1)")
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(.white.opacity(0.45))
                                        .frame(width: 24, alignment: .leading)

                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(track.title)
                                            .font(.system(size: 15, weight: .medium))
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
                                .padding(.vertical, 6)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)

                    Spacer().frame(height: 80)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.black, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }
}
