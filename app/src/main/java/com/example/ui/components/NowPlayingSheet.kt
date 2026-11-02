package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.media.PlayerState
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenGlow
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SurfaceDark2
import com.example.ui.theme.SurfaceDark3
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    isVisible: Boolean,
    playerState: PlayerState,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onOpenSpeed: () -> Unit,
    onOptionsClick: () -> Unit
) {
    BackHandler(enabled = isVisible) {
        onClose()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(320, easing = FastOutSlowInEasing)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        )
    ) {
        val song = playerState.currentSong ?: return@AnimatedVisibility
        val haptic = LocalHapticFeedback.current

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = if (playerState.isPlaying) 1.025f else 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = AnimRepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        var isDraggingSeek by remember { mutableStateOf(false) }
        var dragProgress by remember { mutableFloatStateOf(0f) }

        val currentProgress = if (playerState.durationMs > 0) {
            (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        val activeProgress = if (isDraggingSeek) dragProgress else currentProgress

        // Drag down gesture tracking
        var dragOffsetY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            dragOffsetY += dragAmount
                            if (dragOffsetY > 65f) {
                                onClose()
                                dragOffsetY = 0f
                            }
                        },
                        onDragEnd = { dragOffsetY = 0f },
                        onDragCancel = { dragOffsetY = 0f }
                    )
                }
                .testTag("now_playing_screen")
        ) {
            // Dynamic blurred background art
            if (!song.albumArtUri.isNullOrEmpty()) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                )
            }

            // Dark ambient gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PitchBlack.copy(alpha = 0.65f),
                                PitchBlack.copy(alpha = 0.85f),
                                PitchBlack.copy(alpha = 0.98f)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(46.dp).testTag("close_now_playing")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM LIBRARY",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = song.album,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier.size(46.dp).testTag("now_playing_options")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // High-res Album Art Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f)
                        .scale(pulseScale)
                        .shadow(28.dp, RoundedCornerShape(22.dp), spotColor = BrandGreen)
                        .clip(RoundedCornerShape(22.dp))
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
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title, Artist, Like, and Playlist Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    ) {
                        Text(
                            text = song.title,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onToggleLike(song)
                            },
                            modifier = Modifier.size(44.dp).testTag("like_button")
                        ) {
                            Icon(
                                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (song.isLiked) BrandGreen else TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = { onAddToPlaylist(song) },
                            modifier = Modifier.size(44.dp).testTag("add_to_playlist_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Ultra-thin Material 3 Progress Bar (2dp) with tiny thumb
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = activeProgress,
                        onValueChange = {
                            isDraggingSeek = true
                            dragProgress = it
                        },
                        onValueChangeFinished = {
                            isDraggingSeek = false
                            val targetMs = (dragProgress * playerState.durationMs).toLong()
                            onSeek(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = BrandGreen,
                            activeTrackColor = BrandGreen,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreen)
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(2.5.dp),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = BrandGreen,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("seek_bar")
                    )

                    val posToFormat = if (isDraggingSeek) {
                        (dragProgress * playerState.durationMs).toLong()
                    } else {
                        playerState.currentPositionMs
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(posToFormat),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatMs(playerState.durationMs),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lark Player Style Speed Badge Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark2)
                        .clickable(onClick = onOpenSpeed)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("speed_chip_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%.2fx".format(playerState.speedSettings.speed),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Playback Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onToggleShuffle()
                        },
                        modifier = Modifier.size(46.dp).testTag("shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.isShuffle) BrandGreen else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(52.dp).testTag("previous_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Circular Play / Pause with Haptic Feedback
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onTogglePlay()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                color = PitchBlack,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = PitchBlack,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Next
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(52.dp).testTag("next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Repeat
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onCycleRepeat()
                        },
                        modifier = Modifier.size(46.dp).testTag("repeat_button")
                    ) {
                        val icon = if (playerState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                        val tint = if (playerState.repeatMode != RepeatMode.OFF) BrandGreen else TextSecondary
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}
