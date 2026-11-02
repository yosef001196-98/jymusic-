package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs_meta",
    indices = [
        Index("title"),
        Index("artist"),
        Index("album")
    ]
)
data class SongMetaEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val albumArtUri: String?,
    val folderName: String = "Music",
    val folderPath: String = "",
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val isLiked: Boolean = false,
    val lyrics: String? = null,
    val isDemo: Boolean = false
)

@Entity(
    tableName = "playlists",
    indices = [Index("name")]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    indices = [
        Index("playlistId"),
        Index("songId")
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val folderPath: String,
    val folderName: String
)
