package com.wally.musesick.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.wally.musesick.ui.MusicViewModel
import com.wally.musesick.ui.ScreenState
import com.wally.musesick.ui.components.ExistingPlaylistSheet
import com.wally.musesick.ui.components.MiniPlayerBar
import com.wally.musesick.ui.components.NewPlaylistSheet
import com.wally.musesick.ui.components.SongActionMenuSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val queue by viewModel.queue.collectAsState()
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

    // Song Context Menu State
    val contextMenuTrack by viewModel.contextMenuTrack.collectAsState()
    val targetTrackForPlaylist by viewModel.targetTrackForPlaylist.collectAsState()
    val isExistingPlaylistSheetOpen by viewModel.isExistingPlaylistSheetOpen.collectAsState()
    val isNewPlaylistSheetOpen by viewModel.isNewPlaylistSheetOpen.collectAsState()

    // Swipeable pages: Stream (0), Playlist (1), Local (2)
    val pageTitles = remember { listOf("Stream", "Playlist", "Local") }
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

    BackHandler(enabled = isFullPlayerOpen || contextMenuTrack != null || isExistingPlaylistSheetOpen || isNewPlaylistSheetOpen || currentScreen != ScreenState.HOME || (currentScreen == ScreenState.HOME && pagerState.currentPage != 0)) {
        when {
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
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

                    // Minimalist Nothing-style page indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    }
                }
            }
        },
        bottomBar = {
            if (playbackState.currentTrack != null && !isFullPlayerOpen) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    MiniPlayerBar(
                        state = playbackState,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.nextTrack() },
                        onClick = { viewModel.openFullPlayer() }
                    )
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
                                0 -> StreamTab(
                                    artists = artists,
                                    searchResult = searchResult,
                                    isLoading = isSearchLoading,
                                    searchQuery = searchQuery,
                                    playbackState = playbackState,
                                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                                    onArtistClick = { viewModel.selectArtist(it) },
                                    onAlbumClick = { viewModel.selectAlbum(it) },
                                    onSongClick = { track, list -> viewModel.playTrack(track, list) },
                                    onSongLongClick = { viewModel.openSongMenu(it) },
                                    onChangeArtistsClick = { viewModel.openArtistManager() }
                                )
                                1 -> PlaylistsTab(
                                    playlists = playlists,
                                    onCreatePlaylistClick = { viewModel.openNewPlaylistSheet() },
                                    onPlaylistClick = { viewModel.selectPlaylist(it) }
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
                                onSongLongClick = { viewModel.openSongMenu(it) }
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
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onTrackLongClick = { viewModel.openSongMenu(it) },
                                onPlayAll = { tracks -> viewModel.playAll(tracks) },
                                onShuffleAll = { tracks -> viewModel.shuffleAll(tracks) },
                                onRemoveTrack = { track -> viewModel.removeTrackFromPlaylist(playlist.id, track.id) },
                                onDeletePlaylist = { viewModel.deletePlaylist(playlist.id) }
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
            onDismiss = { viewModel.closeSongMenu() }
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
            onDismiss = { viewModel.closeExistingPlaylistSheet() }
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
            onDismiss = { viewModel.closeNewPlaylistSheet() }
        )
    }

    // Full Player Modal Sheet
    AnimatedVisibility(
        visible = isFullPlayerOpen,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
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
            onClose = { viewModel.closeFullPlayer() }
        )
    }
}
