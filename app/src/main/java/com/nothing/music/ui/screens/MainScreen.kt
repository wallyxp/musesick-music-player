package com.nothing.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import com.nothing.music.ui.MusicViewModel
import com.nothing.music.ui.ScreenState
import com.nothing.music.ui.components.ExistingPlaylistSheet
import com.nothing.music.ui.components.MiniPlayerBar
import com.nothing.music.ui.components.NewPlaylistSheet
import com.nothing.music.ui.components.SongActionMenuSheet
import com.nothing.music.ui.screens.PlaylistDetailScreen
import com.nothing.music.ui.screens.PlaylistsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsState()
    val isArtistManagerOpen by viewModel.isArtistManagerOpen.collectAsState()

    // Artists & Search State
    val artists by viewModel.artists.collectAsState()
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
    val isExistingPlaylistSheetOpen by viewModel.isExistingPlaylistSheetOpen.collectAsState()
    val isNewPlaylistSheetOpen by viewModel.isNewPlaylistSheetOpen.collectAsState()

    BackHandler(enabled = isFullPlayerOpen || isArtistManagerOpen || contextMenuTrack != null || isExistingPlaylistSheetOpen || isNewPlaylistSheetOpen || currentScreen != ScreenState.HOME) {
        when {
            contextMenuTrack != null -> viewModel.closeSongMenu()
            isExistingPlaylistSheetOpen -> viewModel.closeExistingPlaylistSheet()
            isNewPlaylistSheetOpen -> viewModel.closeNewPlaylistSheet()
            isFullPlayerOpen -> viewModel.closeFullPlayer()
            isArtistManagerOpen -> viewModel.closeArtistManager()
            else -> viewModel.navigateBack()
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
                // Material You App Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Music",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Mode Selector Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedTab == 0,
                                onClick = { viewModel.setTab(0) },
                                label = { Text("Stream") },
                                shape = RoundedCornerShape(20.dp)
                            )
                            FilterChip(
                                selected = selectedTab == 1,
                                onClick = { viewModel.setTab(1) },
                                label = { Text("Device") },
                                shape = RoundedCornerShape(20.dp)
                            )
                            FilterChip(
                                selected = selectedTab == 2,
                                onClick = { viewModel.setTab(2) },
                                label = { Text("Playlists") },
                                shape = RoundedCornerShape(20.dp)
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
                        when (selectedTab) {
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
                            1 -> LocalTab(
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
                            2 -> PlaylistsTab(
                                playlists = playlists,
                                onCreatePlaylistClick = { viewModel.openNewPlaylistSheet() },
                                onPlaylistClick = { viewModel.selectPlaylist(it) }
                            )
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
                                onTrackLongClick = { viewModel.openSongMenu(it) },
                                onPlayAll = { tracks -> viewModel.playAll(tracks) },
                                onShuffleAll = { tracks -> viewModel.shuffleAll(tracks) }
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
                }
            }
        }
    }

    // Artist Manager Bottom Sheet
    if (isArtistManagerOpen) {
        ArtistManagerSheet(
            artists = artists,
            onToggleArtist = { viewModel.toggleArtistVisibility(it) },
            onAddArtist = { viewModel.addCustomArtist(it) },
            onRemoveArtist = { viewModel.removeCustomArtist(it) },
            onDismiss = { viewModel.closeArtistManager() }
        )
    }

    // Song Action Context Menu Sheet (Long press)
    contextMenuTrack?.let { track ->
        SongActionMenuSheet(
            track = track,
            onPlayNow = { viewModel.playTrack(track) },
            onPlayNext = { viewModel.playNext(track) },
            onAddToQueue = { viewModel.addToQueue(track) },
            onAddToExistingPlaylist = { viewModel.openExistingPlaylistSheet() },
            onAddToNewPlaylist = { viewModel.openNewPlaylistSheet() },
            onDismiss = { viewModel.closeSongMenu() }
        )
    }

    // Existing Playlist Picker Sheet
    if (isExistingPlaylistSheetOpen && contextMenuTrack != null) {
        ExistingPlaylistSheet(
            playlists = playlists,
            track = contextMenuTrack!!,
            onSelectPlaylist = { playlist -> viewModel.addTrackToPlaylist(playlist.id, contextMenuTrack!!) },
            onCreateNewClick = { viewModel.openNewPlaylistSheet() },
            onDismiss = { viewModel.closeExistingPlaylistSheet() }
        )
    }

    // New Playlist Creation Sheet
    if (isNewPlaylistSheetOpen) {
        NewPlaylistSheet(
            initialTrack = contextMenuTrack,
            onCreatePlaylist = { title, uri -> viewModel.createPlaylist(title, uri, contextMenuTrack) },
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
            onTrackLongClick = { viewModel.openSongMenu(it) },
            onClose = { viewModel.closeFullPlayer() }
        )
    }
}
