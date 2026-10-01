import Foundation

public enum AudioFormat: String, Codable, CaseIterable {
    case aac = "AAC"
    case opus = "OPUS"
    case mp3 = "MP3"
    case flac = "FLAC"
    case m4a = "M4A"
    case unknown = "AUDIO"

    public var label: String {
        switch self {
        case .aac: return "AAC • 256kbps"
        case .opus: return "OPUS • 160kbps"
        case .mp3: return "MP3 • 320kbps"
        case .flac: return "FLAC • Lossless"
        case .m4a: return "M4A • 256kbps"
        case .unknown: return "HQ AUDIO"
        }
    }
}

public struct Track: Identifiable, Codable, Equatable, Hashable {
    public let id: String
    public var title: String
    public var artist: String
    public var album: String
    public var duration: String
    public var durationMs: Int64
    public var thumbnailUrl: String?
    public var highResThumbnailUrl: String?
    public var lowResThumbnailUrl: String?
    public var isLocal: BooleanLiteralType = false
    public var localPath: String?
    public var isVideo: Bool = false
    public var audioFormat: AudioFormat = .aac

    public init(
        id: String,
        title: String,
        artist: String,
        album: String = "",
        duration: String = "0:00",
        durationMs: Int64 = 0,
        thumbnailUrl: String? = nil,
        highResThumbnailUrl: String? = nil,
        lowResThumbnailUrl: String? = nil,
        isLocal: Bool = false,
        localPath: String? = nil,
        isVideo: Bool = false,
        audioFormat: AudioFormat = .aac
    ) {
        self.id = id
        self.title = title
        self.artist = artist
        self.album = album
        self.duration = duration
        if durationMs > 0 {
            self.durationMs = durationMs
        } else {
            let parts = duration.split(separator: ":").compactMap { Int64($0) }
            if parts.count == 2 {
                self.durationMs = (parts[0] * 60 + parts[1]) * 1000
            } else if parts.count == 3 {
                self.durationMs = (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            } else {
                self.durationMs = 0
            }
        }
        self.thumbnailUrl = thumbnailUrl
        self.highResThumbnailUrl = highResThumbnailUrl ?? thumbnailUrl
        self.lowResThumbnailUrl = lowResThumbnailUrl ?? thumbnailUrl
        self.isLocal = isLocal
        self.localPath = localPath
        self.isVideo = isVideo
        self.audioFormat = audioFormat
    }

    public var formattedDuration: String {
        if duration != "0:00" && !duration.isEmpty {
            return duration
        }
        let totalSeconds = durationMs / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    public var youtubeMusicUrl: String {
        return "https://music.youtube.com/watch?v=\(id)"
    }
}
