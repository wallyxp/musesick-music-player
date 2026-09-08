package com.wally.musesick.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.wally.musesick.MainActivity
import com.wally.musesick.R
import com.wally.musesick.model.PlaybackState
import com.wally.musesick.model.PlaybackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MusicService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playerManager: PlayerManager

    override fun onCreate() {
        super.onCreate()
        playerManager = PlayerManager.getInstance(this)
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "NothingMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playerManager.resume()
                }

                override fun onPause() {
                    playerManager.pause()
                }

                override fun onSkipToNext() {
                    playerManager.nextTrack()
                }

                override fun onSkipToPrevious() {
                    playerManager.previousTrack()
                }

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
            ACTION_PLAY -> playerManager.resume()
            ACTION_PAUSE -> playerManager.pause()
            ACTION_PREV -> playerManager.previousTrack()
            ACTION_NEXT -> playerManager.nextTrack()
            ACTION_STOP -> {
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

        val isPlaying = state.isPlaying
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                createActionIntent(ACTION_PAUSE)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Play",
                createActionIntent(ACTION_PLAY)
            ).build()
        }

        val prevAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            createActionIntent(ACTION_PREV)
        ).build()

        val nextAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            createActionIntent(ACTION_NEXT)
        ).build()

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subText = "[ ${track.audioFormat.label} ]"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(subText)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFD71921.toInt()) // Iconic Nothing Red
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
            .build()

        // Update MediaSession state
        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                state.currentPositionMs,
                1.0f
            )
        mediaSession.setPlaybackState(playbackStateBuilder.build())

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls for Music"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "nothing_music_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_PLAY = "com.wally.musesick.ACTION_PLAY"
        const val ACTION_PAUSE = "com.wally.musesick.ACTION_PAUSE"
        const val ACTION_PREV = "com.wally.musesick.ACTION_PREV"
        const val ACTION_NEXT = "com.wally.musesick.ACTION_NEXT"
        const val ACTION_STOP = "com.wally.musesick.ACTION_STOP"
        const val ACTION_UPDATE = "com.wally.musesick.ACTION_UPDATE"

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE
            }
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

