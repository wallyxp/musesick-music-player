import Foundation

public enum PlaybackStatus: String, Codable {
    case stopped
    case buffering
    case playing
    case paused
    case error
}

public struct PlaybackState: Equatable {
    public var currentTrack: Track?
    public var status: PlaybackStatus
    public var currentPositionMs: Int64
    public var durationMs: Int64
    public var isShuffle: Bool
    public var isRepeat: Bool
    public var isBuffering: Bool { status == .buffering }
    public var isPlaying: Bool { status == .playing }
    public var errorMessage: String?

    public init(
        currentTrack: Track? = nil,
        status: PlaybackStatus = .stopped,
        currentPositionMs: Int64 = 0,
        durationMs: Int64 = 0,
        isShuffle: Bool = false,
        isRepeat: Bool = false,
        errorMessage: String? = nil
    ) {
        self.currentTrack = currentTrack
        self.status = status
        self.currentPositionMs = currentPositionMs
        self.durationMs = durationMs
        self.isShuffle = isShuffle
        self.isRepeat = isRepeat
        self.errorMessage = errorMessage
    }

    public var progressFraction: Float {
        guard durationMs > 0 else { return 0 }
        let fraction = Float(currentPositionMs) / Float(durationMs)
        return min(max(fraction, 0), 1)
    }

    public var formattedPosition: String {
        formatTime(ms: currentPositionMs)
    }

    public var formattedDuration: String {
        formatTime(ms: durationMs)
    }

    private func formatTime(ms: Int64) -> String {
        let totalSeconds = ms / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}
