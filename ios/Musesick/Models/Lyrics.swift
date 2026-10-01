import Foundation

public struct LyricLine: Identifiable, Codable, Equatable, Hashable {
    public var id: String { "\(timeMs)_\(text)" }
    public let timeMs: Int64
    public let text: String

    public init(timeMs: Int64, text: String) {
        self.timeMs = timeMs
        self.text = text
    }
}

public enum LyricsUiState: Equatable {
    case idle
    case loading
    case success([LyricLine])
    case instrumental
    case notFound(String)
    case error(String)

    public var lyricsList: [LyricLine] {
        if case .success(let lines) = self {
            return lines
        }
        return []
    }
}
