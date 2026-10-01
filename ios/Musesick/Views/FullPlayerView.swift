import SwiftUI

public struct FullPlayerView: View {
    @ObservedObject var playerManager: PlayerManager
    @Environment(\.dismiss) private var dismiss

    @State private var showLyrics = false
    @State private var showQueue = false
    @State private var sliderValue: Double = -1.0
    @State private var isDraggingSlider = false

    public init(playerManager: PlayerManager) {
        self.playerManager = playerManager
    }

    private var currentTrack: Track? {
        playerManager.state.currentTrack
    }

    public var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Full-bleed Black Background
                Color.black.ignoresSafeArea()

                if let track = currentTrack {
                    // Background Album Art with Crossfade
                    AsyncImage(url: URL(string: track.highResThumbnailUrl ?? track.thumbnailUrl ?? "")) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: geometry.size.width, height: geometry.size.height)
                                .clipped()
                        default:
                            Color(white: 0.08)
                        }
                    }
                    .ignoresSafeArea()

                    // Top Dark Gradient Scrim
                    VStack {
                        LinearGradient(
                            colors: [
                                Color.black.opacity(0.85),
                                Color.black.opacity(0.45),
                                Color.clear
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(height: 220)
                        Spacer()
                    }
                    .frame(width: geometry.size.width)
                    .ignoresSafeArea()

                    // Bottom Dark Gradient Scrim
                    VStack {
                        Spacer()
                        LinearGradient(
                            colors: [
                                Color.clear,
                                Color.black.opacity(0.65),
                                Color.black.opacity(0.92),
                                Color.black
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(height: 440)
                    }
                    .frame(width: geometry.size.width)
                    .ignoresSafeArea()

                    // Main Player UI Column
                    VStack(spacing: 0) {
                        // Top Bar
                        topBar(track: track)

                        Spacer()

                        // Track Title & Artist
                        VStack(spacing: 6) {
                            Text(track.title)
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.white)
                                .lineLimit(2)
                                .multilineTextAlignment(.center)

                            Text(track.artist)
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(.white.opacity(0.8))
                                .lineLimit(1)
                        }
                        .padding(.horizontal, 24)

                        Spacer().frame(height: 28)

                        // Scrubber Slider & Timestamps
                        sliderSection

                        Spacer().frame(height: 22)

                        // Playback Controls Row
                        controlsRow

                        Spacer().frame(height: 26)

                        // Bottom Action Buttons (Lyrics, Queue, Share)
                        bottomActionButtons(track: track)
                            .padding(.bottom, 28)
                    }
                    .frame(width: geometry.size.width, height: geometry.size.height)

                    // Frosted Glass Lyrics Overlay
                    if showLyrics {
                        lyricsOverlay(track: track)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                            .zIndex(20)
                    }

                    // Sliding Queue Sheet
                    if showQueue {
                        queueOverlay
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                            .zIndex(30)
                    }
                } else {
                    Text("No track playing")
                        .foregroundColor(.white)
                }
            }
            .animation(.spring(response: 0.35, dampingFraction: 0.8), value: showLyrics)
            .animation(.spring(response: 0.35, dampingFraction: 0.8), value: showQueue)
        }
    }

    // MARK: - Subviews

    private func topBar(track: Track) -> some View {
        HStack {
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.down")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(Color.black.opacity(0.35))
                    .clipShape(Circle())
            }

            Spacer()

            Text("NOW PLAYING")
                .font(.system(size: 13, weight: .bold))
                .tracking(1.5)
                .foregroundColor(.white)

            Spacer()

            // Format Badge
            Text(track.audioFormat.label)
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Color.white.opacity(0.2))
                .clipShape(Capsule())
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.top, 50)
    }

    private var sliderSection: some View {
        VStack(spacing: 6) {
            let currentFrac = Double(playerManager.state.progressFraction)
            let progress = isDraggingSlider ? sliderValue : max(0.0, min(1.0, currentFrac))

            Slider(
                value: Binding(
                    get: { progress },
                    set: { val in
                        isDraggingSlider = true
                        sliderValue = val
                    }
                ),
                in: 0...1,
                onEditingChanged: { editing in
                    if !editing {
                        let targetMs = Int64(sliderValue * Double(playerManager.state.durationMs))
                        playerManager.seekTo(positionMs: targetMs)
                        isDraggingSlider = false
                        sliderValue = -1.0
                    }
                }
            )
            .tint(.white)

            HStack {
                Text(playerManager.state.formattedPosition)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.white.opacity(0.8))

                Spacer()

                Text(playerManager.state.formattedDuration)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.white.opacity(0.8))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 24)
    }

    private var controlsRow: some View {
        HStack {
            // Shuffle
            Button(action: { playerManager.toggleShuffle() }) {
                Image(systemName: "shuffle")
                    .font(.system(size: 18))
                    .foregroundColor(playerManager.state.isShuffle ? .white : .white.opacity(0.45))
                    .frame(width: 44, height: 44)
            }
            .frame(maxWidth: .infinity)

            // Previous
            Button(action: { playerManager.previousTrack() }) {
                Image(systemName: "backward.fill")
                    .font(.system(size: 20))
                    .foregroundColor(.white)
                    .frame(width: 48, height: 48)
                    .background(Color.white.opacity(0.18))
                    .clipShape(Circle())
            }
            .frame(maxWidth: .infinity)

            // Big Circular Play/Pause
            Button(action: { playerManager.togglePlayPause() }) {
                ZStack {
                    Circle()
                        .fill(Color.white)
                        .frame(width: 68, height: 68)

                    if playerManager.state.isBuffering {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .black))
                            .scaleEffect(1.2)
                    } else {
                        Image(systemName: playerManager.state.isPlaying ? "pause.fill" : "play.fill")
                            .font(.system(size: 26))
                            .foregroundColor(.black)
                            .offset(x: playerManager.state.isPlaying ? 0 : 2)
                    }
                }
            }
            .frame(maxWidth: .infinity)

            // Next
            Button(action: { playerManager.nextTrack() }) {
                Image(systemName: "forward.fill")
                    .font(.system(size: 20))
                    .foregroundColor(.white)
                    .frame(width: 48, height: 48)
                    .background(Color.white.opacity(0.18))
                    .clipShape(Circle())
            }
            .frame(maxWidth: .infinity)

            // Repeat
            Button(action: { playerManager.toggleRepeat() }) {
                Image(systemName: "repeat")
                    .font(.system(size: 18))
                    .foregroundColor(playerManager.state.isRepeat ? .white : .white.opacity(0.45))
                    .frame(width: 44, height: 44)
            }
            .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
    }

    private func bottomActionButtons(track: Track) -> some View {
        HStack(spacing: 12) {
            // Lyrics Button
            Button(action: { showLyrics = true }) {
                HStack(spacing: 6) {
                    Image(systemName: "quote.bubble.fill")
                        .font(.system(size: 14))
                    Text("Lyrics")
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color.white.opacity(0.2))
                .clipShape(Capsule())
            }

            // Queue Button
            Button(action: { showQueue = true }) {
                HStack(spacing: 6) {
                    Image(systemName: "list.bullet")
                        .font(.system(size: 14))
                    Text("Queue (\(playerManager.queue.count))")
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color.white.opacity(0.2))
                .clipShape(Capsule())
            }

            // Share Button (Icon only)
            if let shareUrl = URL(string: track.isLocal ? "https://musesick.app" : track.youtubeMusicUrl) {
                ShareLink(item: shareUrl, message: Text("Listening to \(track.title) by \(track.artist) on Musesick")) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 42, height: 42)
                        .background(Color.white.opacity(0.2))
                        .clipShape(Circle())
                }
            }
        }
    }

    // MARK: - Lyrics Overlay

    private func lyricsOverlay(track: Track) -> some View {
        ZStack {
            // Frosted blurred album cover background
            AsyncImage(url: URL(string: track.highResThumbnailUrl ?? track.thumbnailUrl ?? "")) { phase in
                if let image = phase.image {
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .blur(radius: 36)
                        .scaleEffect(1.2)
                        .ignoresSafeArea()
                } else {
                    Color.black.ignoresSafeArea()
                }
            }

            // Dark frosted glass scrim
            Color.black.opacity(0.62)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                // Header
                HStack {
                    Button(action: { showLyrics = false }) {
                        Image(systemName: "chevron.down")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                    }

                    Spacer()

                    VStack(spacing: 2) {
                        Text("LYRICS")
                            .font(.system(size: 14, weight: .bold))
                            .tracking(2.0)
                            .foregroundColor(.white)

                        Text("\(track.title) • \(track.artist)")
                            .font(.system(size: 11))
                            .foregroundColor(.white.opacity(0.7))
                            .lineLimit(1)
                    }

                    Spacer()

                    Color.clear.frame(width: 44, height: 44)
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)

                // Synced Lyrics ScrollView
                ScrollViewReader { proxy in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            switch playerManager.lyricsState {
                            case .loading:
                                HStack {
                                    Spacer()
                                    ProgressView().tint(.white)
                                    Spacer()
                                }
                                .padding(.top, 40)

                            case .success(let lines):
                                ForEach(lines) { line in
                                    let isCurrent = isLineCurrent(line: line, allLines: lines)
                                    Text(line.text)
                                        .font(.system(size: isCurrent ? 24 : 18, weight: isCurrent ? .bold : .medium))
                                        .foregroundColor(isCurrent ? .white : .white.opacity(0.4))
                                        .scaleEffect(isCurrent ? 1.04 : 1.0, anchor: .leading)
                                        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isCurrent)
                                        .id(line.id)
                                        .onTapGesture {
                                            playerManager.seekTo(positionMs: line.timeMs)
                                        }
                                }

                            case .instrumental:
                                Text("Instrumental track")
                                    .font(.system(size: 18, weight: .medium))
                                    .foregroundColor(.white.opacity(0.6))
                                    .padding(.top, 40)

                            case .notFound(let reason):
                                Text(reason)
                                    .font(.system(size: 16))
                                    .foregroundColor(.white.opacity(0.5))
                                    .padding(.top, 40)

                            case .error(let msg):
                                Text(msg)
                                    .font(.system(size: 16))
                                    .foregroundColor(.white.opacity(0.5))
                                    .padding(.top, 40)

                            case .idle:
                                EmptyView()
                            }
                        }
                        .padding(.horizontal, 28)
                        .padding(.vertical, 20)
                    }
                    .onChange(of: playerManager.state.currentPositionMs) { newPos in
                        if case .success(let lines) = playerManager.lyricsState {
                            if let current = lines.last(where: { $0.timeMs <= newPos }) {
                                withAnimation {
                                    proxy.scrollTo(current.id, anchor: .center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private func isLineCurrent(line: LyricLine, allLines: [LyricLine]) -> Bool {
        guard let idx = allLines.firstIndex(where: { $0.id == line.id }) else { return false }
        let currentPos = playerManager.state.currentPositionMs
        let nextTime = (idx + 1 < allLines.count) ? allLines[idx + 1].timeMs : Int64.max
        return currentPos >= line.timeMs && currentPos < nextTime
    }

    // MARK: - Queue Overlay

    private var queueOverlay: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 12) {
                // Header
                HStack {
                    Button(action: { showQueue = false }) {
                        Image(systemName: "chevron.down")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                    }

                    Spacer()

                    Text("PLAYING QUEUE (\(playerManager.queue.count))")
                        .font(.system(size: 14, weight: .bold))
                        .tracking(1.5)
                        .foregroundColor(.white)

                    Spacer()

                    Color.clear.frame(width: 44, height: 44)
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)

                // Queue List
                List {
                    ForEach(Array(playerManager.queue.enumerated()), id: \.element.id) { index, item in
                        let isCurrent = item.id == currentTrack?.id
                        HStack(spacing: 14) {
                            Text("\(index + 1)")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(isCurrent ? .white : .white.opacity(0.4))
                                .frame(width: 24, alignment: .leading)

                            VStack(alignment: .leading, spacing: 3) {
                                Text(item.title)
                                    .font(.system(size: 15, weight: isCurrent ? .bold : .medium))
                                    .foregroundColor(isCurrent ? .white : .white.opacity(0.9))
                                    .lineLimit(1)

                                Text("\(item.artist) • \(item.audioFormat.label)")
                                    .font(.system(size: 12))
                                    .foregroundColor(.white.opacity(0.55))
                                    .lineLimit(1)
                            }

                            Spacer()

                            Text(item.formattedDuration)
                                .font(.system(size: 12))
                                .foregroundColor(.white.opacity(0.5))
                        }
                        .padding(.vertical, 4)
                        .listRowBackground(isCurrent ? Color(white: 0.18) : Color(white: 0.08))
                        .contentShape(Rectangle())
                        .onTapGesture {
                            playerManager.play(track: item)
                        }
                    }
                    .onDelete { indexSet in
                        for idx in indexSet {
                            playerManager.removeFromQueue(at: idx)
                        }
                    }
                    .onMove { indices, newOffset in
                        for idx in indices {
                            playerManager.reorderQueue(from: idx, to: newOffset)
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
    }
}
