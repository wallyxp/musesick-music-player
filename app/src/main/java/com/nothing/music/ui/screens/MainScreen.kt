package com.nothing.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.music.ui.MusicViewModel
import com.nothing.music.ui.components.MiniPlayerBar
import com.nothing.music.ui.theme.NothingBorder
import com.nothing.music.ui.theme.NothingCard
import com.nothing.music.ui.theme.NothingRed
import com.nothing.music.ui.theme.NothingTextMuted
import com.nothing.music.ui.theme.NothingTextPrimary

@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsState()

    val ytTracks by viewModel.ytTracks.collectAsState()
    val isYtLoading by viewModel.isYtLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val localTracks by viewModel.localTracks.collectAsState()
    val isLocalLoading by viewModel.isLocalLoading.collectAsState()
    val localFilter by viewModel.localFormatFilter.collectAsState()

    BackHandler(enabled = isFullPlayerOpen) {
        viewModel.closeFullPlayer()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        containerColor = Color.Black,
        topBar = {
            // Nothing OS Top Header Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo with Red Indicator Dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (playbackState.isPlaying) NothingRed else NothingTextMuted)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MUSIC",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = NothingTextPrimary
                        )
                    }

                    // Mode Switcher: [ STREAM ] | [ LOCAL ]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NothingCard)
                            .border(1.dp, NothingBorder, RoundedCornerShape(20.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabPill(
                            label = "STREAM",
                            isSelected = selectedTab == 0,
                            onClick = { viewModel.setTab(0) }
                        )
                        TabPill(
                            label = "LOCAL",
                            isSelected = selectedTab == 1,
                            onClick = { viewModel.setTab(1) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Floating MiniPlayerBar
            if (playbackState.currentTrack != null) {
                MiniPlayerBar(
                    state = playbackState,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextTrack() },
                    onClick = { viewModel.openFullPlayer() }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> StreamTab(
                    tracks = ytTracks,
                    isLoading = isYtLoading,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    playbackState = playbackState,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onCategorySelect = { viewModel.loadCategory(it) },
                    onTrackSelect = { track, list -> viewModel.playTrack(track, list) }
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
                    onTrackSelect = { track, list -> viewModel.playTrack(track, list) }
                )
            }
        }
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
            onClose = { viewModel.closeFullPlayer() }
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
            color = if (isSelected) Color.Black else NothingTextMuted
        )
    }
}

