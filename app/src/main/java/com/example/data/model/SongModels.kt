package com.example.data.model

import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val uriString: String,
    val albumArtUri: String? = null,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val isLiked: Boolean = false,
    val lyrics: String? = null,
    val isDemo: Boolean = false
) {
    val durationFormatted: String
        get() {
            val totalSecs = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSecs / 60
            val seconds = totalSecs % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class EqualizerSettings(
    val bass: Int = 0,    // -15 to +15 dB
    val mid: Int = 0,     // -15 to +15 dB
    val treble: Int = 0   // -15 to +15 dB
)

data class Playlist(
    val id: Long = 0,
    val name: String,
    val songIds: List<String> = emptyList(),
    val isAuto: Boolean = false,
    val coverUri: String? = null
)
