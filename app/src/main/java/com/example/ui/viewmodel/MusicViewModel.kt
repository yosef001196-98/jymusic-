package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlaylistEntity
import com.example.data.media.AudioPlayerManager
import com.example.data.media.PlayerState
import com.example.data.model.EqualizerSettings
import com.example.data.model.PlaybackSpeedSettings
import com.example.data.model.Song
import com.example.data.model.SongFolder
import com.example.data.model.UserPreferences
import com.example.data.repository.MusicRepository
import com.example.data.repository.PlaylistUiModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class DetailView {
    data class PlaylistView(val title: String, val songs: List<Song>, val playlistId: Long? = null) : DetailView()
    data class ArtistView(val artist: String, val songs: List<Song>) : DetailView()
    data class AlbumView(val album: String, val songs: List<Song>) : DetailView()
    data class FolderView(val folderName: String, val songs: List<Song>) : DetailView()
}

// Folders tab placed at the END as requested in Module 3
enum class MusicTab(val title: String) {
    SONGS("Songs"),
    PLAYLISTS("Playlists"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    FOLDERS("Folders")
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application, viewModelScope)
    val playerManager = AudioPlayerManager(application, viewModelScope)

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val allSongs: StateFlow<List<Song>> = repository.allSongs
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val songFolders: StateFlow<List<SongFolder>> = repository.songFolders
    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences

    private val _selectedTab = MutableStateFlow(MusicTab.SONGS)
    val selectedTab: StateFlow<MusicTab> = _selectedTab.asStateFlow()

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

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // Dialog & Sheet States
    val showEqualizerDialog = MutableStateFlow(false)
    val showSleepTimerDialog = MutableStateFlow(false)
    val showSpeedSheet = MutableStateFlow(false)
    val showLyricsDialog = MutableStateFlow(false)
    val showNewPlaylistDialog = MutableStateFlow(false)
    val showPlaylistPicker = MutableStateFlow(false)
    val showSongInfoDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog = MutableStateFlow(false)
    val showEditTagsDialog = MutableStateFlow(false)

    val songForMenu = MutableStateFlow<Song?>(null)

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val customPlaylists: StateFlow<List<PlaylistEntity>> = repository.customPlaylists
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Enriched Playlists with accurate songCount & 2x2 collage album arts
    val playlistsWithArt: StateFlow<List<PlaylistUiModel>> = combine(
        customPlaylists,
        allSongs
    ) { playlists, songs ->
        val songMap = songs.associateBy { it.id }
        playlists.map { pl ->
            try {
                val songIds = repository.getPlaylistSongIds(pl.id).first()
                val plSongs = songIds.mapNotNull { songMap[it] }
                val albumArts = plSongs.mapNotNull { it.albumArtUri }.distinct().take(4)
                PlaylistUiModel(
                    id = pl.id,
                    name = pl.name,
                    songCount = songIds.size,
                    firstSongArtUri = pl.coverUri ?: albumArts.firstOrNull(),
                    albumArts = albumArts,
                    songIds = songIds
                )
            } catch (e: Exception) {
                PlaylistUiModel(
                    id = pl.id,
                    name = pl.name,
                    songCount = 0,
                    firstSongArtUri = pl.coverUri,
                    albumArts = emptyList(),
                    songIds = emptyList()
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        playerManager.onSongStartedListener = { song ->
            viewModelScope.launch {
                repository.recordSongPlayed(song)
            }
        }
    }

    fun setSelectedTab(tab: MusicTab) {
        _selectedTab.value = tab
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

    fun openPlaylist(playlistId: Long, playlistName: String) {
        viewModelScope.launch {
            val songs = repository.getSongsForPlaylist(playlistId).first()
            _detailView.value = DetailView.PlaylistView(playlistName, songs, playlistId)
        }
    }

    fun closeDetail() {
        _detailView.value = null
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    // Playback operations
    fun playSong(song: Song, queue: List<Song>? = null) {
        playerManager.playSong(song, queue)
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

    fun setSpeedSettings(speed: Float, preservePitch: Boolean) {
        playerManager.setSpeedSettings(speed, preservePitch)
    }

    fun setSleepTimer(minutes: Int) = playerManager.setSleepTimer(minutes)
    fun updateEqualizer(settings: EqualizerSettings) = playerManager.updateEqualizer(settings)

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            repository.toggleLike(song)
            val msg = if (!song.isLiked) "Added to Liked Songs" else "Removed from Liked Songs"
            _toastMessage.emit(msg)
        }
    }

    fun addToQueue(song: Song) {
        playerManager.addToQueue(song)
        viewModelScope.launch {
            _toastMessage.emit("Added to Queue")
        }
    }

    fun playNextInQueue(song: Song) {
        playerManager.playNextInQueue(song)
        viewModelScope.launch {
            _toastMessage.emit("Will play next")
        }
    }

    fun saveLyrics(songId: String, lyrics: String) {
        viewModelScope.launch {
            repository.saveLyrics(songId, lyrics)
            _toastMessage.emit("Lyrics Saved")
        }
    }

    fun updateSongTags(song: Song, newTitle: String, newArtist: String) {
        viewModelScope.launch {
            val validTitle = newTitle.trim().ifBlank { song.title }
            val validArtist = newArtist.trim().ifBlank { song.artist }
            repository.updateSongMetadata(song.id, validTitle, validArtist)
            _toastMessage.emit("Updated tags for \"$validTitle\"")
            showEditTagsDialog.value = false
            songForMenu.value = null
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

    // Playlist actions: Enforce trim() and prohibit empty or "." names
    fun createPlaylist(name: String, songIds: List<String> = emptyList()) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed == ".") {
            viewModelScope.launch {
                _toastMessage.emit("Please enter a valid playlist name")
            }
            return
        }
        viewModelScope.launch {
            repository.createPlaylist(trimmed, songIds)
            _toastMessage.emit("Created playlist \"$trimmed\"")
            cancelSelection()
            showNewPlaylistDialog.value = false
        }
    }

    fun addSongsToPlaylist(playlistId: Long, playlistName: String, songIds: List<String>) {
        viewModelScope.launch {
            repository.addSongsToPlaylist(playlistId, songIds)
            _toastMessage.emit("Added ${songIds.size} songs to $playlistName")
            cancelSelection()
            showPlaylistPicker.value = false
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _toastMessage.emit("Playlist deleted")
            closeDetail()
        }
    }

    fun excludeFolder(folder: SongFolder) {
        viewModelScope.launch {
            repository.excludeFolder(folder.path, folder.name)
            _toastMessage.emit("Excluded \"${folder.name}\" from library")
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            repository.deleteSong(song)
            _toastMessage.emit("Deleted \"${song.title}\"")
            showDeleteConfirmDialog.value = false
            songForMenu.value = null
        }
    }

    fun scanDevice() {
        viewModelScope.launch {
            _toastMessage.emit("Scanning device for music...")
            repository.scanDeviceTracks()
            val count = repository.allSongs.value.filter { !it.isDemo }.size
            _toastMessage.emit("Found $count device tracks")
        }
    }

    fun updateProfile(name: String, avatarUri: Uri?) {
        viewModelScope.launch {
            repository.updateProfile(name, avatarUri)
            _toastMessage.emit("Profile updated & saved")
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setIgnoreShortTracks(ignore: Boolean) {
        viewModelScope.launch {
            repository.setIgnoreShortTracks(ignore)
        }
    }

    fun setPauseOnDisconnect(pause: Boolean) {
        viewModelScope.launch {
            repository.setPauseOnDisconnect(pause)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
