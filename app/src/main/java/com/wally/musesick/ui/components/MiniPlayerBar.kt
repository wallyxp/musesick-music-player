package com.wally.musesick.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wally.musesick.model.PlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Unified interactive bottom bar providing:
 * 1. Bottom-center rectangular circular Search Bar when no track is active (~1/3+ width, centered).
 * 2. When a track starts playing, moves the search bar to the left with a sliding and bouncing animation,
 *    transforms itself into the search icon button, while the Now Playing bar slides in from the right.
 * 3. Double-tapping the Now Playing bar stops the current song, slides it back to the right, and restores
 *    the search bar to its centered resting position.
 * 4. Supports glassmorphism styling for Ambient and Custom Image themes, floating above content with visible edges.
 */
@Composable
fun BottomNavigationControlBar(
    hasActiveTrack: Boolean,
    onSearchClick: () -> Unit,
    miniPlayerState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onDoubleTapNowPlaying: () -> Unit = {},
    isGlassmorphism: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Instant direct trigger for search opening
    val triggerSearchExpansion: () -> Unit = {
        onSearchClick()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 16.dp) // Elevated position
    ) {
        val availableWidth = maxWidth
        val barHeight = 56.dp
        // Expanded search bar width (wider pill resting state)
        val searchRestingWidth = 230.dp.coerceAtMost(availableWidth - 20.dp)
        // Set searchIconSize equal to barHeight so it is ALWAYS an exact 1:1 circle
        val searchIconSize = barHeight
        val gap = 6.dp

        // Target X offset for search pill/icon:
        // When no track is active, centered: (availableWidth - searchRestingWidth) / 2
        // When track is active, slid to left: 0.dp
        val searchTargetOffsetX = if (hasActiveTrack) {
            0.dp
        } else {
            ((availableWidth - searchRestingWidth) / 2).coerceAtLeast(0.dp)
        }

        // Animated bouncing offset from center to left (and left to center)
        val searchOffsetX by animateDpAsState(
            targetValue = searchTargetOffsetX,
            animationSpec = spring(
                dampingRatio = 0.65f, // Bouncing spring!
                stiffness = 380f
            ),
            label = "search_offset_x"
        )

        // Dynamic width of the search element:
        // When no track is active: 230.dp (wider pill, centered)
        // When track is active: 56.dp (ALWAYS a 56x56 perfect circle)
        val searchWidth by animateDpAsState(
            targetValue = if (hasActiveTrack) searchIconSize else searchRestingWidth,
            animationSpec = spring(
                dampingRatio = 0.65f, // Bouncing spring!
                stiffness = 380f
            ),
            label = "search_bar_width"
        )

        // SEARCH PILL OR CIRCLE (slides left and shrinks to circular button when track active)
        SearchPillOrIcon(
            hasActiveTrack = hasActiveTrack,
            width = searchWidth,
            isGlassmorphism = isGlassmorphism,
            onClick = triggerSearchExpansion,
            modifier = Modifier.offset(x = searchOffsetX)
        )

        // NOW PLAYING BAR (Slides in from the right when active; wider player footprint)
        val nowPlayingStartOffset = searchIconSize + gap
        val nowPlayingWidth = (availableWidth - nowPlayingStartOffset).coerceAtLeast(0.dp)

        AnimatedVisibility(
            visible = hasActiveTrack,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f)
            ) + fadeIn(tween(140)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
            ) + fadeOut(tween(120)),
            modifier = Modifier
                .offset(x = nowPlayingStartOffset)
                .width(nowPlayingWidth)
        ) {
            NowPlayingPillBar(
                state = miniPlayerState,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onClick = onOpenFullPlayer,
                onDoubleTap = onDoubleTapNowPlaying,
                isGlassmorphism = isGlassmorphism,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
}

/**
 * Search Bar in either resting centered 1/3+ pill state or transformed circular button state.
 */
@Composable
fun SearchPillOrIcon(
    hasActiveTrack: Boolean,
    width: Dp,
    isGlassmorphism: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    val shape = if (hasActiveTrack && width <= 58.dp) CircleShape else RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .width(width)
            .height(56.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = surfaceColor,
        border = borderStroke,
        tonalElevation = if (isGlassmorphism) 0.dp else 3.dp,
        shadowElevation = if (isGlassmorphism) 8.dp else 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = hasActiveTrack,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "search_pill_content"
            ) { isIconOnly ->
                if (isIconOnly) {
                    // Circular icon state
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    // Standalone 1/3+ pill state
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Now Playing Bar styled as a sleek rectangular search bar (pill shaped), supporting glassmorphism and double-tap to stop.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingPillBar(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onSwipeUp: () -> Unit = onClick,
    isGlassmorphism: Boolean = false,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return
    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }
    var nextClickTrigger by remember { mutableIntStateOf(0) }
    val nextButtonScale by animateFloatAsState(
        targetValue = if (nextClickTrigger > 0) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        finishedListener = { if (nextClickTrigger > 0) nextClickTrigger = 0 },
        label = "mini_pill_next_scale"
    )

    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var pendingTapJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Ultra-responsive tap handler: reliably detects double-tap to remove miniplayer without opening full player
    val handlePillClick: () -> Unit = {
        val now = SystemClock.uptimeMillis()
        if (now - lastTapTimestamp < 260L) {
            pendingTapJob?.cancel()
            pendingTapJob = null
            lastTapTimestamp = 0L
            onDoubleTap()
        } else {
            lastTapTimestamp = now
            pendingTapJob = coroutineScope.launch {
                delay(190)
                onClick()
            }
        }
    }

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

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    verticalDragAccumulator += delta
                    if (verticalDragAccumulator < -25f) {
                        onSwipeUp()
                        verticalDragAccumulator = 0f
                    }
                },
                onDragStopped = { velocity ->
                    if (velocity < -100f || verticalDragAccumulator < -15f) {
                        onSwipeUp()
                    }
                    verticalDragAccumulator = 0f
                }
            )
            .clickable(onClick = handlePillClick),
        shape = RoundedCornerShape(28.dp),
        color = surfaceColor,
        border = borderStroke,
        tonalElevation = if (isGlassmorphism) 0.dp else 3.dp,
        shadowElevation = if (isGlassmorphism) 8.dp else 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 6.dp, end = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art thumbnail (circular pill style)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isGlassmorphism) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    val thumbUrl = track.lowResThumbnailUrl ?: track.thumbnailUrl
                    if (!thumbUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = thumbUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        NothingFormatBadge(
                            label = track.audioFormat.label,
                            isHighlighted = track.isLocal
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${track.artist} • ${track.audioFormat.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause Button
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(36.dp),
                    colors = if (isGlassmorphism) {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Next Button
                IconButton(
                    onClick = {
                        nextClickTrigger++
                        onNext()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            scaleX = nextButtonScale
                            scaleY = nextButtonScale
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Sleek dynamic progress bar along the bottom of the pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(if (isGlassmorphism) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.progressFraction)
                        .height(2.5.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Retained for backwards compatibility with any existing callers.
 */
@Composable
fun MiniPlayerBar(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit = onClick,
    modifier: Modifier = Modifier
) {
    NowPlayingPillBar(
        state = state,
        onTogglePlayPause = onTogglePlayPause,
        onNext = onNext,
        onClick = onClick,
        onDoubleTap = {},
        onSwipeUp = onSwipeUp,
        isGlassmorphism = false,
        modifier = modifier
    )
}
