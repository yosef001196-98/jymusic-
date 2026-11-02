package com.example.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val uriString: String,
    val albumArtUri: String? = null,
    val folderName: String = "Music",
    val folderPath: String = "",
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

data class SongFolder(
    val name: String,
    val path: String,
    val songs: List<Song>
) {
    val countFormatted: String get() = formatSongCount(songs.size)
}

data class PlaybackSpeedSettings(
    val speed: Float = 1.0f,
    val preservePitch: Boolean = true
)

data class UserPreferences(
    val themeMode: String = "DARK", // DARK, LIGHT, SYSTEM
    val ignoreShortTracks: Boolean = true, // ignore < 30s
    val pauseOnDisconnect: Boolean = true,
    val profileName: String = "JY Listener",
    val profileAvatarUri: String? = null
)

fun formatSongCount(count: Int): String {
    return if (count == 1) "1 song" else "$count songs"
}
