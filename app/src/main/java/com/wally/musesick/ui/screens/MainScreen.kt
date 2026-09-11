package com.wally.musesick.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.launch
import com.wally.musesick.ui.MusicViewModel
import androidx.compose.ui.graphics.Color
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.Playlist
import com.wally.musesick.ui.ScreenState
import com.wally.musesick.ui.components.AddFavoriteArtistSheet
import com.wally.musesick.ui.components.EditPlaylistSheet
import com.wally.musesick.ui.components.ExistingPlaylistSheet
import com.wally.musesick.ui.components.MiniPlayerBar
import com.wally.musesick.ui.components.NewPlaylistSheet
import com.wally.musesick.ui.components.SongActionMenuSheet
import com.wally.musesick.ui.components.UpdateDialog
import com.wally.musesick.ui.screens.ArtistOnboardingScreen
import com.wally.musesick.ui.screens.SettingsAppThemeScreen
import com.wally.musesick.ui.screens.SettingsNowPlayingScreen
import com.wally.musesick.ui.screens.SettingsScreen
import com.wally.musesick.ui.theme.rememberAmbientBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Playlist export JSON: local file save launcher
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingExportTitle by remember { mutableStateOf("playlist") }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(pendingExportJson!!)
                }
                Toast.makeText(context, "Playlist exported!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            pendingExportJson = null
        }
    }

    val hasCompletedArtistOnboarding by viewModel.hasCompletedArtistOnboarding.collectAsState()
    val playerStyle by viewModel.playerStyle.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val appThemeVariant by viewModel.appThemeVariant.collectAsState()
    val playerTheme by viewModel.playerTheme.collectAsState()
    val playerThemeVariant by viewModel.playerThemeVariant.collectAsState()
    val customAccentColor by viewModel.customAccentColor.collectAsState()
    val customThemeImagePath by viewModel.customThemeImagePath.collectAsState()

    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val isQueueReorderable by viewModel.isQueueReorderable.collectAsState()
    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val artistSearchQuery by viewModel.artistSearchQuery.collectAsState()
    val artistSearchResults by viewModel.artistSearchResults.collectAsState()
    val isArtistSearching by viewModel.isArtistSearching.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isSearchLoading by viewModel.isSearchLoading.collectAsState()

    // Artist Detail State
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val artistAlbums by viewModel.artistAlbums.collectAsState()
    val artistSongs by viewModel.artistSongs.collectAsState()
    val artistTrackListType by viewModel.artistTrackListType.collectAsState()
    val allArtistTracks by viewModel.allArtistTracks.collectAsState()
    val isAllTracksLoading by viewModel.isAllTracksLoading.collectAsState()
    val isArtistLoading by viewModel.isArtistLoading.collectAsState()

    // Album Detail State
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val albumTracks by viewModel.albumTracks.collectAsState()
    val isAlbumLoading by viewModel.isAlbumLoading.collectAsState()

    // Local Tracks State
    val localTracks by viewModel.localTracks.collectAsState()
    val isLocalLoading by viewModel.isLocalLoading.collectAsState()
    val localFilter by viewModel.localFormatFilter.collectAsState()

    // Playlist State
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }

    // For You & Search States
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val favoriteArtists by viewModel.favoriteArtists.collectAsState()
    val suggestedPlaylists by viewModel.suggestedPlaylists.collectAsState()
    val isSuggestedPlaylistsLoading by viewModel.isSuggestedPlaylistsLoading.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    var isAddFavoriteArtistSheetOpen by remember { mutableStateOf(false) }

    // Song Context Menu State
    val contextMenuTrack by viewModel.contextMenuTrack.collectAsState()
    val targetTrackForPlaylist by viewModel.targetTrackForPlaylist.collectAsState()
    val isExistingPlaylistSheetOpen by viewModel.isExistingPlaylistSheetOpen.collectAsState()
    val isNewPlaylistSheetOpen by viewModel.isNewPlaylistSheetOpen.collectAsState()

    // YouTube Playlist Import State
    val isImportingYtPlaylist by viewModel.isImportingYtPlaylist.collectAsState()
    val ytPlaylistPreview by viewModel.ytPlaylistPreview.collectAsState()
    val ytPlaylistImportError by viewModel.ytPlaylistImportError.collectAsState()

    // In-App Update State
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val updateToastMessage by viewModel.updateToastMessage.collectAsState()

    LaunchedEffect(updateToastMessage) {
        updateToastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUpdateToast()
        }
    }

    // Swipeable pages: For You (0), Playlist (1), Local (2)
    val pageTitles = remember { listOf("For You", "Playlist", "Local") }
    val pagerState = rememberPagerState(initialPage = selectedTab.coerceIn(0, 2), pageCount = { 3 })

    LaunchedEffect(pagerState.currentPage) {
        if (viewModel.selectedTab.value != pagerState.currentPage) {
            viewModel.setTab(pagerState.currentPage)
        }
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    if (!hasCompletedArtistOnboarding) {
        ArtistOnboardingScreen(
            initialArtists = artists,
            searchResults = artistSearchResults,
            isSearching = isArtistSearching,
            onSearchQueryChange = { viewModel.onArtistSearchQueryChanged(it) },
            onConfirmSelection = { selected ->
                viewModel.completeArtistOnboarding(selected)
            }
        )
        return
    }

    BackHandler(enabled = editingPlaylist != null || isFullPlayerOpen || contextMenuTrack != null || isExistingPlaylistSheetOpen || isNewPlaylistSheetOpen || isAddFavoriteArtistSheetOpen || currentScreen != ScreenState.HOME || (currentScreen == ScreenState.HOME && pagerState.currentPage != 0)) {
        when {
            editingPlaylist != null -> editingPlaylist = null
            isAddFavoriteArtistSheetOpen -> isAddFavoriteArtistSheetOpen = false
            contextMenuTrack != null -> viewModel.closeSongMenu()
            isExistingPlaylistSheetOpen -> viewModel.closeExistingPlaylistSheet()
            isNewPlaylistSheetOpen -> viewModel.closeNewPlaylistSheet()
            isFullPlayerOpen -> viewModel.closeFullPlayer()
            currentScreen != ScreenState.HOME -> viewModel.navigateBack()
            currentScreen == ScreenState.HOME && pagerState.currentPage != 0 -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
        }
    }

    val isPhotoOrAmbient = (appTheme == AppTheme.CUSTOM_IMAGE || appTheme == AppTheme.AMBIENT)
    val ambientBrush = rememberAmbientBrush()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isPhotoOrAmbient) Color(0xFF121212) else MaterialTheme.colorScheme.background)
    ) {
        if (appTheme == AppTheme.CUSTOM_IMAGE && !customThemeImagePath.isNullOrEmpty()) {
            AsyncImage(
                model = File(customThemeImagePath!!),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.08f)
                    .blur(28.dp)
                    .clipToBounds(),
                contentScale = ContentScale.Crop
            )
            // Very darkened layer on top of it
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
            )
        } else if (appTheme == AppTheme.AMBIENT) {
            // Ambient gradient based on the hour of the day
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.08f)
                    .background(ambientBrush)
                    .blur(28.dp)
                    .clipToBounds()
            )
            // Very darkened layer on top of it
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isPhotoOrAmbient) Color.Transparent else MaterialTheme.colorScheme.background),
            containerColor = if (isPhotoOrAmbient) Color.Transparent else MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp),
        topBar = {
                if (currentScreen == ScreenState.HOME) {
                    // Header displaying the name of the active window on the top-left
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = pageTitles.getOrElse(pagerState.currentPage) { "Stream" },
                            transitionSpec = {
                                (fadeIn() + slideInVertically { -it / 2 }) togetherWith
                                        (fadeOut() + slideOutVertically { it / 2 })
                            },
                            label = "active_screen_title"
                        ) { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Minimalist Nothing-style page indicator dots & Settings Icon
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pageTitles.indices.forEach { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .height(5.dp)
                                        .width(if (isSelected) 18.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = { viewModel.openSettings() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
        bottomBar = {
            if (playbackState.currentTrack != null) {
                AnimatedVisibility(
                    visible = !isFullPlayerOpen,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.84f, stiffness = 380f)
                    ) + fadeIn(animationSpec = tween(220)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f)
                    ) + fadeOut(animationSpec = tween(180))
                ) {
                    Box(modifier = Modifier.navigationBarsPadding()) {
                        MiniPlayerBar(
                            state = playbackState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.nextTrack() },
                            onClick = { viewModel.openFullPlayer() },
                            onSwipeUp = { viewModel.openFullPlayer() }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    ScreenState.HOME -> {
                        // Swipeable windows: Stream (0), Playlist (1), Local (2)
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            when (page) {
                                0 -> ForYouTab(
                                    recentlyPlayed = recentlyPlayed,
                                    favoriteArtists = favoriteArtists,
                                    playlists = playlists,
                                    suggestedPlaylists = suggestedPlaylists,
                                    isSuggestedPlaylistsLoading = isSuggestedPlaylistsLoading,
                                    playbackState = playbackState,
                                    onSearchClick = { viewModel.openSearch() },
                                    onSeeAllRecentlyPlayed = { viewModel.openRecentlyPlayed() },
                                    onManageFavoriteArtists = { isAddFavoriteArtistSheetOpen = true },
                                    onCreatePlaylistClick = { viewModel.openNewPlaylistSheet() },
                                    onArtistClick = { viewModel.selectArtist(it) },
                                    onPlaylistClick = { viewModel.selectPlaylist(it) },
                                    onSongClick = { track, list -> viewModel.playTrack(track, list) },
                                    onSongLongClick = { viewModel.openSongMenu(it) }
                                )
                                1 -> PlaylistsTab(
                                    playlists = playlists,
                                    onCreatePlaylistClick = { viewModel.openNewPlaylistSheet() },
                                    onPlaylistClick = { viewModel.selectPlaylist(it) },
                                    onEditPlaylistClick = { editingPlaylist = it },
                                    onDeletePlaylistClick = { viewModel.deletePlaylist(it.id) }
                                )
                                2 -> LocalTab(
                                    allTracks = localTracks,
                                    filteredTracks = viewModel.getFilteredLocalTracks(),
                                    isLoading = isLocalLoading,
                                    selectedFilter = localFilter,
                                    playbackState = playbackState,
                                    onFilterSelect = { viewModel.setLocalFormatFilter(it) },
                                    onRefresh = { viewModel.loadLocalTracks() },
                                    onFilesPicked = { viewModel.onFilesPicked(it) },
                                    onTrackSelect = { track, list -> viewModel.playTrack(track, list) },
                                    onTrackLongClick = { viewModel.openSongMenu(it) }
                                )
                            }
                        }
                    }
                    ScreenState.ARTIST_DETAIL -> {
                        selectedArtist?.let { artist ->
                            ArtistDetailScreen(
                                artist = artist,
                                albums = artistAlbums,
                                songs = artistSongs,
                                isLoading = isArtistLoading,
                                playbackState = playbackState,
                                onBack = { viewModel.navigateBack() },
                                onAlbumClick = { viewModel.selectAlbum(it) },
                                onSongClick = { track, list -> viewModel.playTrack(track, list) },
                                onSongLongClick = { viewModel.openSongMenu(it) },
                                onSeeMoreSongs = { viewModel.openArtistSongsList() },
                                isFavorite = favoriteArtists.any { it.id == artist.id },
                                onToggleFavorite = { viewModel.toggleFavoriteArtist(artist) }
                            )
                        }
                    }
                    ScreenState.ALBUM_DETAIL -> {
                        selectedAlbum?.let { album ->
                            AlbumDetailScreen(
                                album = album,
                                tracks = albumTracks,
                                isLoading = isAlbumLoading,
                                playbackState = playbackState,
                                onBack = { viewModel.navigateBack() },
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onTrackLongClick = { viewModel.openSongMenu(it) }
                            )
                        }
                    }
                    ScreenState.PLAYLIST_DETAIL -> {
                        selectedPlaylist?.let { playlist ->
                            PlaylistDetailScreen(
                                playlist = playlist,
                                playbackState = playbackState,
                                onBack = { viewModel.navigateBack() },
                                onTrackClick = { track, _ -> viewModel.playPlaylistTrack(playlist, track) },
                                onTrackLongClick = { viewModel.openSongMenu(it) },
                                onPlayAll = { _ -> viewModel.playAllFromPlaylist(playlist) },
                                onShuffleAll = { _ -> viewModel.shuffleAllFromPlaylist(playlist) },
                                onRemoveTrack = { track -> viewModel.removeTrackFromPlaylist(playlist.id, track.id) },
                                onDeletePlaylist = { viewModel.deletePlaylist(playlist.id) },
                                onExportLocally = {
                                    val json = viewModel.exportPlaylistAsJson(playlist)
                                    pendingExportJson = json
                                    pendingExportTitle = playlist.title
                                    saveFileLauncher.launch("${playlist.title}.json")
                                },
                                onExportShare = {
                                    val json = viewModel.exportPlaylistAsJson(playlist)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        putExtra(Intent.EXTRA_SUBJECT, "Musesick Playlist: ${playlist.title}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share playlist via..."))
                                },
                                onReorderTracks = { from, to ->
                                    viewModel.reorderTracksInPlaylist(playlist.id, from, to)
                                },
                                onEditPlaylist = {
                                    editingPlaylist = playlist
                                }
                            )
                        }
                    }
                    ScreenState.ARTIST_MANAGER -> {
                        ArtistSearchScreen(
                            artists = artists,
                            searchQuery = artistSearchQuery,
                            searchResults = artistSearchResults,
                            isSearching = isArtistSearching,
                            onSearchQueryChange = { viewModel.onArtistSearchQueryChanged(it) },
                            onAddArtist = { artist ->
                                viewModel.addArtist(artist)
                                Toast.makeText(context, "Added ${artist.name} to Home", Toast.LENGTH_SHORT).show()
                            },
                            onRemoveArtist = { artist ->
                                viewModel.removeArtist(artist)
                                Toast.makeText(context, "Removed ${artist.name}", Toast.LENGTH_SHORT).show()
                            },
                            onToggleVisibility = { artist -> viewModel.toggleArtistVisibility(artist) },
                            onResetDefaults = {
                                viewModel.resetArtistsToDefault()
                                Toast.makeText(context, "Restored default artists", Toast.LENGTH_SHORT).show()
                            },
                            onBack = { viewModel.closeArtistManager() }
                        )
                    }
                    ScreenState.ARTIST_TRACK_LIST -> {
                        selectedArtist?.let { artist ->
                            ArtistTrackListScreen(
                                artist = artist,
                                trackListType = artistTrackListType,
                                tracks = allArtistTracks,
                                isLoading = isAllTracksLoading,
                                playbackState = playbackState,
                                onBack = { viewModel.navigateBack() },
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onTrackLongClick = { viewModel.openSongMenu(it) },
                                onPlayAll = { tracks -> viewModel.playAll(tracks) },
                                onShuffleAll = { tracks -> viewModel.shuffleAll(tracks) }
                            )
                        }
                    }
                    ScreenState.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            searchResult = searchResult,
                            recentSearches = recentSearches,
                            isLoading = isSearchLoading,
                            playbackState = playbackState,
                            onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onAddRecentSearch = { viewModel.addRecentSearch(it) },
                            onRemoveRecentSearch = { viewModel.removeRecentSearch(it) },
                            onClearRecentSearches = { viewModel.clearRecentSearches() },
                            onArtistClick = { viewModel.selectArtist(it) },
                            onAlbumClick = { viewModel.selectAlbum(it) },
                            onSongClick = { track, list -> viewModel.playTrack(track, list) },
                            onSongLongClick = { viewModel.openSongMenu(it) },
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    ScreenState.RECENTLY_PLAYED -> {
                        RecentlyPlayedScreen(
                            tracks = recentlyPlayed,
                            playbackState = playbackState,
                            onBack = { viewModel.navigateBack() },
                            onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                            onTrackLongClick = { viewModel.openSongMenu(it) },
                            onPlayAll = { tracks -> viewModel.playAll(tracks) },
                            onShuffleAll = { tracks -> viewModel.shuffleAll(tracks) },
                            onClearAll = {
                                viewModel.clearRecentlyPlayed()
                                Toast.makeText(context, "Recently played cleared", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    ScreenState.SETTINGS -> {
                        SettingsScreen(
                            currentVersion = viewModel.currentAppVersion,
                            playerStyle = playerStyle,
                            appTheme = appTheme,
                            appThemeVariant = appThemeVariant,
                            customAccentColor = customAccentColor,
                            isCheckingUpdate = isCheckingUpdate,
                            onBack = { viewModel.navigateBack() },
                            onCheckForUpdates = { viewModel.checkForUpdates(manual = true) },
                            onOpenNowPlaying = { viewModel.openNowPlayingSettings() },
                            onOpenAppTheme = { viewModel.openAppThemeSettings() }
                        )
                    }
                    ScreenState.SETTINGS_NOW_PLAYING -> {
                        SettingsNowPlayingScreen(
                            currentStyle = playerStyle,
                            currentPlayerTheme = playerTheme,
                            currentPlayerThemeVariant = playerThemeVariant,
                            customAccentColor = customAccentColor,
                            onStyleSelected = { viewModel.setPlayerStyle(it) },
                            onPlayerThemeSelected = { theme, variant -> viewModel.setPlayerTheme(theme, variant) },
                            onPlayerThemeVariantSelected = { viewModel.setPlayerThemeVariant(it) },
                            onColorSelected = { viewModel.setCustomAccentColor(it) },
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    ScreenState.SETTINGS_APP_THEME -> {
                        SettingsAppThemeScreen(
                            currentTheme = appTheme,
                            currentVariant = appThemeVariant,
                            customAccentColor = customAccentColor,
                            customThemeImagePath = customThemeImagePath,
                            onThemeSelected = { theme, variant -> viewModel.setAppTheme(theme, variant) },
                            onVariantSelected = { viewModel.setAppThemeVariant(it) },
                            onColorSelected = { viewModel.setCustomAccentColor(it) },
                            onSelectCustomImage = { viewModel.setCustomThemeImage(it) },
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
            }
        }
    }
    }

    // Song Action Context Menu Sheet (Long press)
    contextMenuTrack?.let { track ->
        SongActionMenuSheet(
            track = track,
            onPlayNow = {
                viewModel.playTrack(track)
                viewModel.closeSongMenu()
            },
            onPlayNext = {
                viewModel.playNext(track)
                viewModel.closeSongMenu()
                Toast.makeText(context, "Playing next: ${track.title}", Toast.LENGTH_SHORT).show()
            },
            onAddToQueue = {
                viewModel.addToQueue(track)
                viewModel.closeSongMenu()
                Toast.makeText(context, "Added to queue: ${track.title}", Toast.LENGTH_SHORT).show()
            },
            onAddToExistingPlaylist = {
                viewModel.openExistingPlaylistSheet(track)
            },
            onAddToNewPlaylist = {
                viewModel.openNewPlaylistSheet(track)
            },
            onDismiss = { viewModel.closeSongMenu() },
            isCustomImageTheme = isPhotoOrAmbient,
            customThemeImagePath = customThemeImagePath,
            ambientBrush = if (appTheme == AppTheme.AMBIENT) ambientBrush else null
        )
    }

    // Existing Playlist Picker Sheet
    if (isExistingPlaylistSheetOpen && targetTrackForPlaylist != null) {
        ExistingPlaylistSheet(
            playlists = playlists,
            track = targetTrackForPlaylist!!,
            onSelectPlaylist = { playlist ->
                val track = targetTrackForPlaylist!!
                viewModel.addTrackToPlaylist(playlist.id, track)
                Toast.makeText(context, "Added to ${playlist.title}", Toast.LENGTH_SHORT).show()
            },
            onCreateNewClick = {
                viewModel.openNewPlaylistSheet(targetTrackForPlaylist)
            },
            onDismiss = { viewModel.closeExistingPlaylistSheet() },
            isCustomImageTheme = isPhotoOrAmbient,
            customThemeImagePath = customThemeImagePath,
            ambientBrush = if (appTheme == AppTheme.AMBIENT) ambientBrush else null
        )
    }

    // New Playlist Creation Sheet
    if (isNewPlaylistSheetOpen) {
        NewPlaylistSheet(
            initialTrack = targetTrackForPlaylist,
            onCreatePlaylist = { title, uri ->
                val track = targetTrackForPlaylist
                viewModel.createPlaylist(title, uri, track)
                Toast.makeText(context, "Created playlist \"$title\"", Toast.LENGTH_SHORT).show()
            },
            onImportJson = { json ->
                viewModel.importPlaylistFromJson(json)
                Toast.makeText(context, "Playlist imported!", Toast.LENGTH_SHORT).show()
                viewModel.closeNewPlaylistSheet()
            },
            isImportingYtPlaylist = isImportingYtPlaylist,
            ytPlaylistPreview = ytPlaylistPreview,
            ytPlaylistImportError = ytPlaylistImportError,
            onFetchYtPlaylist = { url -> viewModel.fetchYouTubePlaylistPreview(url) },
            onConfirmImportYtPlaylist = { customTitle ->
                viewModel.confirmImportYouTubePlaylist(customTitle) { created ->
                    Toast.makeText(context, "Imported \"${created.title}\" (${created.tracks.size} songs)", Toast.LENGTH_SHORT).show()
                }
            },
            onClearYtPreview = { viewModel.clearYouTubePlaylistPreview() },
            onDismiss = { viewModel.closeNewPlaylistSheet() },
            isCustomImageTheme = isPhotoOrAmbient,
            customThemeImagePath = customThemeImagePath,
            ambientBrush = if (appTheme == AppTheme.AMBIENT) ambientBrush else null
        )
    }

    // Edit Playlist Sheet
    val currentEditingPlaylist = editingPlaylist
    if (currentEditingPlaylist != null) {
        EditPlaylistSheet(
            playlist = currentEditingPlaylist,
            onSave = { newTitle, newImageUri, removeImage ->
                viewModel.editPlaylist(currentEditingPlaylist.id, newTitle, newImageUri, removeImage)
                Toast.makeText(context, "Playlist updated", Toast.LENGTH_SHORT).show()
                editingPlaylist = null
            },
            onDismiss = { editingPlaylist = null },
            isCustomImageTheme = isPhotoOrAmbient,
            customThemeImagePath = customThemeImagePath,
            ambientBrush = if (appTheme == AppTheme.AMBIENT) ambientBrush else null
        )
    }

    // Full Player Modal Sheet
    AnimatedVisibility(
        visible = isFullPlayerOpen,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)
        ) + fadeOut(animationSpec = tween(180))
    ) {
        FullPlayerSheet(
            state = playbackState,
            queue = queue,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.nextTrack() },
            onPrevious = { viewModel.previousTrack() },
            onSeek = { viewModel.seekToFraction(it) },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onToggleRepeat = { viewModel.toggleRepeat() },
            onTrackSelect = { viewModel.playTrack(it) },
            onReorderQueue = { from, to -> viewModel.reorderQueue(from, to) },
            onRemoveFromQueue = { viewModel.removeFromQueue(it) },
            onTrackMenuClick = { viewModel.openSongMenu(it) },
            onClose = { viewModel.closeFullPlayer() },
            playerStyle = playerStyle,
            playerTheme = playerTheme,
            playerThemeVariant = playerThemeVariant,
            isQueueReorderable = isQueueReorderable,
            customAccentColor = customAccentColor,
            appTheme = appTheme,
            customThemeImagePath = customThemeImagePath,
            ambientBrush = if (appTheme == AppTheme.AMBIENT) ambientBrush else null
        )
    }

    // In-App Update Dialog
    updateInfo?.let { info ->
        UpdateDialog(
            updateInfo = info,
            currentVersion = viewModel.currentAppVersion,
            downloadProgress = downloadProgress,
            isDownloading = downloadProgress != null,
            onUpdateClick = { viewModel.startUpdateDownload(context) },
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }

    // Manage Favourite Artists Sheet
    if (isAddFavoriteArtistSheetOpen) {
        AddFavoriteArtistSheet(
            favoriteArtists = favoriteArtists,
            searchResults = artistSearchResults,
            isSearching = isArtistSearching,
            onSearchQueryChange = { viewModel.onArtistSearchQueryChanged(it) },
            onAddFavorite = { artist ->
                viewModel.addFavoriteArtist(artist)
                Toast.makeText(context, "Added ${artist.name} to favourites", Toast.LENGTH_SHORT).show()
            },
            onRemoveFavorite = { artistId ->
                viewModel.removeFavoriteArtist(artistId)
                Toast.makeText(context, "Removed from favourites", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                isAddFavoriteArtistSheetOpen = false
                viewModel.onArtistSearchQueryChanged("")
            }
        )
    }
}
