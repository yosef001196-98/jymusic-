package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.data.model.formatSongCount
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SurfaceDark2
import com.example.ui.theme.SurfaceDark3
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DetailView

@Composable
fun DetailViewScreen(
    detailView: DetailView,
    currentSongId: String?,
    isPlaying: Boolean,
    isSelecting: Boolean,
    selectedSongIds: Set<String>,
    onBack: () -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onDeletePlaylist: ((Long) -> Unit)? = null,
    onAddSongs: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val title = when (detailView) {
        is DetailView.PlaylistView -> detailView.title
        is DetailView.ArtistView -> detailView.artist
        is DetailView.AlbumView -> detailView.album
        is DetailView.FolderView -> detailView.folderName
    }

    val songs = when (detailView) {
        is DetailView.PlaylistView -> detailView.songs
        is DetailView.ArtistView -> detailView.songs
        is DetailView.AlbumView -> detailView.songs
        is DetailView.FolderView -> detailView.songs
    }

    val playlistId = (detailView as? DetailView.PlaylistView)?.playlistId
    val isPlaylist = detailView is DetailView.PlaylistView

    val firstArtUri = remember(songs) {
        songs.firstOrNull { !it.albumArtUri.isNullOrEmpty() }?.albumArtUri
    }

    val totalDurationFormatted = remember(songs) {
        val totalMs = songs.sumOf { it.durationMs }
        val minutes = totalMs / 1000 / 60
        if (minutes > 0) "$minutes min" else "${totalMs / 1000} sec"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .testTag("detail_view_screen")
    ) {
        // Vibrant Spotify Gradient Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF134E2E), // Spotify Deep Emerald
                            Color(0xFF0F2D1E),
                            PitchBlack
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Content
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Top Navigation Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (playlistId != null && onDeletePlaylist != null) {
                            IconButton(
                                onClick = { onDeletePlaylist(playlistId) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .testTag("delete_playlist_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Playlist",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Spotify Cover Artwork
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .align(Alignment.CenterHorizontally)
                            .shadow(16.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark3),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!firstArtUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = firstArtUri,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Spotify Style Playlist Placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF282828), Color(0xFF121212))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle / Creator row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPlaylist) "Playlist • JY Music" else "Collection • JY Music",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = " • ${formatSongCount(songs.size)} • $totalDurationFormatted",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Spotify Action Bar: Add + Shuffle + Big Green Play FAB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onAddSongs != null && isPlaylist) {
                            IconButton(
                                onClick = onAddSongs,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDark2)
                                    .testTag("playlist_add_songs_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Songs",
                                    tint = BrandGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        IconButton(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    onPlayAll(songs.shuffled())
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark2)
                                .testTag("playlist_shuffle_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Large Circular Spotify Green Play Button (56dp)
                        if (songs.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = BrandGreen,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable { onPlayAll(songs) }
                                    .testTag("play_all_button")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying && songs.any { it.id == currentSongId }) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play All",
                                        tint = PitchBlack,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Empty State
            if (songs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "This playlist is empty",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add songs to your playlist from your library",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        if (onAddSongs != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onAddSongs,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandGreen,
                                    contentColor = PitchBlack
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.testTag("empty_add_songs_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add Songs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Spotify Tracks List
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    val isCurrent = currentSongId == song.id

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("playlist_track_${song.id}")
                    ) {
                        // Track Number
                        Text(
                            text = "${index + 1}",
                            color = if (isCurrent) BrandGreen else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.width(28.dp)
                        )

                        // Track Art Thumbnail
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDark3),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!song.albumArtUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = if (isCurrent) BrandGreen else TextPrimary,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${song.artist} • ${song.durationFormatted}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 3-Dots Menu Button
                        IconButton(
                            onClick = { onSongMenuClick(song) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
