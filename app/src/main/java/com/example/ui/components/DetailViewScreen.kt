package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.data.model.formatSongCount
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SurfaceDark2
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("detail_view_screen")
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark2)
                    .testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = formatSongCount(songs.size),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (playlistId != null && onDeletePlaylist != null) {
                IconButton(
                    onClick = { onDeletePlaylist(playlistId) },
                    modifier = Modifier.size(42.dp).testTag("delete_playlist_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Playlist",
                        tint = ErrorRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Play All Button
        if (songs.isNotEmpty()) {
            Button(
                onClick = { onPlayAll(songs) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    contentColor = PitchBlack
                ),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .testTag("play_all_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Play All (${songs.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Track List with bottom padding preventing overlap with floating mini player
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs in this collection yet",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongItemRow(
                        song = song,
                        isPlaying = isPlaying && currentSongId == song.id,
                        isCurrent = currentSongId == song.id,
                        isSelecting = isSelecting,
                        isSelected = selectedSongIds.contains(song.id),
                        onClick = { onSongClick(song) },
                        onLongClick = { onSongLongClick(song) },
                        onMenuClick = { onSongMenuClick(song) },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
