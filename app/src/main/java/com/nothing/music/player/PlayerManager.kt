package com.nothing.music.player

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.nothing.music.model.PlaybackState
import com.nothing.music.model.PlaybackStatus
import com.nothing.music.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerManager private constructor(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private var currentIndex = -1

    // Local Audio Player
    private var localPlayer: MediaPlayer? = null

    // YouTube Headless WebView Audio Bridge
    private var webView: WebView? = null
    private var isWebViewReady = false
    private var pendingVideoId: String? = null

    private var progressJob: Job? = null

    init {
        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        mainHandler.post {
            try {
                val wv = WebView(context.applicationContext)
                val settings = wv.settings
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                wv.webChromeClient = WebChromeClient()
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isWebViewReady = true
                        pendingVideoId?.let {
                            playYouTubeVideo(it)
                            pendingVideoId = null
                        }
                    }
                }

                wv.addJavascriptInterface(WebAppInterface(), "AndroidBridge")

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>body { background-color: black; margin: 0; padding: 0; overflow: hidden; }</style>
                    </head>
                    <body>
                        <div id="player"></div>
                        <script>
                            var tag = document.createElement('script');
                            tag.src = "https://www.youtube.com/iframe_api";
                            var firstScriptTag = document.getElementsByTagName('script')[0];
                            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                            var player;
                            function onYouTubeIframeAPIReady() {
                                player = new YT.Player('player', {
                                    height: '1',
                                    width: '1',
                                    playerVars: {
                                        'playsinline': 1,
                                        'controls': 0,
                                        'autoplay': 1,
                                        'rel': 0,
                                        'origin': 'https://music.youtube.com'
                                    },
                                    events: {
                                        'onReady': onPlayerReady,
                                        'onStateChange': onPlayerStateChange,
                                        'onError': onPlayerError
                                    }
                                });
                            }

                            function onPlayerReady(event) {
                                AndroidBridge.onPlayerReady();
                            }

                            function onPlayerStateChange(event) {
                                // YT.PlayerState: -1 (unstarted), 0 (ended), 1 (playing), 2 (paused), 3 (buffering), 5 (video cued)
                                AndroidBridge.onStateChange(event.data);
                            }

                            function onPlayerError(event) {
                                AndroidBridge.onError(event.data);
                            }

                            function playVideo(id) {
                                if (player && player.loadVideoById) {
                                    player.loadVideoById(id);
                                }
                            }

                            function pauseVideo() {
                                if (player && player.pauseVideo) {
                                    player.pauseVideo();
                                }
                            }

                            function resumeVideo() {
                                if (player && player.playVideo) {
                                    player.playVideo();
                                }
                            }

                            function seekTo(sec) {
                                if (player && player.seekTo) {
                                    player.seekTo(sec, true);
                                }
                            }

                            // Polling for progress
                            setInterval(function() {
                                if (player && player.getCurrentTime && player.getDuration) {
                                    var curr = player.getCurrentTime();
                                    var dur = player.getDuration();
                                    if (dur > 0) {
                                        AndroidBridge.onTimeUpdate(curr, dur);
                                    }
                                }
                            }, 500);
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                wv.loadDataWithBaseURL("https://music.youtube.com", html, "text/html", "UTF-8", null)
                webView = wv
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private inner class WebAppInterface {
        @JavascriptInterface
        fun onPlayerReady() {
            isWebViewReady = true
        }

        @JavascriptInterface
        fun onStateChange(state: Int) {
            mainHandler.post {
                val current = _playbackState.value
                when (state) {
                    1 -> { // Playing
                        _playbackState.value = current.copy(status = PlaybackStatus.PLAYING, errorMessage = null)
                        notifyService()
                    }
                    2 -> { // Paused
                        _playbackState.value = current.copy(status = PlaybackStatus.PAUSED)
                        notifyService()
                    }
                    3 -> { // Buffering
                        _playbackState.value = current.copy(status = PlaybackStatus.BUFFERING)
                    }
                    0 -> { // Ended
                        handleTrackCompletion()
                    }
                }
            }
        }

        @JavascriptInterface
        fun onTimeUpdate(currentTimeSeconds: Double, durationSeconds: Double) {
            mainHandler.post {
                val current = _playbackState.value
                if (current.currentTrack?.isLocal == false) {
                    val posMs = (currentTimeSeconds * 1000).toLong()
                    val durMs = (durationSeconds * 1000).toLong()
                    _playbackState.value = current.copy(
                        currentPositionMs = posMs,
                        durationMs = if (durMs > 0) durMs else current.durationMs
                    )
                }
            }
        }

        @JavascriptInterface
        fun onError(errorCode: Int) {
            mainHandler.post {
                _playbackState.value = _playbackState.value.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = "Streaming error (Code $errorCode)"
                )
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        if (newQueue != null) {
            _queue.value = newQueue
            currentIndex = newQueue.indexOfFirst { it.id == track.id }
        } else {
            val q = _queue.value
            val idx = q.indexOfFirst { it.id == track.id }
            if (idx != -1) {
                currentIndex = idx
            } else {
                _queue.value = q + track
                currentIndex = _queue.value.lastIndex
            }
        }

        _playbackState.value = _playbackState.value.copy(
            status = PlaybackStatus.BUFFERING,
            currentTrack = track,
            currentPositionMs = 0L,
            durationMs = track.durationMs,
            errorMessage = null
        )

        stopLocalPlayer()
        pauseYouTube()

        if (track.isLocal) {
            playLocalTrack(track)
        } else {
            playYouTubeVideo(track.id)
        }

        notifyService()
    }

    private fun playLocalTrack(track: Track) {
        stopProgressLoop()
        try {
            val uri = Uri.parse(track.contentUri ?: return)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                setOnPreparedListener { player ->
                    player.start()
                    _playbackState.value = _playbackState.value.copy(
                        status = PlaybackStatus.PLAYING,
                        durationMs = player.duration.toLong(),
                        errorMessage = null
                    )
                    startProgressLoop()
                    notifyService()
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    _playbackState.value = _playbackState.value.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = "Playback error ($what, $extra)"
                    )
                    true
                }
                prepareAsync()
            }
            localPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = _playbackState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = "Failed to load audio: ${e.message}"
            )
        }
    }

    private fun playYouTubeVideo(videoId: String) {
        mainHandler.post {
            if (isWebViewReady && webView != null) {
                webView?.evaluateJavascript("playVideo('$videoId');", null)
            } else {
                pendingVideoId = videoId
            }
        }
    }

    fun togglePlayPause() {
        val state = _playbackState.value
        val track = state.currentTrack ?: return

        if (state.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        val track = _playbackState.value.currentTrack ?: return
        if (track.isLocal) {
            localPlayer?.let {
                if (it.isPlaying) it.pause()
            }
        } else {
            pauseYouTube()
        }
        _playbackState.value = _playbackState.value.copy(status = PlaybackStatus.PAUSED)
        notifyService()
    }

    fun resume() {
        val track = _playbackState.value.currentTrack ?: return
        if (track.isLocal) {
            localPlayer?.let {
                it.start()
                _playbackState.value = _playbackState.value.copy(status = PlaybackStatus.PLAYING)
                startProgressLoop()
            }
        } else {
            mainHandler.post {
                webView?.evaluateJavascript("resumeVideo();", null)
            }
            _playbackState.value = _playbackState.value.copy(status = PlaybackStatus.PLAYING)
        }
        notifyService()
    }

    private fun pauseYouTube() {
        mainHandler.post {
            webView?.evaluateJavascript("pauseVideo();", null)
        }
    }

    fun seekTo(positionMs: Long) {
        val track = _playbackState.value.currentTrack ?: return
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        if (track.isLocal) {
            localPlayer?.seekTo(positionMs.toInt())
        } else {
            val sec = (positionMs / 1000).toInt()
            mainHandler.post {
                webView?.evaluateJavascript("seekTo($sec);", null)
            }
        }
    }

    fun nextTrack() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIndex = if (_playbackState.value.isShuffle) {
            q.indices.filter { it != currentIndex }.randomOrNull() ?: currentIndex
        } else {
            (currentIndex + 1) % q.size
        }

        currentIndex = nextIndex
        playTrack(q[currentIndex])
    }

    fun previousTrack() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_playbackState.value.currentPositionMs > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = if (currentIndex > 0) currentIndex - 1 else q.size - 1
        currentIndex = prevIndex
        playTrack(q[currentIndex])
    }

    fun toggleShuffle() {
        _playbackState.value = _playbackState.value.copy(
            isShuffle = !_playbackState.value.isShuffle
        )
    }

    fun toggleRepeat() {
        _playbackState.value = _playbackState.value.copy(
            isRepeat = !_playbackState.value.isRepeat
        )
    }

    fun playNext(track: Track) {
        val currentQueue = _queue.value.toMutableList()
        if (currentQueue.isEmpty()) {
            playTrack(track)
            return
        }
        val insertIndex = (currentIndex + 1).coerceAtMost(currentQueue.size)
        // If track already exists in queue, remove it first if desired or insert copy
        currentQueue.add(insertIndex, track)
        _queue.value = currentQueue
    }

    fun addToQueue(track: Track) {
        val currentQueue = _queue.value.toMutableList()
        if (currentQueue.isEmpty()) {
            playTrack(track)
            return
        }
        currentQueue.add(track)
        _queue.value = currentQueue
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val list = _queue.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return

        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)

        // Update currentIndex to keep track of currently playing song
        if (currentIndex == fromIndex) {
            currentIndex = toIndex
        } else if (fromIndex < currentIndex && toIndex >= currentIndex) {
            currentIndex--
        } else if (fromIndex > currentIndex && toIndex <= currentIndex) {
            currentIndex++
        }

        _queue.value = list
    }

    fun removeFromQueue(index: Int) {
        val list = _queue.value.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        if (index < currentIndex) {
            currentIndex--
        } else if (index == currentIndex && list.isNotEmpty()) {
            currentIndex = currentIndex.coerceAtMost(list.lastIndex)
            playTrack(list[currentIndex])
        }
        _queue.value = list
    }

    private fun handleTrackCompletion() {
        if (_playbackState.value.isRepeat) {
            seekTo(0)
            resume()
        } else {
            val q = _queue.value
            if (currentIndex < q.size - 1 || _playbackState.value.isShuffle) {
                nextTrack()
            } else {
                _playbackState.value = _playbackState.value.copy(
                    status = PlaybackStatus.PAUSED,
                    currentPositionMs = 0L
                )
                notifyService()
            }
        }
    }

    private fun startProgressLoop() {
        stopProgressLoop()
        progressJob = scope.launch {
            while (isActive) {
                localPlayer?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            _playbackState.value = _playbackState.value.copy(
                                currentPositionMs = mp.currentPosition.toLong(),
                                durationMs = mp.duration.toLong()
                            )
                        }
                    } catch (e: Exception) {
                        // ignore if player is in invalid state
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopLocalPlayer() {
        stopProgressLoop()
        try {
            localPlayer?.stop()
            localPlayer?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            localPlayer = null
        }
    }

    private fun notifyService() {
        MusicService.startOrUpdate(context)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

