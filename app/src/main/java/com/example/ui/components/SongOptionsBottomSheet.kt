package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark1
import com.example.ui.theme.SurfaceDark2
import com.example.ui.theme.SurfaceDark3
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleLike: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onViewInfo: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark1,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("song_options_bottom_sheet")
        ) {
            // Header: Album Art, Song Title, and Artist
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
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
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.album}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Items
            SheetActionItem(
                icon = Icons.Default.SkipNext,
                title = "Play Next",
                onClick = {
                    onPlayNext()
                    onDismiss()
                }
            )

            SheetActionItem(
                icon = Icons.Default.Queue,
                title = "Add to Queue",
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )

            SheetActionItem(
                icon = Icons.Default.PlaylistAdd,
                title = "Add to Playlist",
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                }
            )

            SheetActionItem(
                icon = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (song.isLiked) "Remove from Liked Songs" else "Save to Liked Songs",
                iconTint = if (song.isLiked) BrandGreen else TextSecondary,
                onClick = {
                    onToggleLike()
                    onDismiss()
                }
            )

            SheetActionItem(
                icon = Icons.Default.Album,
                title = "Go to Album (${song.album})",
                onClick = {
                    onDismiss()
                    onGoToAlbum()
                }
            )

            SheetActionItem(
                icon = Icons.Default.Mic,
                title = "Go to Artist (${song.artist})",
                onClick = {
                    onDismiss()
                    onGoToArtist()
                }
            )

            SheetActionItem(
                icon = Icons.Default.Info,
                title = "View Track Info",
                onClick = {
                    onDismiss()
                    onViewInfo()
                }
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(4.dp))

            // Delete Action (Red text)
            SheetActionItem(
                icon = Icons.Default.Delete,
                title = "Delete from Library",
                iconTint = ErrorRed,
                textColor = ErrorRed,
                onClick = {
                    onDismiss()
                    onDeleteRequest()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SheetActionItem(
    icon: ImageVector,
    title: String,
    iconTint: Color = TextSecondary,
    textColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDark2,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth().testTag("song_info_dialog")
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Track Information",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                InfoRow(label = "Title", value = song.title)
                InfoRow(label = "Artist", value = song.artist)
                InfoRow(label = "Album", value = song.album)
                InfoRow(label = "Duration", value = song.durationFormatted)
                InfoRow(label = "Folder", value = song.folderName)
                if (song.folderPath.isNotBlank()) {
                    InfoRow(label = "Path", value = song.folderPath)
                }
                InfoRow(label = "Play Count", value = "${song.playCount} times")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = PitchBlack
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = TextPrimary, fontSize = 14.sp)
    }
}

@Composable
fun DeleteConfirmationDialog(
    songTitle: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark2,
        title = {
            Text(text = "Delete Track", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = "Are you sure you want to remove \"$songTitle\" from your library?",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
