package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs_meta")
data class SongMetaEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val albumArtUri: String?,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val isLiked: Boolean = false,
    val lyrics: String? = null,
    val isDemo: Boolean = false
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)
