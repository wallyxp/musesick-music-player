package com.wally.musesick.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.os.SystemClock
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.wally.musesick.MainActivity
import com.wally.musesick.R
import com.wally.musesick.model.PlaybackState
import com.wally.musesick.model.PlaybackStatus
import com.wally.musesick.repository.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playerManager: PlayerManager

    // Cache the last loaded artwork so we don't reload on every progress tick
    private var lastArtworkUrl: String? = null
    private var lastArtworkBitmap: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        playerManager = PlayerManager.getInstance(this)
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "NothingMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { playerManager.resume() }
                override fun onPause() { playerManager.pause() }
                override fun onSkipToNext() { playerManager.nextTrack() }
                override fun onSkipToPrevious() { playerManager.previousTrack() }
                override fun onSeekTo(pos: Long) { playerManager.seekTo(pos) }
                override fun onStop() {
                    playerManager.pause()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }

        scope.launch {
            playerManager.playbackState.collect { state ->
                updateNotification(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY  -> playerManager.resume()
            ACTION_PAUSE -> playerManager.pause()
            ACTION_PREV  -> playerManager.previousTrack()
            ACTION_NEXT  -> playerManager.nextTrack()
            ACTION_STOP  -> {
                playerManager.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE -> updateNotification(playerManager.playbackState.value)
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(state: PlaybackState) {
        val track = state.currentTrack ?: return

        // Upgrade to highest available resolution URL
        val rawUrl = track.thumbnailUrl
        val hdUrl: String? = when {
            rawUrl.isNullOrBlank() -> null
            rawUrl.contains("i.ytimg.com") || rawUrl.contains("img.youtube.com") -> {
                // Replace any quality variant with maxresdefault, falling back to sddefault
                rawUrl.replace(Regex("(default|mqdefault|hqdefault|sddefault|hq720|maxresdefault)\\.jpg"), "maxresdefault.jpg")
            }
            else -> YouTubeRepository.upgradeThumbnailUrl(rawUrl)
        }

        scope.launch {
            val bitmap = loadArtwork(hdUrl, rawUrl)
            buildAndPostNotification(state, bitmap)
        }
    }

    private suspend fun loadArtwork(primaryUrl: String?, fallbackUrl: String?): Bitmap? {
        // Use cache — only reload if the track changed
        val urlToLoad = primaryUrl ?: fallbackUrl ?: return lastArtworkBitmap

        if (urlToLoad == lastArtworkUrl && lastArtworkBitmap != null) {
            return lastArtworkBitmap
        }

        val bitmap = tryLoadBitmap(urlToLoad)
            ?: if (primaryUrl != fallbackUrl) tryLoadBitmap(fallbackUrl) else null

        if (bitmap != null) {
            lastArtworkUrl = urlToLoad
            lastArtworkBitmap = bitmap
        }
        return bitmap ?: lastArtworkBitmap
    }

    private suspend fun tryLoadBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(applicationContext)
                    .data(url)
                    .size(512, 512)         // Notification large icon: 512×512 is plenty, sharp
                    .allowHardware(false)   // Need software bitmap for Notification
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun buildAndPostNotification(state: PlaybackState, artwork: Bitmap?) {
        val track = state.currentTrack ?: return
        val isPlaying = state.isPlaying

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause, "Pause", createActionIntent(ACTION_PAUSE)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play, "Play", createActionIntent(ACTION_PLAY)
            ).build()
        }

        val prevAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_previous, "Previous", createActionIntent(ACTION_PREV)
        ).build()

        val nextAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_next, "Next", createActionIntent(ACTION_NEXT)
        ).build()

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subText = "[ ${track.audioFormat.label} ]"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(subText)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFD71921.toInt())
            .setColorized(true)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(isPlaying)

        // Set album art — large icon shows in the notification expansion and lock screen
        if (artwork != null) {
            builder.setLargeIcon(artwork)
        }

        // Update MediaSession metadata so lock screen / wearables also see art
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, state.durationMs)
        if (artwork != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
        }
        mediaSession.setMetadata(metadataBuilder.build())

        // Update MediaSession playback state — include ACTION_SEEK_TO so Android renders a seekbar
        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                state.currentPositionMs,
                if (isPlaying) 1.0f else 0.0f,
                SystemClock.elapsedRealtime()
            )
        mediaSession.setPlaybackState(playbackStateBuilder.build())

        startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls for Musesick"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            playerManager.stop()
            playerManager.release()
            stopForeground(STOP_FOREGROUND_REMOVE)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
            mediaSession.isActive = false
            mediaSession.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onDestroy() {
        try {
            playerManager.stop()
            mediaSession.isActive = false
            mediaSession.release()
        } catch (e: Exception) {
            // ignore
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "nothing_music_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_PLAY   = "com.wally.musesick.ACTION_PLAY"
        const val ACTION_PAUSE  = "com.wally.musesick.ACTION_PAUSE"
        const val ACTION_PREV   = "com.wally.musesick.ACTION_PREV"
        const val ACTION_NEXT   = "com.wally.musesick.ACTION_NEXT"
        const val ACTION_STOP   = "com.wally.musesick.ACTION_STOP"
        const val ACTION_UPDATE = "com.wally.musesick.ACTION_UPDATE"

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, MusicService::class.java).apply { action = ACTION_UPDATE }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
