import Foundation

public actor LyricsService {
    public static let shared = LyricsService()

    private let baseUrl = "https://lrclib.net"
    private let userAgent = "Musesick/1.1.5 (https://github.com/wallyxp/musesick-music-player)"

    public func fetchLyrics(trackName: String, artistName: String, durationSeconds: Int64 = 0) async -> LyricsUiState {
        let cleanTitle = cleanTrackTitle(trackName)
        let cleanArtist = cleanArtistName(artistName)

        // 1. Try exact match with duration
        if let result = await requestApiGet(trackName: cleanTitle, artistName: cleanArtist, duration: durationSeconds) {
            return result
        }

        // 2. Try raw track title if different
        if cleanTitle != trackName {
            if let result = await requestApiGet(trackName: trackName, artistName: cleanArtist, duration: durationSeconds) {
                return result
            }
        }

        // 3. Try without duration restriction
        if durationSeconds > 0 {
            if let result = await requestApiGet(trackName: cleanTitle, artistName: cleanArtist, duration: 0) {
                return result
            }
        }

        // 4. Try search endpoint
        if let result = await requestApiSearch(trackName: cleanTitle, artistName: cleanArtist) {
            return result
        }

        return .notFound("Lyrics not available")
    }

    private func requestApiGet(trackName: String, artistName: String, duration: Int64) async -> LyricsUiState? {
        guard let encodedTrack = trackName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let encodedArtist = artistName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
            return nil
        }

        var urlString = "\(baseUrl)/api/get?track_name=\(encodedTrack)&artist_name=\(encodedArtist)"
        if duration > 0 {
            urlString += "&duration=\(duration)"
        }

        guard let url = URL(string: urlString) else { return nil }
        var request = URLRequest(url: url)
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.timeoutInterval = 6.0

        guard let (data, response) = try? await URLSession.shared.data(for: request),
              let httpRes = response as? HTTPURLResponse,
              httpRes.statusCode == 200,
              let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
            return nil
        }

        return parseLyricsJson(json)
    }

    private func requestApiSearch(trackName: String, artistName: String) async -> LyricsUiState? {
        guard let encodedTrack = trackName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let encodedArtist = artistName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "\(baseUrl)/api/search?track_name=\(encodedTrack)&artist_name=\(encodedArtist)") else {
            return nil
        }

        var request = URLRequest(url: url)
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.timeoutInterval = 6.0

        guard let (data, response) = try? await URLSession.shared.data(for: request),
              let httpRes = response as? HTTPURLResponse,
              httpRes.statusCode == 200,
              let array = (try? JSONSerialization.jsonObject(with: data)) as? [[String: Any]],
              let first = array.first else {
            return nil
        }

        return parseLyricsJson(first)
    }

    private func parseLyricsJson(_ json: [String: Any]) -> LyricsUiState {
        if let instrumental = json["instrumental"] as? Bool, instrumental {
            return .instrumental
        }

        if let syncedLyrics = json["syncedLyrics"] as? String, !syncedLyrics.isEmpty {
            let lines = parseLrc(syncedLyrics)
            if !lines.isEmpty {
                return .success(lines)
            }
        }

        if let plainLyrics = json["plainLyrics"] as? String, !plainLyrics.isEmpty {
            let lines = plainLyrics.components(separatedBy: .newlines).map {
                LyricLine(timeMs: 0, text: $0)
            }
            return .success(lines)
        }

        return .notFound("Lyrics not available")
    }

    private func parseLrc(_ lrc: String) -> [LyricLine] {
        var lines: [LyricLine] = []
        let rawLines = lrc.components(separatedBy: .newlines)
        let pattern = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)"
        let regex = try? NSRegularExpression(pattern: pattern, options: [])

        for line in rawLines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            guard let match = regex?.firstMatch(in: trimmed, options: [], range: NSRange(location: 0, length: trimmed.utf16.count)) else {
                continue
            }

            if let minRange = Range(match.range(at: 1), in: trimmed),
               let secRange = Range(match.range(at: 2), in: trimmed),
               let msRange = Range(match.range(at: 3), in: trimmed),
               let textRange = Range(match.range(at: 4), in: trimmed) {
                let minutes = Int64(trimmed[minRange]) ?? 0
                let seconds = Int64(trimmed[secRange]) ?? 0
                let msStr = String(trimmed[msRange])
                let ms = Int64(msStr) ?? 0
                let normalizedMs = msStr.count == 2 ? ms * 10 : ms

                let totalMs = (minutes * 60 * 1000) + (seconds * 1000) + normalizedMs
                let text = String(trimmed[textRange]).trimmingCharacters(in: .whitespaces)

                lines.append(LyricLine(timeMs: totalMs, text: text))
            }
        }

        return lines.sorted { $0.timeMs < $1.timeMs }
    }

    private func cleanTrackTitle(_ title: String) -> String {
        var clean = title
        let patterns = [
            "\\s*\\(Official\\s+(Music\\s+)?Video\\)",
            "\\s*\\[Official\\s+(Music\\s+)?Video\\]",
            "\\s*\\(Lyric\\s+Video\\)",
            "\\s*\\[Lyric\\s+Video\\]",
            "\\s*\\(Audio\\)",
            "\\s*\\[Audio\\]",
            "\\s*\\(Visualizer\\)",
            "\\s*\\(Official\\s+Audio\\)",
            "\\s*\\(Remastered\\s*\\d*\\)",
            "\\s*\\[Remastered\\s*\\d*\\]",
            "\\s*ft\\..*",
            "\\s*feat\\..*"
        ]
        for p in patterns {
            if let regex = try? NSRegularExpression(pattern: p, options: [.caseInsensitive]) {
                clean = regex.stringByReplacingMatches(in: clean, options: [], range: NSRange(location: 0, length: clean.utf16.count), withTemplate: "")
            }
        }
        return clean.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func cleanArtistName(_ artist: String) -> String {
        return artist.replacingOccurrences(of: " - Topic", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
