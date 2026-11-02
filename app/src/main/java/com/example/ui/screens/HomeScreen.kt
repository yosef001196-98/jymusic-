package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.data.model.SongFolder
import com.example.data.model.formatSongCount
import com.example.ui.components.AlbumCard
import com.example.ui.components.ArtistCard
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.DetailViewScreen
import com.example.ui.components.EditTrackTagsDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.FolderCard
import com.example.ui.components.LyricsDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NewPlaylistCard
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.PlaybackSpeedBottomSheet
import com.example.ui.components.PlaylistCard
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.SelectionBottomBar
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SongInfoDialog
import com.example.ui.components.SongItemRow
import com.example.ui.components.SongOptionsBottomSheet
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenGlow
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark2
import com.example.ui.theme.SurfaceDark3
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DetailView
import com.example.ui.viewmodel.MusicTab
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MusicViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val playlistsWithArt by viewModel.playlistsWithArt.collectAsState()
    val customPlaylists by viewModel.customPlaylists.collectAsState()
    val songFolders by viewModel.songFolders.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()

    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchVisible by viewModel.isSearchVisible.collectAsState()

    val selectedSongIds by viewModel.selectedSongIds.collectAsState()
    val isSelecting by viewModel.isSelecting.collectAsState()
    val detailView by viewModel.detailView.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()

    val showEqualizer by viewModel.showEqualizerDialog.collectAsState()
    val showSleepTimer by viewModel.showSleepTimerDialog.collectAsState()
    val showSpeedSheet by viewModel.showSpeedSheet.collectAsState()
    val showLyrics by viewModel.showLyricsDialog.collectAsState()
    val showNewPlaylist by viewModel.showNewPlaylistDialog.collectAsState()
    val showPlaylistPicker by viewModel.showPlaylistPicker.collectAsState()
    val showSongInfo by viewModel.showSongInfoDialog.collectAsState()
    val showDeleteConfirm by viewModel.showDeleteConfirmDialog.collectAsState()
    val showEditTags by viewModel.showEditTagsDialog.collectAsState()
    val songForMenu by viewModel.songForMenu.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = selectedTab.ordinal,
        pageCount = { MusicTab.values().size }
    )

    // Synchronize Tab & Pager
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setSelectedTab(MusicTab.values()[pagerState.currentPage])
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) {
            pagerState.animateScrollToPage(selectedTab.ordinal)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            viewModel.scanDevice()
        }
    }

    fun requestScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    val filteredSongs = remember(allSongs, searchQuery) {
        if (searchQuery.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    val likedSongs = remember(allSongs) { allSongs.filter { it.isLiked } }
    val recentlyPlayedSongs = remember(allSongs) {
        allSongs.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(25)
    }
    val mostPlayedSongs = remember(allSongs) {
        allSongs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(25)
    }

    val artists = remember(allSongs) {
        allSongs.groupBy { it.artist }.filterKeys { it.isNotBlank() && it != "Unknown Artist" }
    }

    val albums = remember(allSongs) {
        allSongs.groupBy { it.album }.filterKeys { it.isNotBlank() && it != "Unknown Album" }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        // Subtle top radial glow
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
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Sleek Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "JY Music",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Scan Button
                            IconButton(
                                onClick = { requestScan() },
                                modifier = Modifier.size(40.dp).testTag("top_scan_btn")
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
                                        contentDescription = "Scan Media",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Search Button
                            IconButton(
                                onClick = { viewModel.toggleSearch() },
                                modifier = Modifier.size(40.dp).testTag("top_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isSearchVisible) BrandGreen else TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Equalizer Quick Button
                            IconButton(
                                onClick = { viewModel.showEqualizerDialog.value = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("top_equalizer_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Equalizer",
                                    tint = BrandGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Search Field
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
                                .testTag("home_search_field"),
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

                    // Scrollable Tab Row (Chips Style)
                    if (detailView == null && !isSearchVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MusicTab.values().forEach { tab ->
                                val isSelected = selectedTab == tab
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) BrandGreen else SurfaceDark2)
                                        .clickable {
                                            viewModel.setSelectedTab(tab)
                                            scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .testTag("tab_${tab.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) PitchBlack else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // If Detail View open (e.g. Playlist, Folder, Artist, Album)
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
                        onSongMenuClick = { song -> viewModel.songForMenu.value = song },
                        onDeletePlaylist = { pId -> viewModel.deletePlaylist(pId) },
                        onAddSongs = if (detailView is DetailView.PlaylistView) {
                            { viewModel.showPlaylistPicker.value = true }
                        } else null
                    )
                } else if (searchQuery.isNotBlank()) {
                    // Search results with generous bottom padding so it doesn't hide behind mini player
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
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
                                onMenuClick = { viewModel.songForMenu.value = song }
                            )
                        }
                    }
                } else {
                    // Horizontal Pager for 5 tabs
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (MusicTab.values()[page]) {
                            // 0: SONGS TAB
                            MusicTab.SONGS -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(bottom = 120.dp)
                                ) {
                                    val deviceSongCount = allSongs.count { !it.isDemo }
                                    if (deviceSongCount == 0) {
                                        item {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SurfaceDark2,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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
                                                            text = "Tap Scan to load your device music files.",
                                                            color = TextSecondary,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { requestScan() },
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

                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "All Songs",
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = formatSongCount(allSongs.size),
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
                                            onMenuClick = { viewModel.songForMenu.value = song }
                                        )
                                    }
                                }
                            }

                            // 1: PLAYLISTS TAB
                            MusicTab.PLAYLISTS -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(bottom = 120.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        NewPlaylistCard(
                                            onClick = { viewModel.showNewPlaylistDialog.value = true }
                                        )
                                    }

                                    item {
                                        PlaylistCard(
                                            title = "Liked Songs",
                                            songCount = likedSongs.size,
                                            icon = Icons.Default.Favorite,
                                            gradientColors = listOf(Color(0xFF450af5), Color(0xFFc4efd9)),
                                            onClick = {
                                                viewModel.openDetail(
                                                    DetailView.PlaylistView("Liked Songs", likedSongs)
                                                )
                                            }
                                        )
                                    }

                                    item {
                                        PlaylistCard(
                                            title = "Most Played",
                                            songCount = mostPlayedSongs.size,
                                            icon = Icons.Default.TrendingUp,
                                            gradientColors = listOf(Color(0xFFff416c), Color(0xFFff4b2b)),
                                            onClick = {
                                                viewModel.openDetail(
                                                    DetailView.PlaylistView("Most Played", mostPlayedSongs)
                                                )
                                            }
                                        )
                                    }

                                    item {
                                        PlaylistCard(
                                            title = "Recently Played",
                                            songCount = recentlyPlayedSongs.size,
                                            icon = Icons.Default.History,
                                            gradientColors = listOf(Color(0xFF11998e), Color(0xFF38ef7d)),
                                            onClick = {
                                                viewModel.openDetail(
                                                    DetailView.PlaylistView("Recently Played", recentlyPlayedSongs)
                                                )
                                            }
                                        )
                                    }

                                    // Custom user playlists showing mosaic 2x2 collage covers and accurate count
                                    items(playlistsWithArt, key = { it.id }) { pl ->
                                        PlaylistCard(
                                            title = pl.name,
                                            songCount = pl.songCount,
                                            coverUri = pl.firstSongArtUri,
                                            albumArts = pl.albumArts,
                                            onClick = {
                                                viewModel.openPlaylist(pl.id, pl.name)
                                            }
                                        )
                                    }
                                }
                            }

                            // 2: ARTISTS TAB
                            MusicTab.ARTISTS -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 104.dp),
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(bottom = 120.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

                            // 3: ALBUMS TAB
                            MusicTab.ALBUMS -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 135.dp),
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(bottom = 120.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(albums.entries.toList(), key = { it.key }) { (albumName, albumSongs) ->
                                        AlbumCard(
                                            albumName = albumName,
                                            artistName = albumSongs.firstOrNull()?.artist ?: "",
                                            songCount = albumSongs.size,
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

                            // 4: FOLDERS TAB (Lark Player Style - at the END)
                            MusicTab.FOLDERS -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(bottom = 120.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "Music Folders (${songFolders.size})",
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }

                                    items(songFolders, key = { it.name }) { folder ->
                                        FolderCard(
                                            folder = folder,
                                            onClick = {
                                                viewModel.openDetail(
                                                    DetailView.FolderView(folder.name, folder.songs)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Multi-Selection Bar
                SelectionBottomBar(
                    selectedCount = selectedSongIds.size,
                    onAddToPlaylist = { viewModel.showPlaylistPicker.value = true },
                    onCancel = { viewModel.cancelSelection() },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 70.dp)
                )

                // Elegant Floating Mini Player above bottom edge
                if (playerState.currentSong != null && !isNowPlayingExpanded) {
                    MiniPlayer(
                        playerState = playerState,
                        onExpand = { viewModel.setNowPlayingExpanded(true) },
                        onTogglePlay = { viewModel.togglePlayPause() },
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp)
                    )
                }
            }
        }

        // The Showstopper "Now Playing" Screen (Swipe to minimize without stopping audio)
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
                viewModel.songForMenu.value = song
                viewModel.showPlaylistPicker.value = true
            },
            onOpenSpeed = { viewModel.showSpeedSheet.value = true },
            onOptionsClick = {
                viewModel.songForMenu.value = playerState.currentSong
            }
        )

        // Three Dots Options Bottom Sheet
        if (songForMenu != null && !showSongInfo && !showDeleteConfirm) {
            SongOptionsBottomSheet(
                song = songForMenu!!,
                onDismiss = { viewModel.songForMenu.value = null },
                onPlayNext = { viewModel.playNextInQueue(songForMenu!!) },
                onAddToQueue = { viewModel.addToQueue(songForMenu!!) },
                onAddToPlaylist = { viewModel.showPlaylistPicker.value = true },
                onToggleLike = { viewModel.toggleLike(songForMenu!!) },
                onGoToAlbum = {
                    val albumTracks = allSongs.filter { it.album == songForMenu!!.album }
                    viewModel.openDetail(DetailView.AlbumView(songForMenu!!.album, albumTracks))
                    viewModel.setNowPlayingExpanded(false)
                },
                onGoToArtist = {
                    val artistTracks = allSongs.filter { it.artist == songForMenu!!.artist }
                    viewModel.openDetail(DetailView.ArtistView(songForMenu!!.artist, artistTracks))
                    viewModel.setNowPlayingExpanded(false)
                },
                onViewInfo = { viewModel.showSongInfoDialog.value = true },
                onEditTags = { viewModel.showEditTagsDialog.value = true },
                onDeleteRequest = { viewModel.showDeleteConfirmDialog.value = true }
            )
        }

        // Edit Track Tags Dialog
        if (showEditTags && songForMenu != null) {
            EditTrackTagsDialog(
                song = songForMenu!!,
                onDismiss = { viewModel.showEditTagsDialog.value = false },
                onSave = { newTitle, newArtist ->
                    viewModel.updateSongTags(songForMenu!!, newTitle, newArtist)
                }
            )
        }

        // Lark Player Style Speed Bottom Sheet
        if (showSpeedSheet) {
            PlaybackSpeedBottomSheet(
                initialSettings = playerState.speedSettings,
                onDismiss = { viewModel.showSpeedSheet.value = false },
                onSave = { speed, preservePitch ->
                    viewModel.setSpeedSettings(speed, preservePitch)
                }
            )
        }

        // Song Info Dialog
        if (showSongInfo && songForMenu != null) {
            SongInfoDialog(
                song = songForMenu!!,
                onDismiss = { viewModel.showSongInfoDialog.value = false }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirm && songForMenu != null) {
            DeleteConfirmationDialog(
                songTitle = songForMenu!!.title,
                onDismiss = { viewModel.showDeleteConfirmDialog.value = false },
                onConfirmDelete = { viewModel.deleteSong(songForMenu!!) }
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

        // Lyrics Dialog
        if (showLyrics && songForMenu != null) {
            LyricsDialog(
                songTitle = songForMenu!!.title,
                initialLyrics = songForMenu!!.lyrics,
                onDismiss = { viewModel.showLyricsDialog.value = false },
                onSave = { lyrics -> viewModel.saveLyrics(songForMenu!!.id, lyrics) }
            )
        }

        // Create Playlist Dialog (Starts EMPTY, fixing playlist bug!)
        if (showNewPlaylist) {
            CreatePlaylistDialog(
                onDismiss = { viewModel.showNewPlaylistDialog.value = false },
                onConfirm = { name ->
                    val initialIds = if (isSelecting) selectedSongIds.toList() else emptyList()
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
    }
}
