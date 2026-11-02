package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PlaylistEntity
import com.example.data.model.Song
import com.example.ui.components.AlbumCard
import com.example.ui.components.ArtistCard
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.DetailViewScreen
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.LyricsDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NewPlaylistCard
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.PlaybackSpeedDialog
import com.example.ui.components.PlaylistCard
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.components.SelectionBottomBar
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SongItemRow
import com.example.ui.components.SongOptionsBottomSheet
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenGlow
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark1
import com.example.ui.theme.SurfaceDark2
import com.example.ui.theme.SurfaceDark3
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DetailView
import com.example.ui.viewmodel.FilterChip
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MusicViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val customPlaylists by viewModel.customPlaylists.collectAsState()

    val profileName by viewModel.profileName.collectAsState()
    val profileAvatarUri by viewModel.profileAvatarUri.collectAsState()

    val selectedChip by viewModel.selectedChip.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchVisible by viewModel.isSearchVisible.collectAsState()

    val selectedSongIds by viewModel.selectedSongIds.collectAsState()
    val isSelecting by viewModel.isSelecting.collectAsState()
    val detailView by viewModel.detailView.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()

    val showEqualizer by viewModel.showEqualizerDialog.collectAsState()
    val showSleepTimer by viewModel.showSleepTimerDialog.collectAsState()
    val showSpeed by viewModel.showSpeedDialog.collectAsState()
    val showLyrics by viewModel.showLyricsDialog.collectAsState()
    val showNewPlaylist by viewModel.showNewPlaylistDialog.collectAsState()
    val showPlaylistPicker by viewModel.showPlaylistPicker.collectAsState()
    val showProfile by viewModel.showProfileDialog.collectAsState()
    val songForMenu by viewModel.selectedSongForMenu.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Permission launcher for scanning device audio
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.any { it }
        if (granted) {
            viewModel.scanDevice()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Filtered songs for search
    val filteredSongs = remember(allSongs, searchQuery) {
        if (searchQuery.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    // Auto playlists
    val likedSongs = remember(allSongs) { allSongs.filter { it.isLiked } }
    val recentlyPlayedSongs = remember(allSongs) {
        allSongs.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(25)
    }
    val mostPlayedSongs = remember(allSongs) {
        allSongs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(25)
    }

    // Artists list
    val artists = remember(allSongs) {
        allSongs.groupBy { it.artist }.filterKeys { it.isNotBlank() && it != "Unknown Artist" }
    }

    // Albums list
    val albums = remember(allSongs) {
        allSongs.groupBy { it.album }.filterKeys { it.isNotBlank() && it != "Unknown Album" }
    }

    fun requestScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        // Subtle top green glow matching original app CSS:
        // radial-gradient(circle at 50% 0%, rgba(30, 215, 96, 0.15) 0%, rgba(0,0,0,0) 70%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandGreenGlow, Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(500f, 0f),
                        radius = 800f
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Top App Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title or Icons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "JY Music",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        // Right action buttons: Scan, Equalizer, Search, Profile
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Scan Device Music
                            IconButton(
                                onClick = { requestScanPermission() },
                                modifier = Modifier.size(42.dp).testTag("scan_device_button")
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        color = BrandGreen,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Scan Device Music",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Equalizer Quick Button
                            IconButton(
                                onClick = { viewModel.showEqualizerDialog.value = true },
                                modifier = Modifier.size(42.dp).testTag("equalizer_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Equalizer",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Search Toggle
                            IconButton(
                                onClick = { viewModel.toggleSearch() },
                                modifier = Modifier.size(42.dp).testTag("search_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isSearchVisible) BrandGreen else TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreen)
                                    .clickable { viewModel.showProfileDialog.value = true }
                                    .testTag("profile_avatar_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!profileAvatarUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = profileAvatarUri,
                                        contentDescription = "Profile Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(36.dp)
                                    )
                                } else {
                                    Text(
                                        text = (profileName.firstOrNull() ?: 'J').uppercase(),
                                        color = PitchBlack,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Search Input Bar
                    AnimatedVisibility(visible = isSearchVisible) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search songs, artists, albums...", color = TextSecondary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("search_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedContainerColor = SurfaceDark2,
                                unfocusedContainerColor = SurfaceDark2
                            )
                        )
                    }

                    // Filter Chips Row
                    if (detailView == null && !isSearchVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip.values().forEach { chip ->
                                val isSelected = selectedChip == chip
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) BrandGreen else SurfaceDark2)
                                        .clickable { viewModel.setFilterChip(chip) }
                                        .padding(horizontal = 16.dp, vertical = 7.dp)
                                        .testTag("filter_chip_${chip.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chip.label,
                                        color = if (isSelected) PitchBlack else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Floating Multi-Selection Bar
                    SelectionBottomBar(
                        selectedCount = selectedSongIds.size,
                        onAddToPlaylist = { viewModel.showPlaylistPicker.value = true },
                        onCancel = { viewModel.cancelSelection() }
                    )

                    // Persistent Mini Player
                    if (playerState.currentSong != null && !isNowPlayingExpanded) {
                        MiniPlayer(
                            playerState = playerState,
                            onExpand = { viewModel.setNowPlayingExpanded(true) },
                            onTogglePlay = { viewModel.togglePlayPause() }
                        )
                    }

                    // Navigation Bar
                    NavigationBar(
                        containerColor = PitchBlack,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(56.dp)
                    ) {
                        NavigationBarItem(
                            selected = detailView == null && selectedChip == FilterChip.ALL && !isSearchVisible,
                            onClick = {
                                viewModel.setFilterChip(FilterChip.ALL)
                                if (isSearchVisible) viewModel.toggleSearch()
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PitchBlack,
                                selectedTextColor = BrandGreen,
                                indicatorColor = BrandGreen,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )

                        NavigationBarItem(
                            selected = detailView == null && selectedChip == FilterChip.PLAYLISTS,
                            onClick = { viewModel.setFilterChip(FilterChip.PLAYLISTS) },
                            icon = { Icon(Icons.Default.QueueMusic, contentDescription = "Playlists") },
                            label = { Text("Playlists", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PitchBlack,
                                selectedTextColor = BrandGreen,
                                indicatorColor = BrandGreen,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )

                        NavigationBarItem(
                            selected = isSearchVisible,
                            onClick = {
                                if (!isSearchVisible) viewModel.toggleSearch()
                            },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PitchBlack,
                                selectedTextColor = BrandGreen,
                                indicatorColor = BrandGreen,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // If detail screen is open (e.g. Playlist or Artist tracks)
                if (detailView != null) {
                    DetailViewScreen(
                        detailView = detailView!!,
                        currentSongId = playerState.currentSong?.id,
                        isPlaying = playerState.isPlaying,
                        isSelecting = isSelecting,
                        selectedSongIds = selectedSongIds,
                        onBack = { viewModel.closeDetail() },
                        onPlayAll = { viewModel.playAll(it) },
                        onSongClick = { song ->
                            if (isSelecting) viewModel.toggleSongSelection(song.id)
                            else viewModel.playSong(song, (detailView as? DetailView.PlaylistView)?.songs)
                        },
                        onSongLongClick = { song -> viewModel.startSelection(song.id) },
                        onSongMenuClick = { song -> viewModel.selectedSongForMenu.value = song },
                        onDeletePlaylist = { pId -> viewModel.deletePlaylist(pId) }
                    )
                } else if (searchQuery.isNotBlank()) {
                    // Search Results List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        item {
                            Text(
                                text = "Search Results (${filteredSongs.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                        if (filteredSongs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No matching songs found", color = TextSecondary)
                                }
                            }
                        } else {
                            items(filteredSongs, key = { it.id }) { song ->
                                SongItemRow(
                                    song = song,
                                    isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                                    isCurrent = playerState.currentSong?.id == song.id,
                                    isSelecting = isSelecting,
                                    isSelected = selectedSongIds.contains(song.id),
                                    onClick = {
                                        if (isSelecting) viewModel.toggleSongSelection(song.id)
                                        else viewModel.playSong(song, filteredSongs)
                                    },
                                    onLongClick = { viewModel.startSelection(song.id) },
                                    onMenuClick = { viewModel.selectedSongForMenu.value = song }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(130.dp)) }
                    }
                } else {
                    // Standard Home / Tab Views
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Scan notice banner if no device songs
                        val deviceSongCount = allSongs.count { !it.isDemo }
                        if (deviceSongCount == 0) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceDark2,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = BrandGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Playing High-Fidelity Showcase",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Tap Scan to load audio files directly from your device.",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Button(
                                            onClick = { requestScanPermission() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = BrandGreen,
                                                contentColor = PitchBlack
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Scan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Playlists Grid Section
                        if (selectedChip == FilterChip.ALL || selectedChip == FilterChip.PLAYLISTS) {
                            item {
                                Text(
                                    text = "Playlists",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                                )
                            }

                            // 2-column grid row
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Row 1: Liked Songs & Most Played
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PlaylistCard(
                                            title = "Liked Songs",
                                            subtitle = "${likedSongs.size} songs",
                                            icon = Icons.Default.Favorite,
                                            gradientColors = listOf(Color(0xFF450af5), Color(0xFFc4efd9)),
                                            onClick = {
                                                viewModel.openDetail(
                                                    DetailView.PlaylistView("Liked Songs", likedSongs)
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        PlaylistCard(
                                            title = "Most Played",
                                            subtitle = "${mostPlayedSongs.size} songs",
                                            icon = Icons.Default.TrendingUp,
                                            gradientColors = listOf(Color(0xFFff416c), Color(0xFFff4b2b)),
                                            onClick = {
                                                viewModel.openDetail(
                                                    DetailView.PlaylistView("Most Played", mostPlayedSongs)
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Row 2: Recently Played & + Create Playlist
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PlaylistCard(
                                            title = "Recently Played",
                                            subtitle = "${recentlyPlayedSongs.size} songs",
                                            icon = Icons.Default.History,
                                            gradientColors = listOf(Color(0xFF11998e), Color(0xFF38ef7d)),
                                            onClick = {
                                                viewModel.openDetail(
                                                    DetailView.PlaylistView("Recently Played", recentlyPlayedSongs)
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        NewPlaylistCard(
                                            onClick = { viewModel.showNewPlaylistDialog.value = true },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Custom user playlists
                                    customPlaylists.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pair.forEach { pl ->
                                                PlaylistCard(
                                                    title = pl.name,
                                                    subtitle = "Custom Playlist",
                                                    icon = Icons.Default.QueueMusic,
                                                    onClick = {
                                                        viewModel.openDetail(
                                                            DetailView.PlaylistView(pl.name, allSongs, pl.id)
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (pair.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Artists Row Section
                        if (selectedChip == FilterChip.ALL || selectedChip == FilterChip.ARTISTS) {
                            if (artists.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Artists",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(top = 18.dp, bottom = 12.dp)
                                    )
                                }

                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        items(artists.entries.toList(), key = { it.key }) { (artistName, artistSongs) ->
                                            ArtistCard(
                                                artistName = artistName,
                                                songCount = artistSongs.size,
                                                artworkUri = artistSongs.firstOrNull()?.albumArtUri,
                                                onClick = {
                                                    viewModel.openDetail(
                                                        DetailView.ArtistView(artistName, artistSongs)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Albums Row Section
                        if (selectedChip == FilterChip.ALL || selectedChip == FilterChip.ALBUMS) {
                            if (albums.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Albums",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(top = 18.dp, bottom = 12.dp)
                                    )
                                }

                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        items(albums.entries.toList(), key = { it.key }) { (albumName, albumSongs) ->
                                            AlbumCard(
                                                albumName = albumName,
                                                artistName = albumSongs.firstOrNull()?.artist ?: "",
                                                artworkUri = albumSongs.firstOrNull()?.albumArtUri,
                                                onClick = {
                                                    viewModel.openDetail(
                                                        DetailView.AlbumView(albumName, albumSongs)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // All Songs List Section
                        if (selectedChip == FilterChip.ALL || selectedChip == FilterChip.SONGS) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 18.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "All Songs",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${allSongs.size} tracks",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            items(allSongs, key = { it.id }) { song ->
                                SongItemRow(
                                    song = song,
                                    isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                                    isCurrent = playerState.currentSong?.id == song.id,
                                    isSelecting = isSelecting,
                                    isSelected = selectedSongIds.contains(song.id),
                                    onClick = {
                                        if (isSelecting) viewModel.toggleSongSelection(song.id)
                                        else viewModel.playSong(song, allSongs)
                                    },
                                    onLongClick = { viewModel.startSelection(song.id) },
                                    onMenuClick = { viewModel.selectedSongForMenu.value = song }
                                )
                            }
                        }

                        // Bottom Spacer for mini player and navigation
                        item {
                            Spacer(modifier = Modifier.height(130.dp))
                        }
                    }
                }
            }
        }

        // Full Screen Now Playing Sheet
        NowPlayingSheet(
            isVisible = isNowPlayingExpanded,
            playerState = playerState,
            onClose = { viewModel.setNowPlayingExpanded(false) },
            onTogglePlay = { viewModel.togglePlayPause() },
            onNext = { viewModel.playNext() },
            onPrevious = { viewModel.playPrevious() },
            onSeek = { viewModel.seekTo(it) },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onCycleRepeat = { viewModel.cycleRepeatMode() },
            onToggleLike = { viewModel.toggleLike(it) },
            onAddToPlaylist = { song ->
                viewModel.selectedSongForMenu.value = song
                viewModel.showPlaylistPicker.value = true
            },
            onOptionsClick = {
                // Show playback options sheet/dialog
                viewModel.showSpeedDialog.value = true
            }
        )

        // Song Options Bottom Sheet
        if (songForMenu != null) {
            SongOptionsBottomSheet(
                song = songForMenu!!,
                onDismiss = { viewModel.selectedSongForMenu.value = null },
                onAddToPlaylist = {
                    viewModel.showPlaylistPicker.value = true
                },
                onToggleLike = {
                    viewModel.toggleLike(songForMenu!!)
                },
                onOpenLyrics = {
                    viewModel.showLyricsDialog.value = true
                }
            )
        }

        // Equalizer Dialog
        if (showEqualizer) {
            EqualizerDialog(
                initialSettings = playerState.equalizerSettings,
                onDismiss = { viewModel.showEqualizerDialog.value = false },
                onSave = { viewModel.updateEqualizer(it) }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimer) {
            SleepTimerDialog(
                currentMinutesRemaining = playerState.sleepTimerMinutesRemaining,
                onDismiss = { viewModel.showSleepTimerDialog.value = false },
                onSetSleep = { viewModel.setSleepTimer(it) }
            )
        }

        // Playback Speed Dialog
        if (showSpeed) {
            PlaybackSpeedDialog(
                currentSpeed = playerState.playbackSpeed,
                onDismiss = { viewModel.showSpeedDialog.value = false },
                onSpeedSelected = { viewModel.setPlaybackSpeed(it) }
            )
        }

        // Lyrics Dialog
        if (showLyrics && songForMenu != null) {
            LyricsDialog(
                songTitle = songForMenu!!.title,
                initialLyrics = songForMenu!!.lyrics,
                onDismiss = { viewModel.showLyricsDialog.value = false },
                onSave = { lyrics -> viewModel.saveLyrics(songForMenu!!.id, lyrics) }
            )
        } else if (showLyrics && playerState.currentSong != null) {
            LyricsDialog(
                songTitle = playerState.currentSong!!.title,
                initialLyrics = playerState.currentSong!!.lyrics,
                onDismiss = { viewModel.showLyricsDialog.value = false },
                onSave = { lyrics -> viewModel.saveLyrics(playerState.currentSong!!.id, lyrics) }
            )
        }

        // Create Playlist Dialog
        if (showNewPlaylist) {
            CreatePlaylistDialog(
                onDismiss = { viewModel.showNewPlaylistDialog.value = false },
                onConfirm = { name ->
                    val initialIds = if (isSelecting) selectedSongIds.toList()
                    else songForMenu?.id?.let { listOf(it) } ?: emptyList()
                    viewModel.createPlaylist(name, initialIds)
                }
            )
        }

        // Playlist Picker Sheet
        if (showPlaylistPicker) {
            val targetIds = if (isSelecting) selectedSongIds.toList()
            else songForMenu?.id?.let { listOf(it) }
            ?: playerState.currentSong?.id?.let { listOf(it) }
            ?: emptyList()

            PlaylistPickerDialog(
                playlists = customPlaylists,
                targetSongIds = targetIds,
                onDismiss = { viewModel.showPlaylistPicker.value = false },
                onCreateNewPlaylist = {
                    viewModel.showPlaylistPicker.value = false
                    viewModel.showNewPlaylistDialog.value = true
                },
                onPlaylistSelected = { plId, plName ->
                    viewModel.addSongsToPlaylist(plId, plName, targetIds)
                }
            )
        }

        // Profile Dialog
        if (showProfile) {
            ProfileDialog(
                currentName = profileName,
                currentAvatarUri = profileAvatarUri,
                onDismiss = { viewModel.showProfileDialog.value = false },
                onSave = { name, avatar ->
                    viewModel.updateProfile(name, avatar)
                }
            )
        }
    }
}
