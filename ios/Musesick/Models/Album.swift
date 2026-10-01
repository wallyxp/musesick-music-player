import Foundation

public struct Album: Identifiable, Codable, Equatable, Hashable {
    public let id: String
    public var title: String
    public var artist: String
    public var thumbnailUrl: String?
    public var year: String?
    public var browseId: String?
    public var tracks: [Track]

    public init(
        id: String,
        title: String,
        artist: String,
        thumbnailUrl: String? = nil,
        year: String? = nil,
        browseId: String? = nil,
        tracks: [Track] = []
    ) {
        self.id = id
        self.title = title
        self.artist = artist
        self.thumbnailUrl = thumbnailUrl
        self.year = year
        self.browseId = browseId
        self.tracks = tracks
    }

    public var highResThumbnailUrl: String? {
        guard let url = thumbnailUrl else { return nil }
        return url.replacingOccurrences(of: "w120-h120", with: "w544-h544")
                  .replacingOccurrences(of: "s60", with: "s544")
    }
}
