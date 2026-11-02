package com.example.data.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.EqualizerSettings
import com.example.data.model.PlaybackSpeedSettings
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private var playbackService: PlaybackService? = null
    private var isBound = false

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    var onSongCompletedListener: ((Song) -> Unit)? = null
    var onSongStartedListener: ((Song) -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlaybackService.LocalBinder) {
                val s = service.getService()
                playbackService = s
                isBound = true
                scope.launch {
                    s.playerState.collect { state ->
                        _playerState.value = state
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isBound = false
        }
    }

    init {
        startAndBindService()
        // Fallback polling for service state synchronization
        scope.launch(Dispatchers.Main) {
            while (isActive) {
                val s = getService()
                if (s != null && !isBound) {
                    _playerState.value = s.playerState.value
                }
                delay(1000L)
            }
        }
    }

    private fun startAndBindService() {
        val intent = Intent(context, PlaybackService::class.java)
        try {
            // Bind service with BIND_AUTO_CREATE without calling startForegroundService prematurely.
            // This prevents Android's ForegroundServiceDidNotStartInTimeException / ANR crash after 60 seconds.
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            isBound = true
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Failed to bind PlaybackService", e)
        }
    }

    private fun getService(): PlaybackService? {
        return playbackService ?: PlaybackService.instance
    }

    fun playSong(song: Song, playlist: List<Song>? = null) {
        val s = getService()
        if (s != null) {
            s.playSong(song, playlist)
            onSongStartedListener?.invoke(song)
        } else {
            startAndBindService()
            scope.launch {
                delay(150)
                getService()?.let {
                    it.playSong(song, playlist)
                    onSongStartedListener?.invoke(song)
                }
            }
        }
    }

    fun togglePlayPause() {
        getService()?.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        getService()?.seekTo(positionMs)
    }

    fun playNext() {
        getService()?.playNext()
    }

    fun playPrevious() {
        getService()?.playPrevious()
    }

    fun toggleShuffle() {
        getService()?.toggleShuffle()
    }

    fun cycleRepeatMode() {
        getService()?.cycleRepeatMode()
    }

    fun setSpeedSettings(speed: Float, preservePitch: Boolean) {
        getService()?.setSpeedSettings(speed, preservePitch)
    }

    fun updateEqualizer(settings: EqualizerSettings) {
        getService()?.updateEqualizer(settings)
    }

    fun addToQueue(song: Song) {
        getService()?.addToQueue(song)
    }

    fun playNextInQueue(song: Song) {
        getService()?.playNextInQueue(song)
    }

    fun setSleepTimer(minutes: Int) {
        getService()?.setSleepTimer(minutes)
    }

    fun release() {
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "Error unbinding service", e)
            }
            isBound = false
        }
        playbackService = null
    }
}
