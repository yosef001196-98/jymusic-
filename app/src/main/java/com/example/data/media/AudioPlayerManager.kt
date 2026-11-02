package com.example.data.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.EqualizerSettings
import com.example.data.model.PlaybackSpeedSettings
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val speedSettings: PlaybackSpeedSettings = PlaybackSpeedSettings(),
    val sleepTimerMinutesRemaining: Int? = null,
    val equalizerSettings: EqualizerSettings = EqualizerSettings(),
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1
)

class AudioPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var originalQueue: List<Song> = emptyList()
    private var activeQueue: List<Song> = emptyList()

    var onSongCompletedListener: ((Song) -> Unit)? = null
    var onSongStartedListener: ((Song) -> Unit)? = null

    init {
        initExoPlayer()
        startProgressTracker()
    }

    private fun initExoPlayer() {
        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
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
        })

        // Start Foreground Service so playback is never killed
        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Failed to start PlaybackService", e)
        }

        exoPlayer = player
        initAudioEffects(player.audioSessionId)
    }

    private fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
            applyEqualizerSettings(_playerState.value.equalizerSettings)
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Unable to create Equalizer / BassBoost", e)
        }
    }

    fun playSong(song: Song, playlist: List<Song>? = null) {
        val player = exoPlayer ?: return

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
                .setUri(Uri.parse(song.uriString))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.albumArtUri?.let { Uri.parse(it) })
                        .build()
                )
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            applyPlaybackSpeedSettings(_playerState.value.speedSettings)
            player.play()

            onSongStartedListener?.invoke(song)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error preparing media item", e)
            _playerState.value = _playerState.value.copy(isBuffering = false, isPlaying = false)
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
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
        val player = exoPlayer
        if (player != null && player.currentPosition > 3000) {
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
        val player = exoPlayer ?: return
        try {
            val pitch = if (settings.preservePitch) 1.0f else settings.speed
            player.playbackParameters = PlaybackParameters(settings.speed, pitch)
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Failed to apply playback parameters", e)
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
            Log.w("AudioPlayerManager", "Error adjusting EQ", e)
        }
    }

    fun addToQueue(song: Song) {
        val updatedOriginal = originalQueue + song
        val updatedActive = activeQueue + song
        originalQueue = updatedOriginal
        activeQueue = updatedActive
        _playerState.value = _playerState.value.copy(queue = updatedActive)
    }

    fun playNextInQueue(song: Song) {
        val currentIdx = _playerState.value.queueIndex.coerceAtLeast(0)
        val updatedActive = activeQueue.toMutableList()
        val insertIdx = (currentIdx + 1).coerceAtMost(updatedActive.size)
        updatedActive.add(insertIdx, song)
        activeQueue = updatedActive
        _playerState.value = _playerState.value.copy(queue = updatedActive)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.value = _playerState.value.copy(sleepTimerMinutesRemaining = null)
            return
        }

        _playerState.value = _playerState.value.copy(sleepTimerMinutesRemaining = minutes)
        sleepTimerJob = scope.launch(Dispatchers.Default) {
            var remaining = minutes
            while (remaining > 0 && isActive) {
                delay(60000L)
                remaining--
                _playerState.value = _playerState.value.copy(
                    sleepTimerMinutesRemaining = if (remaining > 0) remaining else null
                )
            }
            if (isActive) {
                exoPlayer?.pause()
            }
        }
    }

    private fun handleTrackCompletion() {
        val current = _playerState.value.currentSong
        if (current != null) {
            onSongCompletedListener?.invoke(current)
        }

        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                exoPlayer?.play()
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
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.isPlaying) {
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

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        equalizer?.release()
        bassBoost?.release()
    }
}
