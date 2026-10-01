import Foundation
import MediaPlayer
import UIKit

public final class NowPlayingManager: @unchecked Sendable {
    public static let shared = NowPlayingManager()

    private init() {}

    public func setupRemoteCommands(
        onPlay: @escaping () -> Void,
        onPause: @escaping () -> Void,
        onTogglePlayPause: @escaping () -> Void,
        onNext: @escaping () -> Void,
        onPrevious: @escaping () -> Void,
        onSeek: @escaping (Double) -> Void
    ) {
        let commandCenter = MPRemoteCommandCenter.shared()

        commandCenter.playCommand.isEnabled = true
        commandCenter.playCommand.addTarget { _ in
            onPlay()
            return .success
        }

        commandCenter.pauseCommand.isEnabled = true
        commandCenter.pauseCommand.addTarget { _ in
            onPause()
            return .success
        }

        commandCenter.togglePlayPauseCommand.isEnabled = true
        commandCenter.togglePlayPauseCommand.addTarget { _ in
            onTogglePlayPause()
            return .success
        }

        commandCenter.nextTrackCommand.isEnabled = true
        commandCenter.nextTrackCommand.addTarget { _ in
            onNext()
            return .success
        }

        commandCenter.previousTrackCommand.isEnabled = true
        commandCenter.previousTrackCommand.addTarget { _ in
            onPrevious()
            return .success
        }

        commandCenter.changePlaybackPositionCommand.isEnabled = true
        commandCenter.changePlaybackPositionCommand.addTarget { event in
            if let posEvent = event as? MPChangePlaybackPositionCommandEvent {
                onSeek(posEvent.positionTime)
                return .success
            }
            return .commandFailed
        }
    }

    public func updateNowPlaying(
        track: Track?,
        isPlaying: Bool,
        currentPositionSeconds: Double,
        durationSeconds: Double,
        artworkImage: UIImage? = nil
    ) {
        guard let track = track else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            return
        }

        var nowPlayingInfo = [String: Any]()
        nowPlayingInfo[MPMediaItemPropertyTitle] = track.title
        nowPlayingInfo[MPMediaItemPropertyArtist] = track.artist
        nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = track.album
        nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = durationSeconds
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPositionSeconds
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? 1.0 : 0.0

        if let image = artworkImage {
            nowPlayingInfo[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
        }

        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
    }
}
