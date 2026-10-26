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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.launch
import com.wally.musesick.R
import com.wally.musesick.ui.MusicViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.Playlist
import com.wally.musesick.ui.ScreenState
import com.wally.musesick.ui.components.AddFavoriteArtistSheet
import com.wally.musesick.ui.components.BottomNavigationControlBar
import com.wally.musesick.ui.components.EditPlaylistSheet
import com.wally.musesick.ui.components.ExistingPlaylistSheet
import com.wally.musesick.ui.components.MiniPlayerBar
import com.wally.musesick.ui.components.NewPlaylistSheet
import com.wally.musesick.ui.components.SongActionMenuSheet
import com.wally.musesick.ui.components.NothingFormatBadge
import androidx.compose.material.icons.filled.Person
import com.wally.musesick.ui.components.UpdateDialog
import com.wally.musesick.ui.components.YouTubeLoginDialog
import com.wally.musesick.ui.screens.ArtistOnboardingScreen
import com.wally.musesick.ui.screens.SettingsAppThemeScreen
import com.wally.musesick.ui.screens.SettingsScreen

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
    val currentPlayingTrackId by viewModel.currentPlayingTrackId.collectAsState()
    val isCurrentTrackPlaying by viewModel.isCurrentTrackPlaying.collectAsState()
    val hasActiveTrack by viewModel.hasActiveTrack.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val isQueueReorderable by viewModel.isQueueReorderable.collectAsState()
    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
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

    // YouTube Music Account State
    val ytAccountInfo by viewModel.ytAccountInfo.collectAsState()
    val isSyncingYtAccount by viewModel.isSyncingYtAccount.collectAsState()
    val autoSyncInterval by viewModel.autoSyncInterval.collectAsState()
    var isYtLoginDialogOpen by remember { mutableStateOf(false) }

    // Offline & Downloads State
    val isOffline by viewModel.isOffline.collectAsState()
    val totalDownloadStorage by viewModel.totalDownloadStorage.collectAsState()

    LaunchedEffect(updateToastMessage) {
        updateToastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUpdateToast()
        }
    }

    // Swipeable pages: For You (0), Playlists (1), Local (2)
    val pageTitles = remember { listOf("For You", "Playlists", "Local") }
    val pagerState = rememberPagerState(initialPage = selectedTab.coerceIn(0, 2), pageCount = { 3 })

    val forYouListState = rememberLazyListState()
    val playlistsGridState = rememberLazyGridState()
    val localListState = rememberLazyListState()

    val isTopScrolled by remember(pagerState.currentPage) {
        derivedStateOf {
            when (pagerState.currentPage) {
                0 -> forYouListState.firstVisibleItemIndex > 0 || forYouListState.firstVisibleItemScrollOffset > 0
                1 -> playlistsGridState.firstVisibleItemIndex > 0 || playlistsGridState.firstVisibleItemScrollOffset > 0
                2 -> localListState.firstVisibleItemIndex > 0 || localListState.firstVisibleItemScrollOffset > 0
                else -> false
            }
        }
    }

    val localFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onFilesPicked(uris)
        }
    }

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

    val aquaBackgroundDrawableRes = when (appThemeVariant) {
        "damegami" -> R.drawable.damegami
        else -> R.drawable.darugami
    }

    val themeDrawableRes: Int? = when (appTheme) {
        AppTheme.DEFAULT -> R.drawable.primarybg
        AppTheme.AQUA -> aquaBackgroundDrawableRes
        else -> null
    }

    val themeBackgroundImage: Any? = when {
        appTheme == AppTheme.DEFAULT -> R.drawable.primarybg
        appTheme == AppTheme.CUSTOM_IMAGE && !customThemeImagePath.isNullOrEmpty() -> File(customThemeImagePath!!)
        appTheme == AppTheme.CUSTOM_IMAGE -> R.drawable.primarybg
        appTheme == AppTheme.AQUA -> aquaBackgroundDrawableRes
        else -> null
    }

    val isGlassmorphism = appTheme in listOf(AppTheme.DEFAULT, AppTheme.CUSTOM_IMAGE, AppTheme.CUSTOM_COLOR, AppTheme.AQUA)
    val isImageBackground = themeBackgroundImage != null || appTheme == AppTheme.CUSTOM_COLOR
    val isCustomImage = isImageBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isImageBackground) Color(0xFF000000) else MaterialTheme.colorScheme.background)
    ) {
        if (isImageBackground && appTheme != AppTheme.CUSTOM_COLOR) {
            if (themeDrawableRes != null) {
                Image(
                    painter = painterResource(id = themeDrawableRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.08f)
                        .blur(28.dp)
                        .clipToBounds(),
                    contentScale = ContentScale.Crop
                )
            } else if (themeBackgroundImage != null) {
                AsyncImage(
                    model = themeBackgroundImage,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.08f)
                        .blur(28.dp)
                        .clipToBounds(),
                    contentScale = ContentScale.Crop
                )
            }
            // Very darkened layer on top of it
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
            )
        }

        // Image background mode: rich expansive gradient in the bottom section of the app
        if (isImageBackground) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x18000000),
                                Color(0x45000000),
                                Color(0x8A000000),
                                Color(0xCE000000),
                                Color(0xFA000000)
                            )
                        )
                    )
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isImageBackground) Color.Transparent else MaterialTheme.colorScheme.background),
            containerColor = if (isImageBackground) Color.Transparent else MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
            val isAccountsOrSettings = currentScreen == ScreenState.SETTINGS ||
                    currentScreen == ScreenState.SETTINGS_APP_THEME ||
                    currentScreen == ScreenState.SETTINGS_AUTO_SYNC
            val showBottomBar = currentScreen != ScreenState.SEARCH && !isFullPlayerOpen && !isAccountsOrSettings && !isNewPlaylistSheetOpen
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f)
                ) + fadeIn(animationSpec = tween(160)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f)
                ) + fadeOut(animationSpec = tween(140))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Smooth gradient overlay in the bottom section
                    val bg = if (isGlassmorphism) Color(0xDE050505) else MaterialTheme.colorScheme.background
                    val bottomGradientBrush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.25f to bg.copy(alpha = 0.40f),
                        0.55f to bg.copy(alpha = 0.85f),
                        0.78f to bg,
                        1.0f to bg
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(bottomGradientBrush)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Playlists Tab (Page 1): Floating Plus Button above bottom bar on the bottom right
                            androidx.compose.animation.AnimatedVisibility(
                                visible = currentScreen == ScreenState.HOME && pagerState.currentPage == 1,
                                enter = fadeIn(tween(160)),
                                exit = fadeOut(tween(140)),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Row(
                                    modifier = Modifier.padding(end = 20.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isGlassmorphism) Color.White.copy(alpha = 0.20f)
                                                else MaterialTheme.colorScheme.primaryContainer
                                            )
                                            .border(
                                                width = 1.dp,
                                                brush = if (isGlassmorphism) Brush.linearGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.50f),
                                                        Color.White.copy(alpha = 0.15f),
                                                        Color.White.copy(alpha = 0.35f)
                                                    )
                                                ) else SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                viewModel.openNewPlaylistSheet()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Playlist",
                                            tint = if (isGlassmorphism) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }

                            // Local Tab (Page 2): 4 Glassmorphism Circular Buttons (Downloaded, Device, Open, Refresh)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = currentScreen == ScreenState.HOME && pagerState.currentPage == 2,
                                enter = fadeIn(tween(160)),
                                exit = fadeOut(tween(140)),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                val isDownloadedActive = localFilter == "DOWNLOADED"
                                val isDeviceActive = localFilter != "DOWNLOADED"

                                // 1. Downloaded
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDownloadedActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            else if (isGlassmorphism) Color.White.copy(alpha = 0.18f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (isDownloadedActive) 1.5.dp else 1.dp,
                                            color = if (isDownloadedActive) MaterialTheme.colorScheme.primary
                                            else if (isGlassmorphism) Color.White.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.setLocalFormatFilter("DOWNLOADED")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Downloaded",
                                        tint = if (isDownloadedActive) MaterialTheme.colorScheme.primary
                                               else if (isGlassmorphism) Color.White.copy(alpha = 0.9f)
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 2. Device (Phone icon)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDeviceActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            else if (isGlassmorphism) Color.White.copy(alpha = 0.18f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (isDeviceActive) 1.5.dp else 1.dp,
                                            color = if (isDeviceActive) MaterialTheme.colorScheme.primary
                                            else if (isGlassmorphism) Color.White.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.setLocalFormatFilter("DEVICE")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = "Device",
                                        tint = if (isDeviceActive) MaterialTheme.colorScheme.primary
                                               else if (isGlassmorphism) Color.White.copy(alpha = 0.9f)
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 3. Open (Plus icon)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isGlassmorphism) Color.White.copy(alpha = 0.18f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isGlassmorphism) Color.White.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            localFilePicker.launch(arrayOf("audio/*", "audio/mpeg", "audio/mp4", "audio/flac"))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Open Audio Files",
                                        tint = if (isGlassmorphism) Color.White.copy(alpha = 0.9f)
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // 4. Refresh (Refresh icon)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isGlassmorphism) Color.White.copy(alpha = 0.18f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isGlassmorphism) Color.White.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.loadLocalTracks()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = if (isGlassmorphism) Color.White.copy(alpha = 0.9f)
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                        val miniPlayerState by viewModel.playbackState.collectAsState()
                        BottomNavigationControlBar(
                            hasActiveTrack = hasActiveTrack,
                            onSearchClick = { viewModel.openSearch() },
                            miniPlayerState = miniPlayerState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.nextTrack() },
                            onOpenFullPlayer = { viewModel.openFullPlayer() },
                            onDoubleTapNowPlaying = {
                                viewModel.closeFullPlayer()
                                viewModel.stopSong()
                            },
                            isGlassmorphism = isGlassmorphism
                        )
                    }
                }
            }
        }
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState == ScreenState.SEARCH) {
                        (slideInVertically(
                            initialOffsetY = { (it * 0.25f).toInt() },
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 1000f)
                        ) + fadeIn(tween(120))) togetherWith (fadeOut(tween(80)))
                    } else if (initialState == ScreenState.SEARCH) {
                        (fadeIn(tween(80))) togetherWith (slideOutVertically(
                            targetOffsetY = { (it * 0.25f).toInt() },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 1000f)
                        ) + fadeOut(tween(100)))
                    } else if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    ScreenState.HOME -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Swipeable windows: Stream (0), Playlist (1), Local (2)
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1
                            ) { page ->
                                when (page) {
                                    0 -> ForYouTab(
                                        listState = forYouListState,
                                        recentlyPlayed = recentlyPlayed,
                                        favoriteArtists = favoriteArtists,
                                        playlists = playlists,
                                        suggestedPlaylists = suggestedPlaylists,
                                        isSuggestedPlaylistsLoading = isSuggestedPlaylistsLoading,
                                        playingTrackId = currentPlayingTrackId,
                                        isPlaying = isCurrentTrackPlaying,
                                        isOffline = isOffline,
                                        isTrackDownloaded = { viewModel.isSongDownloaded(it) },
                                        onGoToDownloads = {
                                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                            viewModel.setLocalFormatFilter("DOWNLOADED")
                                        },
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
                                        gridState = playlistsGridState,
                                        playlists = playlists,
                                        onCreatePlaylistClick = { viewModel.openNewPlaylistSheet() },
                                        onPlaylistClick = { viewModel.selectPlaylist(it) },
                                        onEditPlaylistClick = { editingPlaylist = it },
                                        onDeletePlaylistClick = { viewModel.deletePlaylist(it.id) }
                                    )
                                    2 -> LocalTab(
                                        listState = localListState,
                                        allTracks = localTracks,
                                        filteredTracks = viewModel.getFilteredLocalTracks(),
                                        isLoading = isLocalLoading,
                                        selectedFilter = localFilter,
                                        playingTrackId = currentPlayingTrackId,
                                        isPlaying = isCurrentTrackPlaying,
                                        isTrackDownloaded = { viewModel.isSongDownloaded(it) },
                                        totalDownloadStorage = totalDownloadStorage,
                                        onFilterSelect = { viewModel.setLocalFormatFilter(it) },
                                        onRefresh = { viewModel.loadLocalTracks() },
                                        onFilesPicked = { viewModel.onFilesPicked(it) },
                                        onTrackSelect = { track, list -> viewModel.playTrack(track, list) },
                                        onTrackLongClick = { viewModel.openSongMenu(it) }
                                    )
                                }
                            }

                            // Smooth gradient overlay in the top section (appears only when user starts scrolling)
                            val bg = if (isGlassmorphism) Color.Black else MaterialTheme.colorScheme.background
                            val topGradientBrush = Brush.verticalGradient(
                                0.0f to bg,
                                0.35f to bg,
                                0.58f to bg.copy(alpha = 0.92f),
                                0.80f to bg.copy(alpha = 0.52f),
                                1.0f to Color.Transparent
                            )
                            AnimatedVisibility(
                                visible = isTopScrolled,
                                enter = fadeIn(tween(250)),
                                exit = fadeOut(tween(200)),
                                modifier = Modifier.align(Alignment.TopCenter)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .background(topGradientBrush)
                                )
                            }

                            // Top Floating Header: Title Pill on the left, Offline Cloud Icon in the center, Dots & Profile Pill on the right
                            val glassBorder = BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.32f)
                                    )
                                )
                            )

                            val surfaceColor = if (isGlassmorphism) {
                                Color(0x3D1A1A1A)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
                            }

                            val borderStroke = if (isGlassmorphism) {
                                glassBorder
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                // Title Pill: "For You", "Playlists", "Local" (with glassmorphism and matching search pill styling)
                                Surface(
                                    shape = RoundedCornerShape(28.dp),
                                    color = surfaceColor,
                                    border = borderStroke,
                                    tonalElevation = if (isGlassmorphism) 0.dp else 3.dp,
                                    shadowElevation = if (isGlassmorphism) 8.dp else 4.dp,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .align(Alignment.CenterStart)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 18.dp),
                                        contentAlignment = Alignment.Center
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
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGlassmorphism) Color.White.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }

                                // Offline Indicator Icon in the top center between the page indicator text and the page indicator pill
                                AnimatedVisibility(
                                    visible = isOffline,
                                    enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f),
                                    modifier = Modifier.align(Alignment.Center)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = surfaceColor,
                                        border = borderStroke,
                                        tonalElevation = if (isGlassmorphism) 0.dp else 3.dp,
                                        shadowElevation = if (isGlassmorphism) 8.dp else 4.dp,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = "Device is offline",
                                                tint = if (isGlassmorphism) Color.White.copy(alpha = 0.95f) else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                // Page indicator dots & Profile pill (with glassmorphism and matching search pill styling)
                                Surface(
                                    shape = RoundedCornerShape(28.dp),
                                    color = surfaceColor,
                                    border = borderStroke,
                                    tonalElevation = if (isGlassmorphism) 0.dp else 3.dp,
                                    shadowElevation = if (isGlassmorphism) 8.dp else 4.dp,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .align(Alignment.CenterEnd)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 14.dp, end = 6.dp),
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
                                                        else (if (isGlassmorphism) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                    )
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(index)
                                                        }
                                                    }
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isGlassmorphism) Color(0x33FFFFFF) else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { viewModel.openSettings() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!ytAccountInfo?.avatarUrl.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = ytAccountInfo!!.avatarUrl,
                                                    contentDescription = ytAccountInfo?.name ?: "Profile",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = "Profile",
                                                    tint = if (isGlassmorphism) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
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
                                playingTrackId = currentPlayingTrackId,
                                isPlaying = isCurrentTrackPlaying,
                                onBack = { viewModel.navigateBack() },
                                onAlbumClick = { viewModel.selectAlbum(it) },
                                onSongClick = { track, list -> viewModel.playTrack(track, list) },
                                onSongLongClick = { viewModel.openSongMenu(it) },
                                onSeeMoreSongs = { viewModel.openArtistSongsList() },
                                onPlay = { viewModel.playArtist(artist, shuffle = false) },
                                onShufflePlay = { viewModel.playArtist(artist, shuffle = true) },
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
                                playingTrackId = currentPlayingTrackId,
                                isPlaying = isCurrentTrackPlaying,
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
                                playingTrackId = currentPlayingTrackId,
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
                                isTrackDownloaded = { viewModel.isSongDownloaded(it) },
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
                                playingTrackId = currentPlayingTrackId,
                                isPlaying = isCurrentTrackPlaying,
                                onBack = { viewModel.navigateBack() },
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onTrackLongClick = { viewModel.openSongMenu(it) },
                                onPlayAll = { tracks -> viewModel.playAll(tracks) },
                                onShuffleAll = { tracks -> viewModel.shuffleAll(tracks) },
                                isTrackDownloaded = { viewModel.isSongDownloaded(it) }
                            )
                        }
                    }
                    ScreenState.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            searchResult = searchResult,
                            recentSearches = recentSearches,
                            isLoading = isSearchLoading,
                            playingTrackId = currentPlayingTrackId,
                            isPlaying = isCurrentTrackPlaying,
                            onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onAddRecentSearch = { viewModel.addRecentSearch(it) },
                            onRemoveRecentSearch = { viewModel.removeRecentSearch(it) },
                            onClearRecentSearches = { viewModel.clearRecentSearches() },
                            onArtistClick = { viewModel.selectArtist(it) },
                            onAlbumClick = { viewModel.selectAlbum(it) },
                            onSongClick = { track, list -> viewModel.playTrack(track, list) },
                            onSongLongClick = { viewModel.openSongMenu(it) },
                            onBack = { viewModel.navigateBack() },
                            isTrackDownloaded = { viewModel.isSongDownloaded(it) },
                            appTheme = appTheme
                        )
                    }
                    ScreenState.RECENTLY_PLAYED -> {
                        RecentlyPlayedScreen(
                            tracks = recentlyPlayed,
                            playingTrackId = currentPlayingTrackId,
                            isPlaying = isCurrentTrackPlaying,
                            onBack = { viewModel.navigateBack() },
                            onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                            onTrackLongClick = { viewModel.openSongMenu(it) },
                            onPlayAll = { tracks -> viewModel.playAll(tracks) },
                            onShuffleAll = { tracks -> viewModel.shuffleAll(tracks) },
                            onClearAll = {
                                viewModel.clearRecentlyPlayed()
                                Toast.makeText(context, "Recently played cleared", Toast.LENGTH_SHORT).show()
                            },
                            appTheme = appTheme
                        )
                    }
                    ScreenState.SETTINGS -> {
                        SettingsScreen(
                            currentVersion = viewModel.currentAppVersion,
                            appTheme = appTheme,
                            appThemeVariant = appThemeVariant,
                            customAccentColor = customAccentColor,
                            isCheckingUpdate = isCheckingUpdate,
                            ytAccountInfo = ytAccountInfo,
                            isSyncingYtAccount = isSyncingYtAccount,
                            autoSyncInterval = autoSyncInterval,
                            onLoginYtMusic = { isYtLoginDialogOpen = true },
                            onSyncYtMusic = { viewModel.refreshYtAccountAndPlaylists(silent = false) },
                            onLogoutYtMusic = { viewModel.logoutYtMusic() },
                            onOpenAutoSync = { viewModel.openAutoSyncSettings() },
                            onBack = { viewModel.navigateBack() },
                            onCheckForUpdates = { viewModel.checkForUpdates(manual = true) },
                            onOpenAppTheme = { viewModel.openAppThemeSettings() }
                        )
                    }
                    ScreenState.SETTINGS_AUTO_SYNC -> {
                        SettingsAutoSyncScreen(
                            selectedInterval = autoSyncInterval,
                            isSyncing = isSyncingYtAccount,
                            isLoggedIn = ytAccountInfo != null,
                            onSelectInterval = { viewModel.setAutoSyncInterval(it) },
                            onSyncNow = {
                                if (ytAccountInfo != null) {
                                    viewModel.refreshYtAccountAndPlaylists(silent = false)
                                } else {
                                    isYtLoginDialogOpen = true
                                }
                            },
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
        val isDownloaded = viewModel.isSongDownloaded(track.id)
        val downloadProgress = viewModel.getSongDownloadProgress(track.id)
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
            isDownloaded = isDownloaded,
            downloadProgress = downloadProgress,
            onDownload = { viewModel.downloadSong(track) },
            onRemoveDownload = { viewModel.removeDownloadedSong(track.id) },
            onDismiss = { viewModel.closeSongMenu() },
            isCustomImageTheme = isImageBackground,
            customThemeImagePath = customThemeImagePath,
            customThemeImageRes = themeDrawableRes,
            ambientBrush = null
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
            isCustomImageTheme = isImageBackground,
            customThemeImagePath = customThemeImagePath,
            customThemeImageRes = themeDrawableRes,
            ambientBrush = null
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
            isCustomImageTheme = isImageBackground,
            customThemeImagePath = customThemeImagePath,
            customThemeImageRes = themeDrawableRes,
            ambientBrush = null
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
            isCustomImageTheme = isImageBackground,
            customThemeImagePath = customThemeImagePath,
            customThemeImageRes = themeDrawableRes,
            ambientBrush = null
        )
    }

    // Full Player Modal Sheet
    AnimatedVisibility(
        visible = isFullPlayerOpen,
        enter = slideInVertically(
            initialOffsetY = { (it * 0.35f).toInt() },
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 1000f)
        ) + fadeIn(animationSpec = tween(120)),
        exit = slideOutVertically(
            targetOffsetY = { (it * 0.35f).toInt() },
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 1000f)
        ) + fadeOut(animationSpec = tween(100))
    ) {
        val fullPlayerState by viewModel.playbackState.collectAsState()
        val currentTrack = fullPlayerState.currentTrack
        val isCurDownloaded = currentTrack?.let { viewModel.isSongDownloaded(it.id) } ?: false
        val curDownloadProgress = currentTrack?.let { viewModel.getSongDownloadProgress(it.id) }

        FullPlayerSheet(
            state = fullPlayerState,
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
            appTheme = appTheme,
            isQueueReorderable = isQueueReorderable,
            lyricsState = lyricsState,
            onSeekToPosition = { viewModel.seekToPosition(it) },
            isDownloaded = isCurDownloaded,
            downloadProgress = curDownloadProgress,
            onToggleDownload = { track ->
                if (viewModel.isSongDownloaded(track.id)) {
                    viewModel.removeDownloadedSong(track.id)
                } else {
                    viewModel.downloadSong(track)
                }
            },
            onShareTrack = { track ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    if (track.isLocal) {
                        putExtra(Intent.EXTRA_TEXT, "Listening to ${track.title} by ${track.artist}")
                    } else {
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${track.id}")
                    }
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }
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

    // YouTube Music InnerTube Login Dialog
    if (isYtLoginDialogOpen) {
        YouTubeLoginDialog(
            onLoginSuccess = { cookie, fallbackName, fallbackAvatarUrl ->
                isYtLoginDialogOpen = false
                viewModel.onYtMusicLoginSuccess(cookie, fallbackName, fallbackAvatarUrl)
            },
            onDismiss = { isYtLoginDialogOpen = false }
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
            },
            isCustomImageTheme = isCustomImage
        )
    }
}
