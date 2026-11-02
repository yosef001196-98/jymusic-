package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SongFolder
import com.example.data.model.formatSongCount
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark2
import com.example.ui.theme.SurfaceDark3
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlaylistCard(
    title: String,
    songCount: Int,
    coverUri: String? = null,
    icon: ImageVector? = null,
    gradientColors: List<Color>? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countText = formatSongCount(songCount)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark2)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("playlist_card_$title")
    ) {
        // Left Art Box showing the first song's album art with Coil
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (gradientColors != null) Brush.linearGradient(gradientColors)
                    else Brush.linearGradient(listOf(SurfaceDark3, SurfaceDark2))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!coverUri.isNullOrEmpty()) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = countText,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NewPlaylistCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark2)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("new_playlist_card")
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(SurfaceDark3),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Playlist",
                tint = BrandGreen,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "New Playlist",
                color = BrandGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Create empty playlist",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun FolderCard(
    folder: SongFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark2)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("folder_card_${folder.name}")
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark3),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = BrandGreen,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = folder.countFormatted,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ArtistCard(
    artistName: String,
    songCount: Int,
    artworkUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(108.dp)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag("artist_card_$artistName")
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SurfaceDark2)
                .border(2.dp, SurfaceBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUri.isNullOrEmpty()) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artistName,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = formatSongCount(songCount),
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AlbumCard(
    albumName: String,
    artistName: String,
    songCount: Int,
    artworkUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(135.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark2)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("album_card_$albumName")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark3),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUri.isNullOrEmpty()) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = albumName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = albumName,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "$artistName • ${formatSongCount(songCount)}",
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
