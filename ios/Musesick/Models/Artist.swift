import Foundation

public struct Artist: Identifiable, Codable, Equatable, Hashable {
    public let id: String
    public var name: String
    public var thumbnailUrl: String?
    public var fullImageUrl: String?
    public var subtitle: String?
    public var browseId: String?

    public init(
        id: String,
        name: String,
        thumbnailUrl: String? = nil,
        fullImageUrl: String? = nil,
        subtitle: String? = "Artist",
        browseId: String? = nil
    ) {
        self.id = id
        self.name = name
        self.thumbnailUrl = thumbnailUrl
        self.fullImageUrl = fullImageUrl
        self.subtitle = subtitle
        self.browseId = browseId
    }

    public var highResImageUrl: String? {
        if let full = fullImageUrl, !full.isEmpty { return full }
        guard let thumb = thumbnailUrl, !thumb.isEmpty else { return nil }
        if thumb.contains("=w120-h120") {
            return thumb.replacingOccurrences(of: "=w120-h120", with: "=w1024-h1024")
        } else if thumb.contains("=w60-h60") {
            return thumb.replacingOccurrences(of: "=w60-h60", with: "=w1024-h1024")
        } else if thumb.contains("=w544-h544") {
            return thumb.replacingOccurrences(of: "=w544-h544", with: "=w1024-h1024")
        } else if thumb.contains("=s120") {
            return thumb.replacingOccurrences(of: "=s120", with: "=s1024")
        }
        return thumb
    }

    public static let popularArtists: [Artist] = [
        Artist(
            id: "UCPC0L1d253x-KuMNwa05TpA",
            name: "Taylor Swift",
            thumbnailUrl: "https://yt3.googleusercontent.com/RCpTA6EXJQyjVFDosWOKa2SMmqkua_lA9mHPDWWciLwgqpZLz-k8rXWRF_367trrQ7up9BUwCbk6kRk=w120-h120-p-l90-rj",
            fullImageUrl: "https://yt3.googleusercontent.com/RCpTA6EXJQyjVFDosWOKa2SMmqkua_lA9mHPDWWciLwgqpZLz-k8rXWRF_367trrQ7up9BUwCbk6kRk=w800-h800-p-l90-rj",
            browseId: "UCPC0L1d253x-KuMNwa05TpA"
        ),
        Artist(
            id: "UClYV6hHlupm_S_ObS1W-DYw",
            name: "The Weeknd",
            thumbnailUrl: "https://lh3.googleusercontent.com/U-SAmNOu4TynE818gLCfKsuHZ0U5YNEtO9mrjSI9WCCKERs98LzrCal5kajBBTQNwdcisoB2Bn-pHp4=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/U-SAmNOu4TynE818gLCfKsuHZ0U5YNEtO9mrjSI9WCCKERs98LzrCal5kajBBTQNwdcisoB2Bn-pHp4=w1024-h1024-p-l90-rj",
            browseId: "UClYV6hHlupm_S_ObS1W-DYw"
        ),
        Artist(
            id: "UCU6cE7pdJPc6DU2jSrKEsdQ",
            name: "Drake",
            thumbnailUrl: "https://yt3.googleusercontent.com/MxNjcRJ-uK4Xvx7u90IhEFLQM8x9LIGTA9VCKHq5U4Wn2jOgiWaMtg-qz329SIzqnCyhdCCB3MpdAGs=w120-h120-p-l90-rj",
            fullImageUrl: "https://yt3.googleusercontent.com/MxNjcRJ-uK4Xvx7u90IhEFLQM8x9LIGTA9VCKHq5U4Wn2jOgiWaMtg-qz329SIzqnCyhdCCB3MpdAGs=w800-h800-p-l90-rj",
            browseId: "UCU6cE7pdJPc6DU2jSrKEsdQ"
        ),
        Artist(
            id: "UCERrDZ8oN0U_n9MphMKERcg",
            name: "Billie Eilish",
            thumbnailUrl: "https://lh3.googleusercontent.com/tQC4rOL6xz6FhmFr0ggQExxyGbYSOsyveXVSnPBh2WjEyIzQ9pMHablLJ-0GlMBrLBlBrbWQGmzrV6KN=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/tQC4rOL6xz6FhmFr0ggQExxyGbYSOsyveXVSnPBh2WjEyIzQ9pMHablLJ-0GlMBrLBlBrbWQGmzrV6KN=w1024-h1024-p-l90-rj",
            browseId: "UCERrDZ8oN0U_n9MphMKERcg"
        ),
        Artist(
            id: "UCDxKh1gFWeYsqePvgVzmPoQ",
            name: "Arijit Singh",
            thumbnailUrl: "https://lh3.googleusercontent.com/W_yOqnKSDYyeVOY_AsXhuAtb6rW3vCL3GtJ9DA1GxWOrJfyeSOqzvTv_TkFHijdkVPXWutASBlRFPg=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/W_yOqnKSDYyeVOY_AsXhuAtb6rW3vCL3GtJ9DA1GxWOrJfyeSOqzvTv_TkFHijdkVPXWutASBlRFPg=w1024-h1024-p-l90-rj",
            browseId: "UCDxKh1gFWeYsqePvgVzmPoQ"
        ),
        Artist(
            id: "UCZn4r7heNOPY-C43YIywnVA",
            name: "Bruno Mars",
            thumbnailUrl: "https://lh3.googleusercontent.com/hnefGBrazRhn4Z92bdSZBUENl40ONjRiVDsmZKZh-WZ2iCKE-2c7KKR7SNcZfzLHoRyB3E6as8L87YA=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/hnefGBrazRhn4Z92bdSZBUENl40ONjRiVDsmZKZh-WZ2iCKE-2c7KKR7SNcZfzLHoRyB3E6as8L87YA=w1024-h1024-p-l90-rj",
            browseId: "UCZn4r7heNOPY-C43YIywnVA"
        ),
        Artist(
            id: "UClmXPfaYhXOYsNn_QUyheWQ",
            name: "Ed Sheeran",
            thumbnailUrl: "https://lh3.googleusercontent.com/jQoBIAS6JjFGpcqQY1M_Mh3AasOvFENCdVRxkgax1a0K6qiq7AgE3MbJ6Jtt-Jndcarvoawmrg66KTny=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/jQoBIAS6JjFGpcqQY1M_Mh3AasOvFENCdVRxkgax1a0K6qiq7AgE3MbJ6Jtt-Jndcarvoawmrg66KTny=w1024-h1024-p-l90-rj",
            browseId: "UClmXPfaYhXOYsNn_QUyheWQ"
        ),
        Artist(
            id: "UCJ2m-WpROlZCiZZID9r7NSQ",
            name: "Diljit Dosanjh",
            thumbnailUrl: "https://yt3.googleusercontent.com/7EYXXMXY594V8y4sZT2aawmdKgDAGTu5jNm9C-HpR3jY9cZJ0NMxS__nZKBdWZ1PUpJPjc2BAA=w120-h120-l90-rj",
            fullImageUrl: "https://yt3.googleusercontent.com/7EYXXMXY594V8y4sZT2aawmdKgDAGTu5jNm9C-HpR3jY9cZJ0NMxS__nZKBdWZ1PUpJPjc2BAA=w800-h800-l90-rj",
            browseId: "UCJ2m-WpROlZCiZZID9r7NSQ"
        ),
        Artist(
            id: "UCzVb0SIXp9q9PeKCcFjsBtA",
            name: "Dua Lipa",
            thumbnailUrl: "https://lh3.googleusercontent.com/aFx8s1fTuelgxONGbezmTG0EKR8r82uB5H-Q6ZJtssyCWLJWF8GfZNr4tHo84sXdFCPBKrA4R6zXOss=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/aFx8s1fTuelgxONGbezmTG0EKR8r82uB5H-Q6ZJtssyCWLJWF8GfZNr4tHo84sXdFCPBKrA4R6zXOss=w1024-h1024-p-l90-rj",
            browseId: "UCzVb0SIXp9q9PeKCcFjsBtA"
        ),
        Artist(
            id: "UCGvj8kfUV5Q6lzECIrGY19g",
            name: "Justin Bieber",
            thumbnailUrl: "https://lh3.googleusercontent.com/4ULlRiFBFglNemZJyKn6_e2-iOIdJEbgBgq_79RQclndG6pge0yGgS2k2On6E1FkCJzenyHkHRzkvjFp=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/4ULlRiFBFglNemZJyKn6_e2-iOIdJEbgBgq_79RQclndG6pge0yGgS2k2On6E1FkCJzenyHkHRzkvjFp=w1024-h1024-p-l90-rj",
            browseId: "UCGvj8kfUV5Q6lzECIrGY19g"
        ),
        Artist(
            id: "UCedvOgsKFzcK3hA5taf3KoQ",
            name: "Eminem",
            thumbnailUrl: "https://lh3.googleusercontent.com/JFI6JZrS-Lco4UdpqDfHY5Wgwy51VXWxmNdI7bCBU5CDlIpN6WWyisZ7MGlpjbrxEGYMFpsqoR_UwcE=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/JFI6JZrS-Lco4UdpqDfHY5Wgwy51VXWxmNdI7bCBU5CDlIpN6WWyisZ7MGlpjbrxEGYMFpsqoR_UwcE=w1024-h1024-p-l90-rj",
            browseId: "UCedvOgsKFzcK3hA5taf3KoQ"
        ),
        Artist(
            id: "UCIaFw5VBEK8qaW6nRpx_qnw",
            name: "Coldplay",
            thumbnailUrl: "https://lh3.googleusercontent.com/IOKuXtp8PCQ_Fc-vaRKm3sKIXBxFV51gZheLTH5br-YGnWHFQf_Jywcuk7wbprYRoEbQyS_XZY6-nMJX=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/IOKuXtp8PCQ_Fc-vaRKm3sKIXBxFV51gZheLTH5br-YGnWHFQf_Jywcuk7wbprYRoEbQyS_XZY6-nMJX=w1024-h1024-p-l90-rj",
            browseId: "UCIaFw5VBEK8qaW6nRpx_qnw"
        ),
        Artist(
            id: "UCprAFmT0C6O4X0ToEXpeFTQ",
            name: "Kendrick Lamar",
            thumbnailUrl: "https://yt3.googleusercontent.com/uB8Magh99SvDyT_mcDYeNYxlVZ_F9WN-cJtAFMHw_Q-_N_8y5-uZiay8-EZSKKloNoWxymBzVehSF4PN=w120-h120-p-l90-rj",
            fullImageUrl: "https://yt3.googleusercontent.com/uB8Magh99SvDyT_mcDYeNYxlVZ_F9WN-cJtAFMHw_Q-_N_8y5-uZiay8-EZSKKloNoWxymBzVehSF4PN=w800-h800-p-l90-rj",
            browseId: "UCprAFmT0C6O4X0ToEXpeFTQ"
        ),
        Artist(
            id: "UCyD3XWRK9ko-izf2nBSFitw",
            name: "Post Malone",
            thumbnailUrl: "https://lh3.googleusercontent.com/48LfK4z6o-CCEWgHQnQfg0ltcT9tbZSN0qjSh0FSJsJI5GF48j2-pH219ciG1ML-PI80ZGD4Vz6sjg=w120-h120-p-l90-rj",
            fullImageUrl: "https://lh3.googleusercontent.com/48LfK4z6o-CCEWgHQnQfg0ltcT9tbZSN0qjSh0FSJsJI5GF48j2-pH219ciG1ML-PI80ZGD4Vz6sjg=w1024-h1024-p-l90-rj",
            browseId: "UCyD3XWRK9ko-izf2nBSFitw"
        ),
        Artist(
            id: "UCq3ab_z4H5v0qWn0R4zX_0g",
            name: "Shreya Ghoshal",
            thumbnailUrl: "https://yt3.googleusercontent.com/PgINZNe0qVxgMSXKG5vF82bNN4WCC12zgWsz9I7OLs4CLF9Cn0Vxq7Xc1ToupnzXrCv0nKfe3VM=w120-h120-l90-rj",
            fullImageUrl: "https://yt3.googleusercontent.com/PgINZNe0qVxgMSXKG5vF82bNN4WCC12zgWsz9I7OLs4CLF9Cn0Vxq7Xc1ToupnzXrCv0nKfe3VM=w800-h800-l90-rj",
            browseId: "UCq3ab_z4H5v0qWn0R4zX_0g"
        ),
        Artist(
            id: "UCE5XNpliPM-SmyFEp61tL_g",
            name: "Olivia Rodrigo",
            thumbnailUrl: "https://yt3.googleusercontent.com/41-4WZupE4yY88igineZefzBZ3ud2nrtlBMv61OBWOfOcATol8PhmI5OZ0fLlrTszyZ3Ul9I9sE=w120-h120-l90-rj-dcqUWI7R0J",
            fullImageUrl: "https://yt3.googleusercontent.com/41-4WZupE4yY88igineZefzBZ3ud2nrtlBMv61OBWOfOcATol8PhmI5OZ0fLlrTszyZ3Ul9I9sE=w800-h800-l90-rj-dcqUWI7R0J",
            browseId: "UCE5XNpliPM-SmyFEp61tL_g"
        ),
        Artist(
            id: "UC0076UMUgEng8HORUw_MYHA",
            name: "Ariana Grande",
            thumbnailUrl: "https://yt3.googleusercontent.com/DU6Kpr5TYKcW6QHvMnsJau5_8QSuix8LCLtf5UEaziZZdXw8SxvcxJ9YWmVIQuzhg2R-MVHYgjdGCQ=w120-h120-p-l90-rj",
            fullImageUrl: "https://yt3.googleusercontent.com/DU6Kpr5TYKcW6QHvMnsJau5_8QSuix8LCLtf5UEaziZZdXw8SxvcxJ9YWmVIQuzhg2R-MVHYgjdGCQ=w800-h800-p-l90-rj",
            browseId: "UC0076UMUgEng8HORUw_MYHA"
        ),
        Artist(
            id: "UCiY3z8HAGD6BlSNKVn2kSvQ",
            name: "Bad Bunny",
            thumbnailUrl: "https://yt3.googleusercontent.com/ploU_4iWpDoJlX3FOlwwd_yQcex0I8A0_665lePXAEBbNp1zn5g42eNwg5Q7lvYc2mG2--UNIYcIhww=w120-h120-p-l90-rj",
            fullImageUrl: "https://yt3.googleusercontent.com/ploU_4iWpDoJlX3FOlwwd_yQcex0I8A0_665lePXAEBbNp1zn5g42eNwg5Q7lvYc2mG2--UNIYcIhww=w800-h800-p-l90-rj",
            browseId: "UCiY3z8HAGD6BlSNKVn2kSvQ"
        )
    ]
}

public struct ArtistDetailData {
    public var albums: [Album]
    public var songs: [Track]
    public var heroImageUrl: String?
    public var allSongsBrowseId: String?
    public var allSongsParams: String?

    public init(
        albums: [Album] = [],
        songs: [Track] = [],
        heroImageUrl: String? = nil,
        allSongsBrowseId: String? = nil,
        allSongsParams: String? = nil
    ) {
        self.albums = albums
        self.songs = songs
        self.heroImageUrl = heroImageUrl
        self.allSongsBrowseId = allSongsBrowseId
        self.allSongsParams = allSongsParams
    }
}
