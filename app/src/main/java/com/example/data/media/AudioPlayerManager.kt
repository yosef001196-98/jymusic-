package com.example.data.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.model.EqualizerSettings
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
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutesRemaining: Int? = null,
    val equalizerSettings: EqualizerSettings = EqualizerSettings(),
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1
)

class AudioPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
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
        startProgressTracker()
    }

    private fun initMediaPlayer(): MediaPlayer {
        mediaPlayer?.release()
        releaseAudioEffects()

        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { mp ->
                _playerState.value = _playerState.value.copy(
                    isBuffering = false,
                    durationMs = mp.duration.toLong().coerceAtLeast(0L)
                )
                applyPlaybackSpeed(_playerState.value.playbackSpeed)
                mp.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                _playerState.value.currentSong?.let { song ->
                    onSongStartedListener?.invoke(song)
                }
            }
            setOnCompletionListener {
                handleTrackCompletion()
            }
            setOnErrorListener { _, what, extra ->
                Log.e("AudioPlayerManager", "MediaPlayer error: what=$what extra=$extra")
                _playerState.value = _playerState.value.copy(isPlaying = false, isBuffering = false)
                true
            }
        }

        try {
            initAudioEffects(player.audioSessionId)
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Audio effects init failed", e)
        }

        mediaPlayer = player
        return player
    }

    private fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
            applyEqualizerSettings(_playerState.value.equalizerSettings)
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Unable to create Equalizer / BassBoost", e)
        }
    }

    private fun releaseAudioEffects() {
        try {
            equalizer?.release()
            equalizer = null
            bassBoost?.release()
            bassBoost = null
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Error releasing audio effects", e)
        }
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
            val player = initMediaPlayer()
            val uri = Uri.parse(song.uriString)
            player.setDataSource(context, uri)
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error preparing audio source for ${song.title}", e)
            _playerState.value = _playerState.value.copy(isBuffering = false, isPlaying = false)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
        } else {
            player.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        try {
            val safePos = positionMs.coerceIn(0L, player.duration.toLong().coerceAtLeast(1L))
            player.seekTo(safePos.toInt())
            _playerState.value = _playerState.value.copy(currentPositionMs = safePos)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error seeking", e)
        }
    }

    fun playNext() {
        if (activeQueue.isEmpty()) return
        val nextIdx = (_playerState.value.queueIndex + 1) % activeQueue.size
        playSong(activeQueue[nextIdx])
    }

    fun playPrevious() {
        if (activeQueue.isEmpty()) return
        val player = mediaPlayer
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

    fun setPlaybackSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        val player = mediaPlayer ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = PlaybackParams().apply {
                    this.speed = speed
                    pitch = 1.0f
                }
                player.playbackParams = params
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Cannot set playback params", e)
        }
    }

    fun updateEqualizer(settings: EqualizerSettings) {
        _playerState.value = _playerState.value.copy(equalizerSettings = settings)
        applyEqualizerSettings(settings)
    }

    private fun applyEqualizerSettings(settings: EqualizerSettings) {
        try {
            // Apply BassBoost
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    // Map -15..+15 to 0..1000 strength
                    val strength = ((settings.bass + 15).toFloat() / 30f * 1000f)
                        .toInt().coerceIn(0, 1000).toShort()
                    bb.setStrength(strength)
                }
            }

            // Apply Equalizer bands
            equalizer?.let { eq ->
                val numBands = eq.numberOfBands
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                val range = (maxLevel - minLevel).toFloat()

                // Map -15..+15 to bandLevelRange
                fun toBandLevel(db: Int): Short {
                    val norm = (db + 15f) / 30f
                    return (minLevel + (norm * range)).toInt().toShort()
                }

                if (numBands > 0) {
                    // Low frequencies (Bass)
                    eq.setBandLevel(0.toShort(), toBandLevel(settings.bass))
                }
                if (numBands > 2) {
                    // Mid frequencies
                    val midBand = (numBands / 2).toShort()
                    eq.setBandLevel(midBand, toBandLevel(settings.mid))
                }
                if (numBands > 1) {
                    // High frequencies (Treble)
                    val highBand = (numBands - 1).toShort()
                    eq.setBandLevel(highBand, toBandLevel(settings.treble))
                }
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Error adjusting EQ", e)
        }
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
                delay(60000L) // 1 minute
                remaining--
                _playerState.value = _playerState.value.copy(
                    sleepTimerMinutesRemaining = if (remaining > 0) remaining else null
                )
            }
            if (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                        _playerState.value = _playerState.value.copy(isPlaying = false)
                    }
                }
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
                mediaPlayer?.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
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
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    try {
                        val pos = player.currentPosition.toLong().coerceAtLeast(0L)
                        val dur = player.duration.toLong().coerceAtLeast(0L)
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = pos,
                            durationMs = if (dur > 0) dur else _playerState.value.durationMs
                        )
                    } catch (e: Exception) {
                        // ignored if state in transition
                    }
                }
                delay(400L)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioEffects()
    }
}
