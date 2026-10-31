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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import com.wally.musesick.R
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.ui.theme.LocalAppTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wally.musesick.model.PlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Unified interactive bottom bar providing:
 * 1. Bottom-center rectangular circular Search Bar when no track is active (~1/3+ width, centered).
 * 2. When a track starts playing, moves the search bar to the left with a sliding and bouncing animation,
 *    transforms itself into the circular search button, while the Now Playing bar and the external Source Icon
 *    slide in to form a centered 3-element control bar: [Search] [NowPlaying] [Source].
 * 3. Upon closing the miniplayer (triple tap), only the centered search bar remains.
 * 4. Height has been increased on top and bottom for both the miniplayer and search bar (64dp).
 */
@Composable
fun BottomNavigationControlBar(
    hasActiveTrack: Boolean,
    onSearchClick: () -> Unit,
    miniPlayerState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    onOpenFullPlayer: () -> Unit,
    onTripleTapNowPlaying: () -> Unit = {},
    onDoubleTapNowPlaying: () -> Unit = onTripleTapNowPlaying,
    isGlassmorphism: Boolean = false,
    modifier: Modifier = Modifier
) {
    val triggerSearchExpansion: () -> Unit = {
        onSearchClick()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 16.dp)
    ) {
        val availableWidth = maxWidth
        // Increased height on top and bottom: 64dp
        val barHeight = 64.dp
        val iconButtonSize = barHeight // Perfect 64x64 circle
        val gap = 8.dp

        // Miniplayer width matches search resting width
        val miniPlayerWidth = 220.dp.coerceAtMost(availableWidth - (iconButtonSize * 2 + gap * 2 + 16.dp))
        val searchRestingWidth = miniPlayerWidth
        val totalActiveWidth = iconButtonSize + gap + miniPlayerWidth + gap + iconButtonSize

        val groupTargetOffsetX = ((availableWidth - totalActiveWidth) / 2).coerceAtLeast(0.dp)
        val searchRestingOffsetX = ((availableWidth - searchRestingWidth) / 2).coerceAtLeast(0.dp)

        val searchTargetOffsetX = if (hasActiveTrack) groupTargetOffsetX else searchRestingOffsetX

        // Animated bouncing offset from center to left (and left to center)
        val searchOffsetX by animateDpAsState(
            targetValue = searchTargetOffsetX,
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = 380f
            ),
            label = "search_offset_x"
        )

        // Dynamic width of the search element:
        // When no track is active: resting width (wider pill, centered)
        // When track is active: iconButtonSize (exact circle)
        val searchWidth by animateDpAsState(
            targetValue = if (hasActiveTrack) iconButtonSize else searchRestingWidth,
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = 380f
            ),
            label = "search_bar_width"
        )

        // 1. SEARCH PILL OR CIRCLE (slides left and shrinks to circular button when track active)
        SearchPillOrIcon(
            hasActiveTrack = hasActiveTrack,
            width = searchWidth,
            height = barHeight,
            isGlassmorphism = isGlassmorphism,
            onClick = triggerSearchExpansion,
            modifier = Modifier.graphicsLayer { translationX = searchOffsetX.toPx() }
        )

        // 2. NOW PLAYING BAR (in the middle between search icon and source icon)
        val nowPlayingStartOffset = searchOffsetX + iconButtonSize + gap

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
                .graphicsLayer { translationX = nowPlayingStartOffset.toPx() }
                .width(miniPlayerWidth)
        ) {
            NowPlayingPillBar(
                state = miniPlayerState,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onClick = onOpenFullPlayer,
                onTripleTap = onTripleTapNowPlaying,
                isGlassmorphism = isGlassmorphism,
                modifier = Modifier
                    .width(miniPlayerWidth)
                    .height(barHeight)
            )
        }

        // 3. SOURCE ICON BUTTON (outside the miniplayer on the right side)
        val sourceStartOffset = searchOffsetX + iconButtonSize + gap + miniPlayerWidth + gap

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
            modifier = Modifier.graphicsLayer { translationX = sourceStartOffset.toPx() }
        ) {
            val track = miniPlayerState.currentTrack
            SourceIconButton(
                isLocal = track?.isLocal == true,
                size = iconButtonSize,
                isGlassmorphism = isGlassmorphism,
                onClick = {},
                modifier = Modifier.size(iconButtonSize)
            )
        }
    }
}

/**
 * Search Bar in either resting centered pill state or transformed circular button state.
 */
@Composable
fun SearchPillOrIcon(
    hasActiveTrack: Boolean,
    width: Dp,
    height: Dp = 64.dp,
    isGlassmorphism: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppTheme.current
    val isAqua = appTheme == AppTheme.AQUA

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

    val shape = if (hasActiveTrack && width <= (height + 2.dp)) CircleShape else RoundedCornerShape(32.dp)

    Surface(
        modifier = modifier
            .width(width)
            .height(height)
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
                        tint = if (isAqua) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    if (isAqua) {
                        // Aqua theme: Search writing centered in the middle of the pill with Crying Aqua peeking
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            Image(
                                painter = painterResource(id = R.drawable.aqua_search_crying),
                                contentDescription = null,
                                modifier = Modifier
                                    .height(48.dp)
                                    .align(Alignment.BottomStart)
                                    .padding(start = 14.dp),
                                contentScale = ContentScale.FillHeight
                            )
                        }
                    } else {
                        // Standalone pill state
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.titleMedium,
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
}

/**
 * Standalone circular indicator button on the right side outside the miniplayer,
 * indicating track source (Local phone storage vs Cloud stream).
 */
@Composable
fun SourceIconButton(
    isLocal: Boolean,
    size: Dp = 64.dp,
    isGlassmorphism: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppTheme.current
    val isAqua = appTheme == AppTheme.AQUA

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
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = surfaceColor,
        border = borderStroke,
        tonalElevation = if (isGlassmorphism) 0.dp else 3.dp,
        shadowElevation = if (isGlassmorphism) 8.dp else 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLocal) Icons.Default.PhoneAndroid else Icons.Default.Cloud,
                contentDescription = if (isLocal) "Local audio" else "Cloud stream",
                tint = if (isAqua) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Now Playing Bar styled as a sleek rectangular pill matching the search bar size:
 * - Album art covers the entirety of the miniplayer background
 * - Initial state: Song title in marquee text with artist name below it
 * - Single tap / slide up: Opens the fullscreen player as usual
 * - Double tap: Toggles/brings out the naked song controls (Previous, Play/Pause, Next) without quitting/stopping
 * - Triple tap: Closes the player / stops the song
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingPillBar(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    onClick: () -> Unit,
    onTripleTap: () -> Unit = {},
    onSwipeUp: () -> Unit = onClick,
    isGlassmorphism: Boolean = false,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return
    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

    // Toggle controls mode (double-tap brings out controls; track change resets to song title marquee)
    var showControls by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        showControls = false
    }

    var tapCount by remember { mutableIntStateOf(0) }
    var pendingTapJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Multi-tap detector: 1 tap -> open full player, 2 taps -> show controls, 3 taps -> stop song
    val handlePillTap: () -> Unit = {
        pendingTapJob?.cancel()
        val nextCount = tapCount + 1
        tapCount = nextCount

        if (nextCount >= 3) {
            // Triple tap immediately stops playback and closes miniplayer
            tapCount = 0
            pendingTapJob = null
            onTripleTap()
        } else {
            pendingTapJob = coroutineScope.launch {
                delay(190)
                val finalCount = tapCount
                tapCount = 0
                when (finalCount) {
                    1 -> onClick()
                    2 -> showControls = !showControls
                }
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

    val borderStroke = if (isGlassmorphism) {
        glassBorder
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
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
            .clickable(onClick = handlePillTap),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF181818),
        border = borderStroke,
        tonalElevation = 0.dp,
        shadowElevation = if (isGlassmorphism) 8.dp else 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Album art covers the entirety of the miniplayer background
            val artworkUrl = track.lowResThumbnailUrl ?: track.thumbnailUrl ?: track.highResThumbnailUrl
            if (!artworkUrl.isNullOrEmpty()) {
                val context = LocalContext.current
                val imageRequest = remember(artworkUrl) {
                    ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .size(300, 100)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF222222))
                )
            }

            // Dark scrim overlay for high-contrast readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )

            // 2. Animated Content: Initial Marquee Title/Artist vs Double-tap Controls
            AnimatedContent(
                targetState = showControls,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "mini_pill_mode",
                modifier = Modifier.fillMaxSize()
            ) { isControlsMode ->
                if (isControlsMode) {
                    // Controls Mode: Previous, Play/Pause, Next (naked icons without glassmorphism circles)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.size(46.dp)
                        ) {
                            if (state.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                } else {
                    // Initial Info Mode: Marquee Title on top, Artist name below it
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.82f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Dynamic progress bar along the bottom edge of the pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.progressFraction)
                        .height(2.5.dp)
                        .background(Color.White)
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
    onPrevious: () -> Unit = {},
    onClick: () -> Unit,
    onSwipeUp: () -> Unit = onClick,
    modifier: Modifier = Modifier
) {
    NowPlayingPillBar(
        state = state,
        onTogglePlayPause = onTogglePlayPause,
        onNext = onNext,
        onPrevious = onPrevious,
        onClick = onClick,
        onTripleTap = {},
        onSwipeUp = onSwipeUp,
        isGlassmorphism = false,
        modifier = modifier
    )
}
