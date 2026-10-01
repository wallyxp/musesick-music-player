import SwiftUI

public struct RecentlyPlayedView: View {
    @ObservedObject var viewModel: MusicViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showClearAlert = false

    public init(viewModel: MusicViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if viewModel.recentlyPlayed.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 54))
                        .foregroundColor(.white.opacity(0.3))

                    Text("No Recently Played Songs")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)

                    Text("Songs you stream or play locally will show up here.")
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.6))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        // Action Bar: Play All, Shuffle, Clear
                        HStack(spacing: 12) {
                            Button(action: {
                                if let first = viewModel.recentlyPlayed.first {
                                    viewModel.playTrack(first, queue: viewModel.recentlyPlayed)
                                }
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: "play.fill")
                                        .font(.system(size: 13))
                                    Text("Play All")
                                        .font(.system(size: 14, weight: .semibold))
                                }
                                .foregroundColor(.black)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                .background(Color.white)
                                .clipShape(Capsule())
                            }

                            Button(action: {
                                viewModel.playAll(tracks: viewModel.recentlyPlayed, shuffle: true)
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: "shuffle")
                                        .font(.system(size: 13))
                                    Text("Shuffle")
                                        .font(.system(size: 14, weight: .semibold))
                                }
                                .foregroundColor(.white)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                .background(Color.white.opacity(0.15))
                                .clipShape(Capsule())
                            }

                            Spacer()

                            Button(action: {
                                showClearAlert = true
                            }) {
                                HStack(spacing: 4) {
                                    Image(systemName: "trash")
                                        .font(.system(size: 12))
                                    Text("Clear")
                                        .font(.system(size: 13, weight: .medium))
                                }
                                .foregroundColor(.red.opacity(0.85))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(Color.red.opacity(0.12))
                                .clipShape(Capsule())
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 8)

                        // Track count
                        Text("\(viewModel.recentlyPlayed.count) SONGS")
                            .font(.system(size: 12, weight: .bold))
                            .tracking(1.2)
                            .foregroundColor(.white.opacity(0.6))
                            .padding(.horizontal, 16)
                            .padding(.top, 4)

                        // Track list
                        VStack(spacing: 4) {
                            ForEach(Array(viewModel.recentlyPlayed.enumerated()), id: \.element.id) { index, track in
                                trackRow(track: track, index: index)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 90)
                    }
                }
            }
        }
        .navigationTitle("Recently Played")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.black, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .alert("Clear Listening History?", isPresented: $showClearAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Clear All", role: .destructive) {
                viewModel.clearRecentlyPlayed()
            }
        } message: {
            Text("This will remove all songs from your recently played list.")
        }
    }

    private func trackRow(track: Track, index: Int) -> some View {
        Button(action: {
            viewModel.playTrack(track, queue: viewModel.recentlyPlayed)
        }) {
            HStack(spacing: 12) {
                // Index number
                Text("\(index + 1)")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.white.opacity(0.4))
                    .frame(width: 22, alignment: .leading)

                // Thumbnail
                AsyncImage(url: URL(string: track.lowResThumbnailUrl ?? track.thumbnailUrl ?? "")) { phase in
                    if let image = phase.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    } else {
                        Color(white: 0.15)
                    }
                }
                .frame(width: 48, height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 8))

                // Metadata
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

                // Remove button
                Button(action: {
                    withAnimation {
                        viewModel.removeRecentlyPlayed(trackId: track.id)
                    }
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.white.opacity(0.4))
                        .padding(8)
                }
                .buttonStyle(.plain)

                // Duration
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
