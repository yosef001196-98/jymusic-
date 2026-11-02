package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SongMetaDao {
    @Query("SELECT * FROM songs_meta ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongMetaEntity>>

    @Query("SELECT * FROM songs_meta WHERE songId = :id LIMIT 1")
    suspend fun getSongById(id: String): SongMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongMetaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSong(song: SongMetaEntity)

    @Query("UPDATE songs_meta SET isLiked = :isLiked WHERE songId = :id")
    suspend fun updateLiked(id: String, isLiked: Boolean)

    @Query("UPDATE songs_meta SET playCount = playCount + 1, lastPlayed = :timestamp WHERE songId = :id")
    suspend fun incrementPlayCount(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs_meta SET lyrics = :lyrics WHERE songId = :id")
    suspend fun updateLyrics(id: String, lyrics: String?)

    @Query("UPDATE songs_meta SET title = :title, artist = :artist WHERE songId = :id")
    suspend fun updateMetadata(id: String, title: String, artist: String)

    @Query("DELETE FROM songs_meta WHERE songId = :id")
    suspend fun deleteSong(id: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeCrossRef(playlistId: Long, songId: String)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)

    // Solves empty playlist bug: fetches all songs belonging to a playlist in insertion order
    @Transaction
    @Query("""
        SELECT songs_meta.* FROM songs_meta
        INNER JOIN playlist_song_cross_ref ON songs_meta.songId = playlist_song_cross_ref.songId
        WHERE playlist_song_cross_ref.playlistId = :playlistId
        ORDER BY playlist_song_cross_ref.addedAt ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongMetaEntity>>

    @Query("SELECT songId FROM playlist_song_cross_ref WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<String>>

    // Excluded Folders for Smart Folders Scanner
    @Query("SELECT * FROM excluded_folders")
    fun getExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcludedFolder(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :path")
    suspend fun removeExcludedFolder(path: String)
}
