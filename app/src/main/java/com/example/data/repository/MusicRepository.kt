package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.local.SongMetaEntity
import com.example.data.local.UserPreferencesDataStore
import com.example.data.media.AudioScanner
import com.example.data.model.Song
import com.example.data.model.SongFolder
import com.example.data.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaylistUiModel(
    val id: Long,
    val name: String,
    val songCount: Int,
    val firstSongArtUri: String?,
    val albumArts: List<String> = emptyList(),
    val songIds: List<String> = emptyList()
)

class MusicRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val database = AppDatabase.getDatabase(context)
    private val songMetaDao = database.songMetaDao()
    private val playlistDao = database.playlistDao()
    private val scanner = AudioScanner(context)
    val preferencesDataStore = UserPreferencesDataStore(context)

    val userPreferences: StateFlow<UserPreferences> = preferencesDataStore.preferencesFlow
        .stateIn(scope, SharingStarted.Eagerly, UserPreferences())

    private val _rawDeviceSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    val excludedFolders: Flow<List<ExcludedFolderEntity>> = playlistDao.getExcludedFolders()

    // Flow of all songs merging scanner results and Room metadata
    val allSongs: StateFlow<List<Song>> = combine(
        _rawDeviceSongs,
        songMetaDao.getAllSongs()
    ) { scannedList, dbMetaList ->
        val metaMap = dbMetaList.associateBy { it.songId }
        val baseList = if (scannedList.isNotEmpty()) {
            scannedList
        } else {
            scanner.getCuratedShowcaseSongs()
        }

        baseList.map { song ->
            val meta = metaMap[song.id]
            if (meta != null) {
                song.copy(
                    title = meta.title.ifBlank { song.title },
                    artist = meta.artist.ifBlank { song.artist },
                    isLiked = meta.isLiked,
                    playCount = meta.playCount,
                    lastPlayed = meta.lastPlayed,
                    lyrics = meta.lyrics ?: song.lyrics
                )
            } else {
                song
            }
        }.sortedBy { it.title.lowercase() }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    // Group songs into Folders
    val songFolders: StateFlow<List<SongFolder>> = allSongs.combine(_rawDeviceSongs) { songs, _ ->
        songs.groupBy { it.folderName }.map { (folderName, folderSongs) ->
            SongFolder(
                name = folderName,
                path = folderSongs.firstOrNull()?.folderPath ?: "",
                songs = folderSongs
            )
        }.sortedBy { it.name.lowercase() }
    }.stateIn(scope, SharingStarted.Lazily, emptyList())

    val customPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    init {
        scope.launch {
            scanDeviceTracks()
        }
    }

    suspend fun scanDeviceTracks() = withContext(Dispatchers.IO) {
        _isScanning.value = true
        try {
            val minDuration = if (userPreferences.value.ignoreShortTracks) 30000L else 5000L
            val excludedPaths = playlistDao.getExcludedFolders().first().map { it.folderPath }.toSet()
            val deviceTracks = scanner.scanDeviceAudio(minDuration, excludedPaths)
            _rawDeviceSongs.value = deviceTracks

            if (deviceTracks.isNotEmpty()) {
                val entities = deviceTracks.map { song ->
                    SongMetaEntity(
                        songId = song.id,
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        durationMs = song.durationMs,
                        uriString = song.uriString,
                        albumArtUri = song.albumArtUri,
                        folderName = song.folderName,
                        folderPath = song.folderPath,
                        isDemo = false
                    )
                }
                songMetaDao.upsertSongs(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun toggleLike(song: Song) = withContext(Dispatchers.IO) {
        val newLiked = !song.isLiked
        ensureSongEntityExists(song)
        songMetaDao.updateLiked(song.id, newLiked)
    }

    suspend fun recordSongPlayed(song: Song) = withContext(Dispatchers.IO) {
        ensureSongEntityExists(song)
        songMetaDao.incrementPlayCount(song.id, System.currentTimeMillis())
    }

    suspend fun saveLyrics(songId: String, lyrics: String) = withContext(Dispatchers.IO) {
        songMetaDao.updateLyrics(songId, lyrics)
    }

    suspend fun updateSongMetadata(songId: String, newTitle: String, newArtist: String) = withContext(Dispatchers.IO) {
        songMetaDao.updateMetadata(songId, newTitle.trim(), newArtist.trim())
    }

    suspend fun createPlaylist(name: String, initialSongIds: List<String> = emptyList()): Long =
        withContext(Dispatchers.IO) {
            val pl = PlaylistEntity(name = name.trim())
            val plId = playlistDao.insertPlaylist(pl)
            initialSongIds.forEach { sId ->
                playlistDao.insertCrossRef(PlaylistSongCrossRef(playlistId = plId, songId = sId))
            }
            plId
        }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<String>) =
        withContext(Dispatchers.IO) {
            songIds.forEach { sId ->
                playlistDao.insertCrossRef(PlaylistSongCrossRef(playlistId = playlistId, songId = sId))
            }
        }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) =
        withContext(Dispatchers.IO) {
            playlistDao.removeCrossRef(playlistId, songId)
        }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
        playlistDao.clearPlaylistItems(playlistId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { meta ->
                Song(
                    id = meta.songId,
                    title = meta.title,
                    artist = meta.artist,
                    album = meta.album,
                    durationMs = meta.durationMs,
                    uriString = meta.uriString,
                    albumArtUri = meta.albumArtUri,
                    folderName = meta.folderName,
                    folderPath = meta.folderPath,
                    isLiked = meta.isLiked,
                    playCount = meta.playCount,
                    lastPlayed = meta.lastPlayed,
                    lyrics = meta.lyrics,
                    isDemo = meta.isDemo
                )
            }
        }
    }

    fun getPlaylistSongIds(playlistId: Long): Flow<List<String>> {
        return playlistDao.getSongIdsForPlaylist(playlistId)
    }

    suspend fun excludeFolder(path: String, name: String) = withContext(Dispatchers.IO) {
        playlistDao.insertExcludedFolder(ExcludedFolderEntity(folderPath = path, folderName = name))
        scanDeviceTracks()
    }

    suspend fun removeExcludedFolder(path: String) = withContext(Dispatchers.IO) {
        playlistDao.removeExcludedFolder(path)
        scanDeviceTracks()
    }

    suspend fun deleteSong(song: Song) = withContext(Dispatchers.IO) {
        songMetaDao.deleteSong(song.id)
        _rawDeviceSongs.value = _rawDeviceSongs.value.filter { it.id != song.id }
    }

    suspend fun updateProfile(name: String, avatarUri: Uri?) {
        preferencesDataStore.setProfileName(name)
        if (avatarUri != null) {
            preferencesDataStore.saveProfileAvatar(avatarUri)
        }
    }

    suspend fun setThemeMode(mode: String) {
        preferencesDataStore.setThemeMode(mode)
    }

    suspend fun setIgnoreShortTracks(ignore: Boolean) {
        preferencesDataStore.setIgnoreShortTracks(ignore)
        scanDeviceTracks()
    }

    suspend fun setPauseOnDisconnect(pause: Boolean) {
        preferencesDataStore.setPauseOnDisconnect(pause)
    }

    private suspend fun ensureSongEntityExists(song: Song) {
        val existing = songMetaDao.getSongById(song.id)
        if (existing == null) {
            songMetaDao.upsertSong(
                SongMetaEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    durationMs = song.durationMs,
                    uriString = song.uriString,
                    albumArtUri = song.albumArtUri,
                    folderName = song.folderName,
                    folderPath = song.folderPath,
                    isDemo = song.isDemo
                )
            )
        }
    }
}
