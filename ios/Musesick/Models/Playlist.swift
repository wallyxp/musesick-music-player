import Foundation

public struct Playlist: Identifiable, Codable, Equatable, Hashable {
    public let id: String
    public var title: String
    public var trackCount: Int
    public var thumbnailUrl: String?
    public var tracks: [Track]
    public var isUserCreated: Bool

    public init(
        id: String,
        title: String,
        trackCount: Int = 0,
        thumbnailUrl: String? = nil,
        tracks: [Track] = [],
        isUserCreated: Bool = true
    ) {
        self.id = id
        self.title = title
        self.trackCount = trackCount
        self.thumbnailUrl = thumbnailUrl
        self.tracks = tracks
        self.isUserCreated = isUserCreated
    }
}
