import SwiftUI

public struct MiniPlayerBar: View {
    @ObservedObject var playerManager: PlayerManager
    let onOpenFullPlayer: () -> Void

    public init(playerManager: PlayerManager, onOpenFullPlayer: @escaping () -> Void) {
        self.playerManager = playerManager
        self.onOpenFullPlayer = onOpenFullPlayer
    }

    public var body: some View {
        if let track = playerManager.state.currentTrack {
            Button(action: onOpenFullPlayer) {
                HStack(spacing: 12) {
                    // Artwork Thumbnail
                    AsyncImage(url: URL(string: track.lowResThumbnailUrl ?? track.thumbnailUrl ?? "")) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        default:
                            Color(white: 0.2)
                                .overlay(
                                    Image(systemName: "music.note")
                                        .foregroundColor(.white.opacity(0.6))
                                )
                        }
                    }
                    .frame(width: 44, height: 44)
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                    // Track info
                    VStack(alignment: .leading, spacing: 2) {
                        Text(track.title)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .lineLimit(1)

                        Text(track.artist)
                            .font(.system(size: 12))
                            .foregroundColor(.white.opacity(0.7))
                            .lineLimit(1)
                    }

                    Spacer()

                    // Play/Pause Button
                    Button(action: {
                        playerManager.togglePlayPause()
                    }) {
                        if playerManager.state.isBuffering {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .frame(width: 36, height: 36)
                        } else {
                            Image(systemName: playerManager.state.isPlaying ? "pause.fill" : "play.fill")
                                .font(.system(size: 18))
                                .foregroundColor(.white)
                                .frame(width: 36, height: 36)
                        }
                    }

                    // Next Button
                    Button(action: {
                        playerManager.nextTrack()
                    }) {
                        Image(systemName: "forward.fill")
                            .font(.system(size: 16))
                            .foregroundColor(.white.opacity(0.85))
                            .frame(width: 36, height: 36)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color(white: 0.12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.white.opacity(0.12), lineWidth: 1)
                        )
                        .shadow(color: .black.opacity(0.4), radius: 10, y: 5)
                )
                .padding(.horizontal, 16)
            }
            .buttonStyle(.plain)
        }
    }
}
