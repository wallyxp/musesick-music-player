import Foundation
import MediaPlayer

public final class LocalMusicService: @unchecked Sendable {
    public static let shared = LocalMusicService()

    public func fetchLocalTracks() async -> [Track] {
        return await withCheckedContinuation { continuation in
            MPMediaLibrary.requestAuthorization { status in
                guard status == .authorized else {
                    continuation.resume(returning: [])
                    return
                }

                let query = MPMediaQuery.songs()
                guard let items = query.items else {
                    continuation.resume(returning: [])
                    return
                }

                var tracks: [Track] = []
                for item in items {
                    let title = item.title ?? "Unknown Title"
                    let artist = item.artist ?? "Unknown Artist"
                    let album = item.albumTitle ?? ""
                    let duration = item.playbackDuration
                    let persistentId = String(item.persistentID)
                    let durationMs = Int64(duration * 1000)

                    let minutes = Int(duration) / 60
                    let seconds = Int(duration) % 60
                    let formatted = String(format: "%d:%02d", minutes, seconds)

                    let track = Track(
                        id: "local_\(persistentId)",
                        title: title,
                        artist: artist,
                        album: album,
                        duration: formatted,
                        durationMs: durationMs,
                        thumbnailUrl: nil,
                        isLocal: true,
                        localPath: persistentId,
                        audioFormat: .m4a
                    )
                    tracks.append(track)
                }

                continuation.resume(returning: tracks)
            }
        }
    }
}
