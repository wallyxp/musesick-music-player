import SwiftUI

public final class ArtistImageCache {
    public static let shared = ArtistImageCache()
    private let cache = NSCache<NSString, UIImage>()

    private init() {
        cache.countLimit = 200
        cache.totalCostLimit = 80 * 1024 * 1024 // 80 MB
    }

    public func image(for urlString: String) -> UIImage? {
        guard !urlString.isEmpty else { return nil }
        return cache.object(forKey: urlString as NSString)
    }

    public func insertImage(_ image: UIImage, for urlString: String) {
        guard !urlString.isEmpty else { return }
        cache.setObject(image, forKey: urlString as NSString)
    }
}

public struct ArtistPhotoView: View {
    let artist: Artist
    var isHero: Bool = false

    @State private var displayedImage: UIImage? = nil
    @State private var isFullLoaded: Bool = false
    @State private var isThumbnailLoaded: Bool = false
    @State private var refreshTask: Task<Void, Never>? = nil
    @State private var isInWindowScope: Bool = false
    @State private var resolvedThumbnailUrl: String?
    @State private var resolvedFullImageUrl: String?

    public init(artist: Artist, isHero: Bool = false) {
        self.artist = artist
        self.isHero = isHero
    }

    public var body: some View {
        ZStack {
            if let image = displayedImage {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .transition(.opacity.animation(.easeInOut(duration: 0.25)))
            } else {
                // Gradient with initials placeholder while loading
                placeholderView
            }
        }
        .onAppear {
            isInWindowScope = true
            if let match = Artist.popularArtists.first(where: { $0.name.lowercased() == artist.name.lowercased() }) {
                if artist.thumbnailUrl == nil || artist.thumbnailUrl?.contains("lh3.googleusercontent.com/occfWn") == true || artist.thumbnailUrl?.isEmpty == true {
                    resolvedThumbnailUrl = match.thumbnailUrl
                    resolvedFullImageUrl = match.highResImageUrl ?? match.fullImageUrl
                } else {
                    resolvedThumbnailUrl = artist.thumbnailUrl
                    resolvedFullImageUrl = artist.highResImageUrl ?? artist.fullImageUrl
                }
            } else {
                resolvedThumbnailUrl = artist.thumbnailUrl
                resolvedFullImageUrl = artist.highResImageUrl ?? artist.fullImageUrl
            }
            checkCachedImages()
            startWindowScopeLoadingAndRefreshing()
        }
        .onDisappear {
            isInWindowScope = false
            refreshTask?.cancel()
            refreshTask = nil
        }
    }

    private var placeholderView: some View {
        ZStack {
            artistGradient(for: artist.name)

            Text(artistInitials(for: artist.name))
                .font(.system(size: isHero ? 48 : 22, weight: .bold))
                .foregroundColor(.white.opacity(0.85))
        }
    }

    private func checkCachedImages() {
        let fullUrl = resolvedFullImageUrl ?? artist.highResImageUrl ?? ""
        if let cachedFull = ArtistImageCache.shared.image(for: fullUrl) {
            self.displayedImage = cachedFull
            self.isFullLoaded = true
            self.isThumbnailLoaded = true
            return
        }

        let thumbUrl = resolvedThumbnailUrl ?? artist.thumbnailUrl ?? ""
        if let cachedThumb = ArtistImageCache.shared.image(for: thumbUrl) {
            self.displayedImage = cachedThumb
            self.isThumbnailLoaded = true
        }
    }

    private func startWindowScopeLoadingAndRefreshing() {
        refreshTask?.cancel()
        refreshTask = Task { @MainActor in
            var attempt = 0
            while isInWindowScope && !isFullLoaded && !Task.isCancelled {
                attempt += 1

                // 1. If thumbnail is not loaded, try loading thumbnail first
                if !isThumbnailLoaded {
                    let thumbUrl = resolvedThumbnailUrl ?? artist.thumbnailUrl ?? ""
                    if !thumbUrl.isEmpty, let url = URL(string: thumbUrl) {
                        if let img = await downloadImage(from: url) {
                            ArtistImageCache.shared.insertImage(img, for: thumbUrl)
                            if !isFullLoaded {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    self.displayedImage = img
                                    self.isThumbnailLoaded = true
                                }
                            }
                        }
                    }
                }

                // 2. Load full-size image
                let fullUrl = resolvedFullImageUrl ?? artist.highResImageUrl ?? artist.fullImageUrl ?? ""
                if !fullUrl.isEmpty, let url = URL(string: fullUrl) {
                    if let img = await downloadImage(from: url) {
                        ArtistImageCache.shared.insertImage(img, for: fullUrl)
                        withAnimation(.easeInOut(duration: 0.25)) {
                            self.displayedImage = img
                            self.isFullLoaded = true
                            self.isThumbnailLoaded = true
                        }
                        break // Successfully loaded full-size image, stop refreshing
                    }
                }

                // 3. If URLs were missing or download failed repeatedly, resolve fresh URLs from YouTube Music
                if attempt % 2 == 0 || (resolvedThumbnailUrl == nil && resolvedFullImageUrl == nil) {
                    let fresh = await YouTubeService.shared.fetchArtistImage(artistName: artist.name)
                    if let newThumb = fresh.thumbnailUrl {
                        self.resolvedThumbnailUrl = newThumb
                    }
                    if let newFull = fresh.fullImageUrl {
                        self.resolvedFullImageUrl = newFull
                    }
                }

                // Keep refreshing in the window scope if not loaded
                if !isFullLoaded && isInWindowScope {
                    try? await Task.sleep(nanoseconds: 3_000_000_000) // 3 seconds retry interval
                }
            }
        }
    }

    private func downloadImage(from url: URL) async -> UIImage? {
        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 10
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                return nil
            }
            return UIImage(data: data)
        } catch {
            return nil
        }
    }

    private func artistGradient(for name: String) -> LinearGradient {
        let palettes: [[Color]] = [
            [Color.purple.opacity(0.85), Color.blue.opacity(0.85)],
            [Color.pink.opacity(0.85), Color.orange.opacity(0.85)],
            [Color.teal.opacity(0.85), Color.indigo.opacity(0.85)],
            [Color.red.opacity(0.85), Color.purple.opacity(0.85)],
            [Color.blue.opacity(0.85), Color.cyan.opacity(0.85)],
            [Color.orange.opacity(0.85), Color.red.opacity(0.85)]
        ]
        let index = abs(name.hashValue) % palettes.count
        return LinearGradient(
            colors: palettes[index],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private func artistInitials(for name: String) -> String {
        let parts = name.split(separator: " ").filter { !$0.isEmpty }
        if parts.count >= 2 {
            return "\(parts[0].prefix(1))\(parts[1].prefix(1))".uppercased()
        } else if let first = parts.first {
            return String(first.prefix(2)).uppercased()
        }
        return "♪"
    }
}
