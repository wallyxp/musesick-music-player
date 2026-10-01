import Foundation
import AVFoundation
import MediaPlayer
import Combine

@MainActor
public final class PlayerManager: ObservableObject, YouTubePlayerDelegate {
    public static let shared = PlayerManager()

    @Published public var state = PlaybackState()
    @Published public var queue: [Track] = []
    @Published public var lyricsState: LyricsUiState = .idle

    private var currentIndex: Int = 0
    private var avPlayer: AVPlayer?
    private var timeObserverToken: Any?
    private let webEngine = YouTubeWebEngine.shared

    private init() {
        setupAudioSession()
        webEngine.delegate = self
        setupRemoteCommands()
    }

    private func setupAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, policy: .longFormAudio, options: [])
            try session.setActive(true)
            UIApplication.shared.beginReceivingRemoteControlEvents()
            NSLog("[PlayerManager] AVAudioSession configured successfully for .playback (.longFormAudio)")
        } catch {
            NSLog("[PlayerManager] Failed to configure AVAudioSession: %@", error.localizedDescription)
        }
    }

    private func setupRemoteCommands() {
        NowPlayingManager.shared.setupRemoteCommands(
            onPlay: { [weak self] in
                Task { @MainActor in self?.resume() }
            },
            onPause: { [weak self] in
                Task { @MainActor in self?.pause() }
            },
            onTogglePlayPause: { [weak self] in
                Task { @MainActor in self?.togglePlayPause() }
            },
            onNext: { [weak self] in
                Task { @MainActor in self?.nextTrack() }
            },
            onPrevious: { [weak self] in
                Task { @MainActor in self?.previousTrack() }
            },
            onSeek: { [weak self] seconds in
                Task { @MainActor in self?.seekTo(positionMs: Int64(seconds * 1000)) }
            }
        )
    }

    // MARK: - Playback Control

    public func play(track: Track, newQueue: [Track]? = nil) {
        NSLog("[PlayerManager] play(track: '%@', id: '%@', isLocal: %d)", track.title, track.id, track.isLocal ? 1 : 0)
        if let newQ = newQueue {
            self.queue = newQ
            self.currentIndex = newQ.firstIndex(where: { $0.id == track.id }) ?? 0
        } else {
            if let idx = queue.firstIndex(where: { $0.id == track.id }) {
                self.currentIndex = idx
            } else {
                queue.append(track)
                self.currentIndex = queue.count - 1
            }
        }

        state.currentTrack = track
        state.status = .buffering
        state.currentPositionMs = 0
        state.durationMs = track.durationMs
        state.errorMessage = nil

        stopLocalPlayer()
        webEngine.pause()
        setupAudioSession()

        // Fetch synced lyrics
        loadLyrics(for: track)

        if track.isLocal {
            playLocal(track: track)
        } else {
            webEngine.play(videoId: track.id)
        }

        updateNowPlaying()
        NotificationCenter.default.post(name: Notification.Name("MusesickTrackPlayed"), object: track)
    }

    private func playLocal(track: Track) {
        guard let localId = track.localPath,
              let persistentID = UInt64(localId) else {
            state.status = .error
            state.errorMessage = "Could not find local audio file"
            return
        }

        let predicate = MPMediaPropertyPredicate(value: persistentID, forProperty: MPMediaItemPropertyPersistentID)
        let query = MPMediaQuery.songs()
        query.addFilterPredicate(predicate)

        guard let item = query.items?.first, let assetUrl = item.assetURL else {
            state.status = .error
            state.errorMessage = "Asset URL unavailable"
            return
        }

        let playerItem = AVPlayerItem(url: assetUrl)
        let player = AVPlayer(playerItem: playerItem)
        self.avPlayer = player

        // Track duration
        let durationSec = item.playbackDuration
        self.state.durationMs = Int64(durationSec * 1000)

        // Time observer
        let interval = CMTime(seconds: 0.5, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        timeObserverToken = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                let ms = Int64(time.seconds * 1000)
                self.state.currentPositionMs = ms
                self.updateNowPlaying()
            }
        }

        player.play()
        self.state.status = .playing
        updateNowPlaying()
    }

    private func stopLocalPlayer() {
        if let token = timeObserverToken {
            avPlayer?.removeTimeObserver(token)
            timeObserverToken = nil
        }
        avPlayer?.pause()
        avPlayer = nil
    }

    public func togglePlayPause() {
        if state.isPlaying {
            pause()
        } else {
            resume()
        }
    }

    public func pause() {
        guard let track = state.currentTrack else { return }
        if track.isLocal {
            avPlayer?.pause()
        } else {
            webEngine.pause()
        }
        state.status = .paused
        updateNowPlaying()
    }

    public func resume() {
        guard let track = state.currentTrack else { return }
        if track.isLocal {
            avPlayer?.play()
        } else {
            webEngine.resume()
        }
        state.status = .playing
        updateNowPlaying()
    }

    public func seekTo(positionMs: Int64) {
        state.currentPositionMs = positionMs
        let seconds = Double(positionMs) / 1000.0

        if state.currentTrack?.isLocal == true {
            let targetTime = CMTime(seconds: seconds, preferredTimescale: 600)
            avPlayer?.seek(to: targetTime)
        } else {
            webEngine.seek(toSeconds: seconds)
        }
        updateNowPlaying()
    }

    public func nextTrack() {
        guard !queue.isEmpty else { return }
        if state.isShuffle {
            let otherIndices = queue.indices.filter { $0 != currentIndex }
            currentIndex = otherIndices.randomElement() ?? currentIndex
        } else {
            currentIndex = (currentIndex + 1) % queue.count
        }
        play(track: queue[currentIndex])
    }

    public func previousTrack() {
        guard !queue.isEmpty else { return }
        if state.currentPositionMs > 3000 {
            seekTo(positionMs: 0)
            return
        }
        currentIndex = (currentIndex > 0) ? (currentIndex - 1) : (queue.count - 1)
        play(track: queue[currentIndex])
    }

    public func toggleShuffle() {
        state.isShuffle.toggle()
    }

    public func toggleRepeat() {
        state.isRepeat.toggle()
    }

    public func reorderQueue(from: Int, to: Int) {
        guard from >= 0 && from < queue.count, to >= 0 && to < queue.count else { return }
        let currentItem = (currentIndex < queue.count) ? queue[currentIndex] : nil
        let item = queue.remove(at: from)
        queue.insert(item, at: to)
        if let current = currentItem, let newIdx = queue.firstIndex(where: { $0.id == current.id }) {
            currentIndex = newIdx
        }
    }

    public func removeFromQueue(at index: Int) {
        guard index >= 0 && index < queue.count else { return }
        let isCurrent = (index == currentIndex)
        queue.remove(at: index)
        if index < currentIndex {
            currentIndex -= 1
        } else if isCurrent && !queue.isEmpty {
            currentIndex = min(currentIndex, queue.count - 1)
            play(track: queue[currentIndex])
        }
    }

    private func handleTrackEnd() {
        if state.isRepeat {
            seekTo(positionMs: 0)
            resume()
        } else {
            nextTrack()
        }
    }

    // MARK: - Lyrics

    private func loadLyrics(for track: Track) {
        lyricsState = .loading
        Task {
            let result = await LyricsService.shared.fetchLyrics(
                trackName: track.title,
                artistName: track.artist,
                durationSeconds: track.durationMs / 1000
            )
            await MainActor.run {
                self.lyricsState = result
            }
        }
    }

    // MARK: - Now Playing Info

    private func updateNowPlaying() {
        let currentSec = Double(state.currentPositionMs) / 1000.0
        let durSec = Double(state.durationMs) / 1000.0
        NowPlayingManager.shared.updateNowPlaying(
            track: state.currentTrack,
            isPlaying: state.isPlaying,
            currentPositionSeconds: currentSec,
            durationSeconds: durSec
        )
    }

    // MARK: - YouTubePlayerDelegate

    public nonisolated func onPlayerReady() {
        Task { @MainActor in
            self.webEngine.markReady()
        }
    }

    public nonisolated func onStateChange(state: Int) {
        Task { @MainActor in
            switch state {
            case 1: // Playing
                self.state.status = .playing
                self.state.errorMessage = nil
            case 2: // Paused
                self.state.status = .paused
            case 3: // Buffering
                self.state.status = .buffering
            case 0: // Ended
                self.handleTrackEnd()
            default:
                break
            }
            self.updateNowPlaying()
        }
    }

    public nonisolated func onTimeUpdate(currentTimeSeconds: Double, durationSeconds: Double) {
        Task { @MainActor in
            guard self.state.currentTrack?.isLocal == false else { return }
            self.state.currentPositionMs = Int64(currentTimeSeconds * 1000)
            if durationSeconds > 0 {
                self.state.durationMs = Int64(durationSeconds * 1000)
            }
            self.updateNowPlaying()
        }
    }

    public nonisolated func onError(errorCode: Int) {
        Task { @MainActor in
            self.state.status = .error
            self.state.errorMessage = "YouTube player error (\(errorCode))"
        }
    }
}
