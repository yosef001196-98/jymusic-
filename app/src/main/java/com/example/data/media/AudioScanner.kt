package com.example.data.media

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioScanner(private val context: Context) {

    suspend fun scanDeviceAudio(): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Track $id"
                    val artist = it.getString(artistColumn).let { a ->
                        if (a.isNullOrBlank() || a == "<unknown>") "Unknown Artist" else a
                    }
                    val album = it.getString(albumColumn).let { al ->
                        if (al.isNullOrBlank() || al == "<unknown>") "Unknown Album" else al
                    }
                    val duration = it.getLong(durationColumn)
                    val albumId = it.getLong(albumIdColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    songList.add(
                        Song(
                            id = "device_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            uriString = contentUri,
                            albumArtUri = albumArtUri,
                            isDemo = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AudioScanner", "Error querying MediaStore", e)
        }

        songList
    }

    /**
     * Bundled/curated royalty-free showcase tracks so that the user and emulator have
     * playable, beautiful music with high-fidelity artwork and lyrics even before
     * local MP3 files are placed on the device.
     */
    fun getCuratedShowcaseSongs(): List<Song> {
        return listOf(
            Song(
                id = "showcase_1",
                title = "Midnight Horizon",
                artist = "JY Synth Collective",
                album = "Neon Drift Vol. 1",
                durationMs = 186000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                albumArtUri = "https://picsum.photos/seed/synth1/400/400",
                lyrics = "Driving through the neon lights\nEchoes calling in the night\nBasslines pulsing through the floor\nChasing stars and wanting more...",
                isDemo = true
            ),
            Song(
                id = "showcase_2",
                title = "Emerald Euphoria",
                artist = "Aura Wave",
                album = "Green Frequency",
                durationMs = 214000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                albumArtUri = "https://picsum.photos/seed/synth2/400/400",
                lyrics = "Feel the rhythm in the dark\nLighting up a vibrant spark\nGreen horizons in the haze\nLost inside a sonic maze...",
                isDemo = true
            ),
            Song(
                id = "showcase_3",
                title = "Sahara Chillout",
                artist = "Desert Soundscape",
                album = "Nomad Dreams",
                durationMs = 195000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                albumArtUri = "https://picsum.photos/seed/synth3/400/400",
                lyrics = "Golden sands under midnight sky\nCool desert winds blowing by\nOasis waiting in the deep\nMelodies that never sleep...",
                isDemo = true
            ),
            Song(
                id = "showcase_4",
                title = "Cyber Velocity",
                artist = "Pulse Runner",
                album = "Overdrive",
                durationMs = 240000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                albumArtUri = "https://picsum.photos/seed/synth4/400/400",
                lyrics = "Accelerate beyond the grid\nSecret paths the shadows hid\nDigital rain falling fast\nFutures broken from the past...",
                isDemo = true
            ),
            Song(
                id = "showcase_5",
                title = "Lo-Fi Coffee Thoughts",
                artist = "Rainy Beats",
                album = "Late Night Study",
                durationMs = 172000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                albumArtUri = "https://picsum.photos/seed/synth5/400/400",
                lyrics = "Steam rising from warm tea\nNotes flowing gently free\nTurn the page and take a breath\nPeace in every step...",
                isDemo = true
            )
        )
    }
}
