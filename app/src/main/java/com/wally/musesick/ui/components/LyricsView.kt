package com.wally.musesick.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wally.musesick.model.LyricLine
import com.wally.musesick.model.LyricsUiState
import kotlinx.coroutines.delay

/**
 * Scrollable synchronized lyrics container component.
 *
 * Connects to the player's audio playback time state:
 * - [currentTimeMs] should receive [com.wally.musesick.model.PlaybackState.currentPositionMs].
 * - Highlights the active line based on parsed timestamps.
 * - Auto-scrolls the active lyric line to the vertical center of the view.
 * - Pauses auto-scrolling temporarily when the user manually scrolls, then resumes.
 * - Allows tapping any lyric line to seek directly to that timestamp via [onSeekTo].
 * - Handles edge cases: instrumental gaps, instrumental tracks, and missing lyrics.
 */
@Composable
fun LyricsView(
    lyricsState: LyricsUiState,
    currentTimeMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (lyricsState) {
            is LyricsUiState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = textColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading lyrics...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.75f)
                    )
                }
            }

            is LyricsUiState.Instrumental -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Instrumental",
                        tint = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Instrumental",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This track contains no vocal lyrics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            is LyricsUiState.Empty -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Lyrics not available",
                        tint = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = lyricsState.message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No synchronized lyrics found for this track",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            is LyricsUiState.Idle -> {
                // Initial idle state before load begins
                Box(modifier = Modifier.fillMaxSize())
            }

            is LyricsUiState.Success -> {
                SynchronizedLyricsList(
                    lyrics = lyricsState.lyrics,
                    currentTimeMs = currentTimeMs,
                    onSeekTo = onSeekTo,
                    textColor = textColor
                )
            }
        }
    }
}

@Composable
private fun SynchronizedLyricsList(
    lyrics: List<LyricLine>,
    currentTimeMs: Long,
    onSeekTo: (Long) -> Unit,
    textColor: Color
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Calculate active line index based on player's current audio time state
    val activeIndex = remember(lyrics, currentTimeMs) {
        lyrics.indexOfLast { it.time <= currentTimeMs }
    }

    // User drag detection: only pause when user physically touches and drags the list
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var userPausedAutoScroll by remember { mutableStateOf(false) }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            userPausedAutoScroll = true
        } else if (userPausedAutoScroll) {
            // User finished manual dragging; wait 3 seconds before auto-scroll resumes
            delay(3000L)
            userPausedAutoScroll = false
        }
    }

    // Auto-scroll the active lyric line smoothly
    LaunchedEffect(activeIndex, userPausedAutoScroll) {
        if (!userPausedAutoScroll && activeIndex in lyrics.indices) {
            if (activeIndex == 0) {
                listState.animateScrollToItem(0, 0)
            } else {
                // If layoutInfo is not ready yet, wait briefly
                if (listState.layoutInfo.visibleItemsInfo.isEmpty()) {
                    delay(40L)
                }

                val layoutInfo = listState.layoutInfo
                val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == activeIndex }
                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

                if (visibleItem != null && viewportHeight > 0) {
                    // Position the active line at ~36% of screen height from the top
                    val targetY = (viewportHeight * 0.36f).toInt()
                    val scrollDelta = visibleItem.offset - targetY
                    if (kotlin.math.abs(scrollDelta) > 4) {
                        listState.animateScrollBy(
                            scrollDelta.toFloat(),
                            animationSpec = tween(durationMillis = 350)
                        )
                    }
                } else {
                    // Line is not currently visible (e.g. lyrics just opened or track seeked),
                    // scroll nearby with 1 leading line, then refine position
                    val targetIndex = (activeIndex - 1).coerceAtLeast(0)
                    listState.scrollToItem(targetIndex, 0)
                    delay(30L)
                    val updatedLayout = listState.layoutInfo
                    val updatedItem = updatedLayout.visibleItemsInfo.find { it.index == activeIndex }
                    val updatedViewport = updatedLayout.viewportEndOffset - updatedLayout.viewportStartOffset
                    if (updatedItem != null && updatedViewport > 0) {
                        val targetY = (updatedViewport * 0.36f).toInt()
                        val scrollDelta = updatedItem.offset - targetY
                        if (kotlin.math.abs(scrollDelta) > 4) {
                            listState.animateScrollBy(
                                scrollDelta.toFloat(),
                                animationSpec = tween(durationMillis = 350)
                            )
                        }
                    }
                }
            }
        }
    }

    // Instrumental gap detection between current line and next line
    val isInstrumentalBreak = remember(activeIndex, currentTimeMs, lyrics) {
        if (activeIndex in 0 until lyrics.lastIndex) {
            val currentLineTime = lyrics[activeIndex].time
            val nextLineTime = lyrics[activeIndex + 1].time
            val gap = nextLineTime - currentLineTime
            // If the gap to the next line is > 10s and we're 5s past the line start, mark as instrumental gap
            gap > 10_000L && (currentTimeMs - currentLineTime > 5_000L) && (nextLineTime - currentTimeMs > 2_500L)
        } else false
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = 180.dp,
            start = 24.dp,
            end = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(lyrics, key = { index, item -> "${item.time}_$index" }) { index, line ->
            val isActive = index == activeIndex
            val isPassed = index < activeIndex

            // High-contrast highlighting for active line vs dimmed inactive lines
            val alpha by animateFloatAsState(
                targetValue = when {
                    isActive -> if (isInstrumentalBreak) 0.75f else 1f
                    isPassed -> 0.45f
                    else -> 0.35f
                },
                animationSpec = tween(300),
                label = "lyric_alpha_$index"
            )

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.04f else 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                label = "lyric_scale_$index"
            )

            val fontSize = if (isActive) 23.sp else 18.sp
            val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSeekTo(line.time) }
                    )
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = line.text,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    lineHeight = if (isActive) 32.sp else 26.sp,
                    color = textColor.copy(alpha = alpha),
                    textAlign = TextAlign.Start
                )

                // Smooth instrumental gap indicator under active line
                if (isActive && isInstrumentalBreak) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "♪  ♪  ♪",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor.copy(alpha = 0.65f),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "(Instrumental)",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}
