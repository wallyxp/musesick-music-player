import Foundation

public struct Artist: Identifiable, Codable, Equatable, Hashable {
    public let id: String
    public var name: String
    public var thumbnailUrl: String?
    public var subtitle: String?
    public var browseId: String?

    public init(
        id: String,
        name: String,
        thumbnailUrl: String? = nil,
        subtitle: String? = "Artist",
        browseId: String? = nil
    ) {
        self.id = id
        self.name = name
        self.thumbnailUrl = thumbnailUrl
        self.subtitle = subtitle
        self.browseId = browseId
    }
}

public struct ArtistDetailData {
    public var albums: [Album]
    public var songs: [Track]
    public var allSongsBrowseId: String?
    public var allSongsParams: String?

    public init(
        albums: [Album] = [],
        songs: [Track] = [],
        allSongsBrowseId: String? = nil,
        allSongsParams: String? = nil
    ) {
        self.albums = albums
        self.songs = songs
        self.allSongsBrowseId = allSongsBrowseId
        self.allSongsParams = allSongsParams
    }
}
