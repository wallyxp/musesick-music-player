import SwiftUI

public struct LocalTracksView: View {
    @ObservedObject var viewModel: MusicViewModel

    public init(viewModel: MusicViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                if viewModel.localTracks.isEmpty {
                    VStack(spacing: 14) {
                        Image(systemName: "music.note.house")
                            .font(.system(size: 48))
                            .foregroundColor(.white.opacity(0.4))

                        Text("No Local Tracks Found")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)

                        Text("Sync your music library or add files to access local playback.")
                            .font(.system(size: 14))
                            .foregroundColor(.white.opacity(0.6))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                    }
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            // Top Controls
                            HStack(spacing: 14) {
                                Button(action: {
                                    viewModel.playAll(tracks: viewModel.localTracks, shuffle: false)
                                }) {
                                    HStack {
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
                                    viewModel.playAll(tracks: viewModel.localTracks, shuffle: true)
                                }) {
                                    HStack {
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
                            }
                            .padding(.horizontal, 16)
                            .padding(.top, 8)

                            // Track List
                            VStack(spacing: 4) {
                                ForEach(viewModel.localTracks) { track in
                                    Button(action: {
                                        viewModel.playTrack(track, queue: viewModel.localTracks)
                                    }) {
                                        HStack(spacing: 12) {
                                            Circle()
                                                .fill(Color(white: 0.15))
                                                .frame(width: 44, height: 44)
                                                .overlay(
                                                    Image(systemName: "music.note")
                                                        .foregroundColor(.white.opacity(0.6))
                                                )

                                            VStack(alignment: .leading, spacing: 3) {
                                                Text(track.title)
                                                    .font(.system(size: 15, weight: .medium))
                                                    .foregroundColor(.white)
                                                    .lineLimit(1)

                                                Text("\(track.artist) • \(track.album)")
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
            }
            .navigationTitle("Local Music")
            .navigationBarTitleDisplayMode(.large)
            .toolbarBackground(Color.black, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}
