package com.wally.musesick.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.wally.musesick.model.PlaybackState
import com.wally.musesick.model.PlayerStyle
import com.wally.musesick.model.Track
import com.wally.musesick.ui.components.NothingFormatBadge

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex

import com.wally.musesick.model.AppTheme
import com.wally.musesick.ui.theme.MusesickThemed

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
    playerStyle: PlayerStyle = PlayerStyle.FULLSCREEN_ALBUM_ART,
    playerTheme: AppTheme? = null,
    playerThemeVariant: String? = null,
    isQueueReorderable: Boolean = true,
    customAccentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val isFullscreen = playerStyle == PlayerStyle.FULLSCREEN_ALBUM_ART
    val isNonFullscreen = playerStyle == PlayerStyle.NON_FULLSCREEN_ALBUM_ART

    if (isNonFullscreen && playerTheme != null) {
        MusesickThemed(
            theme = playerTheme,
            variant = playerThemeVariant,
            customAccentColor = customAccentColor
        ) {
            FullPlayerSheetInternal(
                state = state,
                queue = queue,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onTrackSelect = onTrackSelect,
                onReorderQueue = onReorderQueue,
                onRemoveFromQueue = onRemoveFromQueue,
                onTrackLongClick = onTrackLongClick,
                onTrackMenuClick = onTrackMenuClick,
                onClose = onClose,
                playerStyle = playerStyle,
                playerTheme = playerTheme,
                isQueueReorderable = isQueueReorderable,
                customAccentColor = customAccentColor,
                modifier = modifier
            )
        }
    } else {
        FullPlayerSheetInternal(
            state = state,
            queue = queue,
            onTogglePlayPause = onTogglePlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
            onTrackSelect = onTrackSelect,
            onReorderQueue = onReorderQueue,
            onRemoveFromQueue = onRemoveFromQueue,
            onTrackLongClick = onTrackLongClick,
            onTrackMenuClick = onTrackMenuClick,
            onClose = onClose,
            playerStyle = playerStyle,
            playerTheme = playerTheme,
            isQueueReorderable = isQueueReorderable,
            customAccentColor = customAccentColor,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullPlayerSheetInternal(
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
    playerStyle: PlayerStyle = PlayerStyle.FULLSCREEN_ALBUM_ART,
    playerTheme: AppTheme? = null,
    isQueueReorderable: Boolean = true,
    customAccentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return
    var showQueue by remember { mutableStateOf(false) }
    val canReorder = isQueueReorderable && queue.size > 1
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var heldIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 76.dp.toPx() }

    BackHandler(enabled = showQueue) {
        showQueue = false
    }

    val isFullscreen = playerStyle == PlayerStyle.FULLSCREEN_ALBUM_ART
    val isNonFullscreen = playerStyle == PlayerStyle.NON_FULLSCREEN_ALBUM_ART

    val playerAccentColor = if (isFullscreen) {
        Color.White
    } else if (playerTheme == AppTheme.CUSTOM_COLOR && customAccentColor != null) {
        customAccentColor
    } else {
        MaterialTheme.colorScheme.primary
    }

    val highResCover = track.highResThumbnailUrl ?: track.thumbnailUrl

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (isFullscreen) {
            // Fullscreen Album Art Background
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
        }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        modifier = Modifier.size(28.dp),
                        tint = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isFullscreen) Color.White else playerAccentColor
                )

                NothingFormatBadge(
                    label = track.audioFormat.label,
                    isHighlighted = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isFullscreen) {
                // Modern style: full-bleed background is already displayed behind
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // Non Full Screen Album Art: Centered Album Art Card styled with playerAccentColor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ElevatedCard(
                        modifier = Modifier.size(260.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
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
                                    .background(playerAccentColor.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    modifier = Modifier.size(90.dp),
                                    tint = playerAccentColor
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

                // Track Title & Artist
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isFullscreen) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Scrubber Slider
                var sliderPosition by remember { mutableFloatStateOf(-1f) }
                val currentProgress = if (sliderPosition >= 0f) sliderPosition else state.progressFraction

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
                    colors = if (isFullscreen) {
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        )
                    } else {
                        SliderDefaults.colors(
                            thumbColor = playerAccentColor,
                            activeTrackColor = playerAccentColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                )

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
                        color = if (isFullscreen) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.formattedDuration,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFullscreen) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
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
                            tint = if (isFullscreen) {
                                if (state.isShuffle) Color.White else Color.White.copy(alpha = 0.5f)
                            } else {
                                if (state.isShuffle) playerAccentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Previous
                    FilledTonalIconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(54.dp),
                        colors = if (isFullscreen) {
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            )
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Big Circular Play/Pause
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(72.dp),
                        colors = if (isFullscreen) {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        } else {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = playerAccentColor,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = if (isFullscreen) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = if (isFullscreen) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Next
                    FilledTonalIconButton(
                        onClick = onNext,
                        modifier = Modifier.size(54.dp),
                        colors = if (isFullscreen) {
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            )
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat
                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isFullscreen) {
                                if (state.isRepeat) Color.White else Color.White.copy(alpha = 0.5f)
                            } else {
                                if (state.isRepeat) playerAccentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Queue toggle button
                FilledTonalButton(
                    onClick = { showQueue = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = if (isFullscreen) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Queue (${queue.size})",
                        color = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onSurface
                    )
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
                                text = "Up Next (${queue.size})",
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
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(queue, key = { index, item -> "${item.id}_$index" }) { index, item ->
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

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isTargeted) 20f else 1f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragOffsetY else 0f
                                            scaleX = zoomScale
                                            scaleY = zoomScale
                                        }
                                        .border(
                                            border = if (isTargeted) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onTrackSelect(item) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = if (isTargeted) Color(0xFF262626) else Color(0xFF141414)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = if (canReorder) 4.dp else 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (canReorder) {
                                            // Hold and Drag Handle with multi-slot one-drag gesture
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .pointerInput(queue.size) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                heldIndex = index
                                                                draggingIndex = index
                                                                dragOffsetY = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsetY += dragAmount.y
                                                                val currentDragIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                                                val deltaSlots = kotlin.math.round(dragOffsetY / itemHeightPx).toInt()
                                                                if (deltaSlots != 0) {
                                                                    val targetIdx = (currentDragIdx + deltaSlots).coerceIn(0, queue.lastIndex)
                                                                    if (targetIdx != currentDragIdx) {
                                                                        onReorderQueue(currentDragIdx, targetIdx)
                                                                        dragOffsetY -= (targetIdx - currentDragIdx) * itemHeightPx
                                                                        draggingIndex = targetIdx
                                                                        heldIndex = targetIdx
                                                                    }
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                heldIndex = null
                                                                draggingIndex = null
                                                                dragOffsetY = 0f
                                                            },
                                                            onDragCancel = {
                                                                heldIndex = null
                                                                draggingIndex = null
                                                                dragOffsetY = 0f
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
        }
    }
