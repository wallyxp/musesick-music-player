package com.wally.musesick.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import com.wally.musesick.R
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.AudioFormat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import com.wally.musesick.model.LyricsUiState
import com.wally.musesick.ui.components.LyricsView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wally.musesick.model.PlaybackState
import com.wally.musesick.model.Track
import com.wally.musesick.ui.components.NothingFormatBadge

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex

import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale

private data class DraggableQueueItem(
    val sessionKey: String,
    val track: Track
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPlayerSheet(
    state: PlaybackState,
    queue: List<Track>,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onTrackSelect: (Track) -> Unit,
    onReorderQueue: (Int, Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onTrackLongClick: (Track) -> Unit = {},
    onTrackMenuClick: (Track) -> Unit = onTrackLongClick,
    onClose: () -> Unit,
    isQueueReorderable: Boolean = true,
    lyricsState: LyricsUiState = LyricsUiState.Idle,
    onSeekToPosition: (Long) -> Unit = {},
    onShareTrack: (Track) -> Unit = {},
    isDownloaded: Boolean = false,
    downloadProgress: Float? = null,
    onToggleDownload: (Track) -> Unit = {},
    appTheme: AppTheme = AppTheme.DEFAULT,
    onUpdateQueue: ((List<Track>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    val canReorder = isQueueReorderable && queue.size > 1
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var heldIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingQueueActive by remember { mutableStateOf(false) }

    val localQueue = remember { mutableStateListOf<DraggableQueueItem>() }
    LaunchedEffect(queue) {
        if (!isDraggingQueueActive) {
            localQueue.clear()
            localQueue.addAll(queue.mapIndexed { idx, t ->
                DraggableQueueItem(sessionKey = "${t.id}_$idx", track = t)
            })
        }
    }

    val queueListState = rememberLazyListState()
    val density = LocalDensity.current
    var itemHeightPx by remember { mutableFloatStateOf(with(density) { 68.dp.toPx() }) }

    LaunchedEffect(isDraggingQueueActive) {
        if (isDraggingQueueActive) {
            while (isActive && isDraggingQueueActive) {
                val dragIdx = draggingIndex
                if (dragIdx != null) {
                    val itemInfo = queueListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == dragIdx }
                    if (itemInfo != null) {
                        itemHeightPx = itemInfo.size.toFloat().coerceAtLeast(40f)
                        val viewportHeight = queueListState.layoutInfo.viewportSize.height
                        val currentY = itemInfo.offset + dragOffsetY
                        val topThreshold = 100f
                        val bottomThreshold = viewportHeight - 120f
                        if (currentY < topThreshold) {
                            val scroll = ((topThreshold - currentY) / 8f).coerceIn(4f, 25f)
                            queueListState.scrollBy(-scroll)
                        } else if (currentY > bottomThreshold) {
                            val scroll = ((currentY - bottomThreshold) / 8f).coerceIn(4f, 25f)
                            queueListState.scrollBy(scroll)
                        }
                    }
                }
                delay(16)
            }
        }
    }

    var verticalSwipeAccumulator by remember { mutableFloatStateOf(0f) }

    var prevClickTrigger by remember { mutableIntStateOf(0) }
    val prevButtonScale by animateFloatAsState(
        targetValue = if (prevClickTrigger > 0) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        finishedListener = { if (prevClickTrigger > 0) prevClickTrigger = 0 },
        label = "full_prev_scale"
    )

    var nextClickTrigger by remember { mutableIntStateOf(0) }
    val nextButtonScale by animateFloatAsState(
        targetValue = if (nextClickTrigger > 0) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        finishedListener = { if (nextClickTrigger > 0) nextClickTrigger = 0 },
        label = "full_next_scale"
    )

    BackHandler(enabled = showQueue || showLyrics) {
        if (showLyrics) {
            showLyrics = false
        } else {
            showQueue = false
        }
    }

    val highResCover = track.highResThumbnailUrl ?: track.thumbnailUrl

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (!showQueue && !showLyrics) {
                    Modifier.draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            verticalSwipeAccumulator += delta
                            if (verticalSwipeAccumulator > 45f) {
                                verticalSwipeAccumulator = 0f
                                onClose()
                            }
                        },
                        onDragStopped = { velocity ->
                            if (velocity > 350f || verticalSwipeAccumulator > 25f) {
                                verticalSwipeAccumulator = 0f
                                onClose()
                            } else {
                                verticalSwipeAccumulator = 0f
                            }
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // Fullscreen Album Art Background with crossfade
        AnimatedContent(
            targetState = track.id,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "fullscreen_bg_art"
        ) { _ ->
            if (!highResCover.isNullOrEmpty()) {
                AsyncImage(
                    model = highResCover,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF161616)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Top Black Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Black Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(440.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.92f),
                            Color.Black
                        )
                    )
                )
        )

        // Main Player UI Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                // Circular glassmorphism Close button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.25f)
                                )
                            ),
                            CircleShape
                        )
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        modifier = Modifier.size(26.dp),
                        tint = Color.White
                    )
                }

                // NOW PLAYING circular rectangular (capsule/pill) glassmorphic background - Center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.25f)
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color.White
                    )
                }

                // Source indicator: Cloud icon for YT stream, Phone icon for local song
                val isStream = !track.isLocal && track.audioFormat == AudioFormat.YOUTUBE
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.25f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isStream) Icons.Default.Cloud else Icons.Default.PhoneAndroid,
                        contentDescription = if (isStream) "YouTube Stream" else "Local On Device",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Track Title & Artist with smooth slide & crossfade
            AnimatedContent(
                targetState = track.id,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(220)))
                },
                label = "full_track_info_transition",
                modifier = Modifier.fillMaxWidth()
            ) { _ ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Scrubber Slider
            var sliderPosition by remember { mutableFloatStateOf(-1f) }
            val currentProgress = if (sliderPosition >= 0f) sliderPosition else state.progressFraction

            if (appTheme == AppTheme.AQUA) {
                AquaPlayerSeekbar(
                    progress = currentProgress,
                    onSeek = { sliderPosition = it },
                    onSeekFinished = {
                        if (sliderPosition >= 0f) {
                            onSeek(sliderPosition)
                            sliderPosition = -1f
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Slider(
                    value = currentProgress,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        if (sliderPosition >= 0f) {
                            onSeek(sliderPosition)
                            sliderPosition = -1f
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                    )
                )
            }

            // Timestamp Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = state.formattedPosition,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = state.formattedDuration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffle) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                }

                // Previous (Circular Glassmorphism)
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape)
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = prevButtonScale
                            scaleY = prevButtonScale
                        }
                        .clip(CircleShape)
                        .background(Color(0x3D1A1A1A))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.32f)
                                )
                            ),
                            CircleShape
                        )
                        .clickable {
                            prevClickTrigger++
                            onPrevious()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Big Circular Play/Pause with morphing animation & buffering state
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    AnimatedContent(
                        targetState = Pair(state.isPlaying, state.isBuffering),
                        transitionSpec = {
                            (scaleIn(animationSpec = spring(dampingRatio = 0.62f, stiffness = 450f)) + fadeIn(tween(160)))
                                .togetherWith(scaleOut(animationSpec = spring(dampingRatio = 0.62f, stiffness = 450f)) + fadeOut(tween(160)))
                        },
                        label = "full_play_pause_transition"
                    ) { (isPlaying, isBuffering) ->
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.Black,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Next (Circular Glassmorphism)
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape)
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = nextButtonScale
                            scaleY = nextButtonScale
                        }
                        .clip(CircleShape)
                        .background(Color(0x3D1A1A1A))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.32f)
                                )
                            ),
                            CircleShape
                        )
                        .clickable {
                            nextClickTrigger++
                            onNext()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.isRepeat) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4 Circular Glassmorphism Action Buttons at bottom (Lyrics, Queue, Share, Download)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val glassBorder = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.32f)
                    )
                )

                // 1. Lyrics toggle (Circular Glassmorphism)
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x3D1A1A1A))
                        .border(1.dp, glassBorder, CircleShape)
                        .clickable { showLyrics = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = "Lyrics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. Queue toggle (Circular Glassmorphism)
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x3D1A1A1A))
                        .border(1.dp, glassBorder, CircleShape)
                        .clickable { showQueue = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue (${queue.size})",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 3. Share button (Circular Glassmorphism)
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x3D1A1A1A))
                        .border(1.dp, glassBorder, CircleShape)
                        .clickable { onShareTrack(track) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 4. Download offline toggle (Circular Glassmorphism)
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDownloaded) Color(0xFFD71921).copy(alpha = 0.28f)
                            else Color(0x3D1A1A1A)
                        )
                        .border(
                            1.dp,
                            if (isDownloaded) Brush.linearGradient(
                                listOf(
                                    Color(0xFFFF5252).copy(alpha = 0.6f),
                                    Color(0xFFD71921).copy(alpha = 0.3f),
                                    Color(0xFFFF5252).copy(alpha = 0.5f)
                                )
                            ) else glassBorder,
                            CircleShape
                        )
                        .clickable { onToggleDownload(track) },
                    contentAlignment = Alignment.Center
                ) {
                    if (downloadProgress != null) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    } else if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded for offline listening",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download for offline listening",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

            // Fullscreen Sliding Queue Window Overlay
            AnimatedVisibility(
                visible = showQueue,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200)),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(30f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { showQueue = false }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Close Queue",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = "QUEUE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = Color.White
                            )

                            TextButton(onClick = { showQueue = false }) {
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Up Next (${localQueue.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            if (canReorder) {
                                Text(
                                    text = "Hold handle to reorder",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            state = queueListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(localQueue, key = { _, itemWrapper -> itemWrapper.sessionKey }) { index, itemWrapper ->
                                val item = itemWrapper.track
                                val isCurrent = item.id == track.id
                                val isDragging = draggingIndex == index
                                val isTargeted = isDragging || (heldIndex == index)

                                val zoomScale by animateFloatAsState(
                                    targetValue = if (isTargeted) 1.05f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "queue_card_zoom"
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (!isDragging) Modifier.animateItem() else Modifier)
                                        .zIndex(if (isTargeted) 20f else 1f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragOffsetY else 0f
                                            scaleX = zoomScale
                                            scaleY = zoomScale
                                        }
                                        .then(
                                            if (isTargeted) Modifier.border(
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) else Modifier
                                        )
                                        .clickable { onTrackSelect(item) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isTargeted) Color(0xFF262626) else Color.Transparent,
                                    tonalElevation = if (isTargeted) 6.dp else 0.dp,
                                    shadowElevation = if (isTargeted) 8.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = if (canReorder) 4.dp else 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (canReorder) {
                                            // Hold and Drag Handle with multi-slot continuous drag gesture
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .pointerInput(itemWrapper.sessionKey) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                val currentIdx = localQueue.indexOfFirst { it.sessionKey == itemWrapper.sessionKey }
                                                                if (currentIdx != -1) {
                                                                    isDraggingQueueActive = true
                                                                    heldIndex = currentIdx
                                                                    draggingIndex = currentIdx
                                                                    dragOffsetY = 0f
                                                                }
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsetY += dragAmount.y
                                                                val currentDragIdx = draggingIndex ?: return@detectDragGestures
                                                                val deltaSlots = kotlin.math.round(dragOffsetY / itemHeightPx).toInt()
                                                                if (deltaSlots != 0) {
                                                                    val targetIdx = (currentDragIdx + deltaSlots).coerceIn(0, localQueue.lastIndex)
                                                                    if (targetIdx != currentDragIdx) {
                                                                        val moved = localQueue.removeAt(currentDragIdx)
                                                                        localQueue.add(targetIdx, moved)
                                                                        dragOffsetY -= (targetIdx - currentDragIdx) * itemHeightPx
                                                                        draggingIndex = targetIdx
                                                                        heldIndex = targetIdx
                                                                    }
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                val finalQueue = localQueue.map { it.track }
                                                                heldIndex = null
                                                                draggingIndex = null
                                                                dragOffsetY = 0f
                                                                isDraggingQueueActive = false
                                                                onUpdateQueue?.invoke(finalQueue) ?: onReorderQueue(0, 0)
                                                            },
                                                            onDragCancel = {
                                                                val finalQueue = localQueue.map { it.track }
                                                                heldIndex = null
                                                                draggingIndex = null
                                                                dragOffsetY = 0f
                                                                isDraggingQueueActive = false
                                                                onUpdateQueue?.invoke(finalQueue)
                                                            }
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DragHandle,
                                                    contentDescription = "Hold and drag to reorder",
                                                    tint = if (isTargeted) Color.White else Color.White.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                        } else {
                                            // For non-reorderable queue (or single item), show index number
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.width(28.dp)
                                            )
                                        }

                                        // Thumbnail / Badge
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF222222)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val thumbUrl = item.lowResThumbnailUrl ?: item.thumbnailUrl
                                            if (!thumbUrl.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = thumbUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                NothingFormatBadge(
                                                    label = item.audioFormat.label,
                                                    isHighlighted = item.isLocal
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Title and Artist
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.9f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.artist} • ${item.audioFormat.label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = item.formattedDuration,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )

                                        // Three dots menu button
                                        IconButton(
                                            onClick = { onTrackMenuClick(item) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Song options",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fullscreen Sliding Lyrics Window Overlay
            AnimatedVisibility(
                visible = showLyrics,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200)),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(35f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Frosted glass theme with background from album art for full screen player (downsampled for zero GPU stutter)
                    if (!highResCover.isNullOrEmpty()) {
                        val context = LocalContext.current
                        val coverReq = remember(highResCover) {
                            ImageRequest.Builder(context)
                                .data(highResCover)
                                .size(160, 160) // Downscaled thumbnail: natural silky smooth bokeh!
                                .crossfade(false)
                                .build()
                        }
                        AsyncImage(
                            model = coverReq,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.15f)
                                .blur(8.dp) // Light blur on tiny 160x160 buffer takes 0.01ms
                                .clipToBounds(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF141414))
                        )
                    }
                    // Frosted dark glass overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.58f))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        // Top Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { showLyrics = false }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Close Lyrics",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "LYRICS",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "${track.title} • ${track.artist}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.size(48.dp))
                        }

                        // Synchronized Auto-scrolling Lyrics View
                        LyricsView(
                            lyricsState = lyricsState,
                            currentTimeMs = state.currentPositionMs,
                            onSeekTo = onSeekToPosition,
                            textColor = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

@Composable
private fun AquaPlayerSeekbar(
    progress: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isInteracting by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val bounceScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.22f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.42f,
            stiffness = 500f
        ),
        label = "AquaThumbBounce"
    )

    // Subtle gentle wobble while dragging
    val infiniteTransition = rememberInfiniteTransition(label = "AquaWobble")
    val dragWobble by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WobbleAngle"
    )
    val rotationAngle = if (isDragging) dragWobble else 0f

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackBgColor = Color.White.copy(alpha = 0.28f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    isInteracting = true

                    val thumbRadiusPx = 18.dp.toPx()
                    val usablePx = (size.width - thumbRadiusPx * 2f).coerceAtLeast(1f)
                    val initialFraction = ((down.position.x - thumbRadiusPx) / usablePx).coerceIn(0f, 1f)
                    onSeek(initialFraction)

                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                            isDragging = true
                            val fraction = ((change.position.x - thumbRadiusPx) / usablePx).coerceIn(0f, 1f)
                            onSeek(fraction)
                        }
                    }

                    isInteracting = false
                    isDragging = false
                    onSeekFinished()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val thumbRadiusDp = 18.dp
        val thumbSizeDp = 36.dp

        val density = androidx.compose.ui.platform.LocalDensity.current
        val thumbRadiusPx = with(density) { thumbRadiusDp.toPx() }
        val usablePx = (widthPx - thumbRadiusPx * 2f).coerceAtLeast(1f)
        val activeWidthPx = usablePx * progress.coerceIn(0f, 1f)
        val thumbOffsetPx = activeWidthPx.toInt()

        // Seekbar track reverted to standard thickness (4.dp)
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val centerY = size.height / 2f
            val trackThickness = 4.dp.toPx()

            // Inactive track
            drawLine(
                color = trackBgColor,
                start = Offset(thumbRadiusPx, centerY),
                end = Offset(size.width - thumbRadiusPx, centerY),
                strokeWidth = trackThickness,
                cap = StrokeCap.Round
            )

            // Active track
            if (activeWidthPx > 0f) {
                drawLine(
                    color = primaryColor,
                    start = Offset(thumbRadiusPx, centerY),
                    end = Offset(thumbRadiusPx + activeWidthPx, centerY),
                    strokeWidth = trackThickness,
                    cap = StrokeCap.Round
                )
            }
        }

        // Timestamp Indicator: larger white circle with playseekbar.png and bounce animation
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx, 0) }
                .size(thumbSizeDp)
                .graphicsLayer {
                    scaleX = bounceScale
                    scaleY = bounceScale
                    this.rotationZ = rotationAngle
                    if (isInteracting) {
                        shadowElevation = 10f
                    }
                }
                .clip(CircleShape)
                .background(Color.White)
                .border(1.5.dp, primaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.playseekbar2),
                contentDescription = "Playback Position",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}
