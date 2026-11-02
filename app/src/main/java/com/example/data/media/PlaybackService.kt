package com.example.data.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.data.model.EqualizerSettings
import com.example.data.model.PlaybackSpeedSettings
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var originalQueue: List<Song> = emptyList()
    private var activeQueue: List<Song> = emptyList()

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    companion object {
        const val CHANNEL_ID = "jy_music_playback_channel"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var instance: PlaybackService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        // 1. Unbreakable ExoPlayer instance living strictly inside the Foreground Service
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // automatic AudioFocus management
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones disconnect
            .setPauseAtEndOfMediaItems(false) // gapless transitions
            .build()

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()

        setupPlayerListeners()
        initAudioEffects(player.audioSessionId)
        startProgressTracker()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? {
        val superBinder = super.onBind(intent)
        return superBinder ?: binder
    }

    private fun setupPlayerListeners() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                updateWidget()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _playerState.value = _playerState.value.copy(isBuffering = true)
                    }
                    Player.STATE_READY -> {
                        val dur = player.duration.coerceAtLeast(0L)
                        _playerState.value = _playerState.value.copy(
                            isBuffering = false,
                            durationMs = if (dur > 0) dur else _playerState.value.durationMs
                        )
                    }
                    Player.STATE_ENDED -> {
                        handleTrackCompletion()
                    }
                    Player.STATE_IDLE -> {
                        _playerState.value = _playerState.value.copy(isBuffering = false)
                    }
                }
            }

            // Error Resilience: Corrupt track error recovery
            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlaybackService", "ExoPlayer error on track: ${error.message}", error)
                val corruptTitle = _playerState.value.currentSong?.title ?: "track"
                serviceScope.launch {
                    _errorEvents.emit("Corrupt audio in \"$corruptTitle\" — skipping to next")
                }
                Toast.makeText(
                    applicationContext,
                    "Cannot play \"$corruptTitle\" (Corrupt or unsupported format)",
                    Toast.LENGTH_SHORT
                ).show()

                // Gracefully skip to next track without crashing
                playNext()
            }
        })
    }

    fun playSong(song: Song, playlist: List<Song>? = null) {
        if (playlist != null && playlist.isNotEmpty()) {
            originalQueue = playlist
            activeQueue = if (_playerState.value.isShuffle) {
                listOf(song) + (playlist.filter { it.id != song.id }.shuffled())
            } else {
                playlist
            }
        } else if (!activeQueue.any { it.id == song.id }) {
            originalQueue = listOf(song)
            activeQueue = listOf(song)
        }

        val index = activeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        _playerState.value = _playerState.value.copy(
            currentSong = song,
            queue = activeQueue,
            queueIndex = index,
            isBuffering = true,
            currentPositionMs = 0L,
            durationMs = song.durationMs
        )

        try {
            val mediaItem = MediaItem.Builder()
                .setUri(song.uriString)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.albumArtUri?.let { android.net.Uri.parse(it) })
                        .build()
                )
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            applyPlaybackSpeedSettings(_playerState.value.speedSettings)
            player.play()
            updateWidget()
        } catch (e: Exception) {
            Log.e("PlaybackService", "Error setting media item", e)
            _playerState.value = _playerState.value.copy(isBuffering = false, isPlaying = false)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, player.duration.coerceAtLeast(1L))
        player.seekTo(safePos)
        _playerState.value = _playerState.value.copy(currentPositionMs = safePos)
    }

    fun playNext() {
        if (activeQueue.isEmpty()) return
        val nextIdx = (_playerState.value.queueIndex + 1) % activeQueue.size
        playSong(activeQueue[nextIdx])
    }

    fun playPrevious() {
        if (activeQueue.isEmpty()) return
        if (player.currentPosition > 3000) {
            seekTo(0)
            return
        }
        val prevIdx = if (_playerState.value.queueIndex - 1 < 0) {
            activeQueue.size - 1
        } else {
            _playerState.value.queueIndex - 1
        }
        playSong(activeQueue[prevIdx])
    }

    fun toggleShuffle() {
        val newShuffle = !_playerState.value.isShuffle
        val currentSong = _playerState.value.currentSong

        activeQueue = if (newShuffle && originalQueue.isNotEmpty()) {
            if (currentSong != null) {
                listOf(currentSong) + (originalQueue.filter { it.id != currentSong.id }.shuffled())
            } else {
                originalQueue.shuffled()
            }
        } else {
            originalQueue
        }

        val newIndex = currentSong?.let { activeQueue.indexOf(it) } ?: 0
        _playerState.value = _playerState.value.copy(
            isShuffle = newShuffle,
            queue = activeQueue,
            queueIndex = newIndex
        )
    }

    fun cycleRepeatMode() {
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playerState.value = _playerState.value.copy(repeatMode = nextMode)
    }

    fun setSpeedSettings(speed: Float, preservePitch: Boolean) {
        val settings = PlaybackSpeedSettings(speed = speed, preservePitch = preservePitch)
        _playerState.value = _playerState.value.copy(speedSettings = settings)
        applyPlaybackSpeedSettings(settings)
    }

    private fun applyPlaybackSpeedSettings(settings: PlaybackSpeedSettings) {
        try {
            val pitch = if (settings.preservePitch) 1.0f else settings.speed
            player.playbackParameters = PlaybackParameters(settings.speed, pitch)
        } catch (e: Exception) {
            Log.w("PlaybackService", "Error applying speed", e)
        }
    }

    private fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
            applyEqualizerSettings(_playerState.value.equalizerSettings)
        } catch (e: Exception) {
            Log.w("PlaybackService", "Audio effects init failed", e)
        }
    }

    fun updateEqualizer(settings: EqualizerSettings) {
        _playerState.value = _playerState.value.copy(equalizerSettings = settings)
        applyEqualizerSettings(settings)
    }

    private fun applyEqualizerSettings(settings: EqualizerSettings) {
        try {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    val strength = ((settings.bass + 15).toFloat() / 30f * 1000f)
                        .toInt().coerceIn(0, 1000).toShort()
                    bb.setStrength(strength)
                }
            }

            virtualizer?.let { v ->
                if (v.strengthSupported) {
                    v.setStrength(500.toShort())
                }
            }

            equalizer?.let { eq ->
                val numBands = eq.numberOfBands
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                val range = (maxLevel - minLevel).toFloat()

                fun toBandLevel(db: Int): Short {
                    val norm = (db + 15f) / 30f
                    return (minLevel + (norm * range)).toInt().toShort()
                }

                if (numBands > 0) eq.setBandLevel(0.toShort(), toBandLevel(settings.bass))
                if (numBands > 2) eq.setBandLevel((numBands / 2).toShort(), toBandLevel(settings.mid))
                if (numBands > 1) eq.setBandLevel((numBands - 1).toShort(), toBandLevel(settings.treble))
            }
        } catch (e: Exception) {
            Log.w("PlaybackService", "Error adjusting EQ", e)
        }
    }

    fun addToQueue(song: Song) {
        originalQueue = originalQueue + song
        activeQueue = activeQueue + song
        _playerState.value = _playerState.value.copy(queue = activeQueue)
    }

    fun playNextInQueue(song: Song) {
        val currentIdx = _playerState.value.queueIndex.coerceAtLeast(0)
        val updated = activeQueue.toMutableList()
        val insertIdx = (currentIdx + 1).coerceAtMost(updated.size)
        updated.add(insertIdx, song)
        activeQueue = updated
        _playerState.value = _playerState.value.copy(queue = updated)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.value = _playerState.value.copy(sleepTimerMinutesRemaining = null)
            return
        }

        _playerState.value = _playerState.value.copy(sleepTimerMinutesRemaining = minutes)
        sleepTimerJob = serviceScope.launch(Dispatchers.Default) {
            var remaining = minutes
            while (remaining > 0 && isActive) {
                delay(60000L)
                remaining--
                _playerState.value = _playerState.value.copy(
                    sleepTimerMinutesRemaining = if (remaining > 0) remaining else null
                )
            }
            if (isActive) {
                player.pause()
            }
        }
    }

    private fun handleTrackCompletion() {
        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                player.play()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (_playerState.value.queueIndex < activeQueue.size - 1) {
                    playNext()
                } else {
                    _playerState.value = _playerState.value.copy(isPlaying = false)
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                if (player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else _playerState.value.durationMs
                    )
                }
                delay(300L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "JY Music Playback"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = "Foreground playback notification channel"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        instance = null
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun updateWidget() {
        try {
            com.example.ui.widget.JYMusicAppWidgetProvider.updateAllWidgets(this)
        } catch (e: Exception) {
            // Ignore widget update errors if receiver not initialized
        }
    }
}
