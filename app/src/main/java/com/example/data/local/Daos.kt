package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SongMetaDao {
    @Query("SELECT * FROM songs_meta")
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

    @Query("DELETE FROM songs_meta WHERE songId = :id")
    suspend fun deleteSong(id: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT songId FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removePlaylistItem(playlistId: Long, songId: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)
}
