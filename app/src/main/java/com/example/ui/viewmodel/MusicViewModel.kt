package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlaylistEntity
import com.example.data.media.AudioPlayerManager
import com.example.data.media.PlayerState
import com.example.data.model.EqualizerSettings
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class DetailView {
    data class PlaylistView(val title: String, val songs: List<Song>, val playlistId: Long? = null) : DetailView()
    data class ArtistView(val artist: String, val songs: List<Song>) : DetailView()
    data class AlbumView(val album: String, val songs: List<Song>) : DetailView()
}

enum class FilterChip(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    PLAYLISTS("Playlists"),
    ARTISTS("Artists"),
    ALBUMS("Albums")
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application, viewModelScope)
    val playerManager = AudioPlayerManager(application, viewModelScope)

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val allSongs: StateFlow<List<Song>> = repository.allSongs
    val isScanning: StateFlow<Boolean> = repository.isScanning

    val profileName: StateFlow<String> = repository.profileName
    val profileAvatarUri: StateFlow<String?> = repository.profileAvatarUri

    private val _selectedChip = MutableStateFlow(FilterChip.ALL)
    val selectedChip: StateFlow<FilterChip> = _selectedChip.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _selectedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongIds: StateFlow<Set<String>> = _selectedSongIds.asStateFlow()

    private val _isSelecting = MutableStateFlow(false)
    val isSelecting: StateFlow<Boolean> = _isSelecting.asStateFlow()

    private val _detailView = MutableStateFlow<DetailView?>(null)
    val detailView: StateFlow<DetailView?> = _detailView.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    // Dialog visibilities
    val showEqualizerDialog = MutableStateFlow(false)
    val showSleepTimerDialog = MutableStateFlow(false)
    val showSpeedDialog = MutableStateFlow(false)
    val showLyricsDialog = MutableStateFlow(false)
    val showNewPlaylistDialog = MutableStateFlow(false)
    val showPlaylistPicker = MutableStateFlow(false)
    val showProfileDialog = MutableStateFlow(false)
    val selectedSongForMenu = MutableStateFlow<Song?>(null)

    // User feedback
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Combined Playlists (Auto + Custom)
    val customPlaylists: StateFlow<List<PlaylistEntity>> = repository.customPlaylists
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        playerManager.onSongStartedListener = { song ->
            viewModelScope.launch {
                repository.recordSongPlayed(song)
            }
        }
    }

    fun setFilterChip(chip: FilterChip) {
        _selectedChip.value = chip
        _detailView.value = null
    }

    fun toggleSearch() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) {
            _searchQuery.value = ""
        }
    }

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun openDetail(view: DetailView) {
        _detailView.value = view
    }

    fun closeDetail() {
        _detailView.value = null
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    // Playback control wrappers
    fun playSong(song: Song, queue: List<Song>? = null) {
        playerManager.playSong(song, queue)
        setNowPlayingExpanded(true)
    }

    fun playAll(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            playerManager.playSong(songs.first(), songs)
            setNowPlayingExpanded(true)
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun playNext() = playerManager.playNext()
    fun playPrevious() = playerManager.playPrevious()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleRepeatMode() = playerManager.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)
    fun setSleepTimer(minutes: Int) = playerManager.setSleepTimer(minutes)
    fun updateEqualizer(settings: EqualizerSettings) = playerManager.updateEqualizer(settings)

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            repository.toggleLike(song)
            val msg = if (!song.isLiked) "Added to Liked Songs" else "Removed from Liked Songs"
            _toastMessage.emit(msg)
        }
    }

    fun saveLyrics(songId: String, lyrics: String) {
        viewModelScope.launch {
            repository.saveLyrics(songId, lyrics)
            _toastMessage.emit("Lyrics Saved")
        }
    }

    // Selection mode
    fun startSelection(songId: String) {
        _isSelecting.value = true
        _selectedSongIds.value = setOf(songId)
    }

    fun toggleSongSelection(songId: String) {
        val current = _selectedSongIds.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
            if (current.isEmpty()) {
                _isSelecting.value = false
            }
        } else {
            current.add(songId)
        }
        _selectedSongIds.value = current
    }

    fun cancelSelection() {
        _isSelecting.value = false
        _selectedSongIds.value = emptySet()
    }

    // Playlist actions
    fun createPlaylist(name: String, songIds: List<String> = emptyList()) {
        viewModelScope.launch {
            val validName = name.trim()
            if (validName.isNotBlank()) {
                repository.createPlaylist(validName, songIds)
                _toastMessage.emit("Playlist \"$validName\" Created")
                cancelSelection()
            }
        }
    }

    fun addSongsToPlaylist(playlistId: Long, playlistName: String, songIds: List<String>) {
        viewModelScope.launch {
            repository.addSongsToPlaylist(playlistId, songIds)
            _toastMessage.emit("Added ${songIds.size} song(s) to $playlistName")
            cancelSelection()
            showPlaylistPicker.value = false
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            _toastMessage.emit("Removed from playlist")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _toastMessage.emit("Playlist deleted")
            closeDetail()
        }
    }

    fun scanDevice() {
        viewModelScope.launch {
            _toastMessage.emit("Scanning device for music...")
            repository.scanDeviceTracks()
            val count = repository.allSongs.value.filter { !it.isDemo }.size
            _toastMessage.emit("Found $count songs on device")
        }
    }

    fun updateProfile(name: String, avatarUri: String?) {
        repository.updateProfile(name, avatarUri)
        viewModelScope.launch {
            _toastMessage.emit("Profile updated")
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
