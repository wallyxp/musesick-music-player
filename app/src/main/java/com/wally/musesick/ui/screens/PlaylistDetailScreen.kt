package com.wally.musesick.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.wally.musesick.model.Playlist
import com.wally.musesick.model.Track
import com.wally.musesick.ui.components.GlassBackButton
import com.wally.musesick.ui.theme.NothingRed

private data class DraggablePlaylistItem(
    val sessionKey: String,
    val track: Track
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    playingTrackId: String? = null,
    isPlaying: Boolean = false,
    onBack: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onTrackClick(list.first(), list) },
    onShuffleAll: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onTrackClick(list.shuffled().first(), list.shuffled()) },
    onRemoveTrack: (Track) -> Unit = {},
    onDeletePlaylist: () -> Unit = {},
    onExportLocally: () -> Unit = {},
    onExportShare: () -> Unit = {},
    onReorderTracks: (Int, Int) -> Unit = { _, _ -> },
    onUpdateTracks: ((List<Track>) -> Unit)? = null,
    isTrackDownloaded: (Track) -> Boolean = { false },
    onEditPlaylist: () -> Unit = {},
    onBottomReachedChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUserCreated = !playlist.id.startsWith("suggested_")
    var showMenu by remember { mutableStateOf(false) }
    var heldIndex by remember { mutableStateOf<Int?>(null) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingActive by remember { mutableStateOf(false) }

    val localTracks = remember { mutableStateListOf<DraggablePlaylistItem>() }
    LaunchedEffect(playlist.tracks) {
        if (!isDraggingActive) {
            localTracks.clear()
            localTracks.addAll(playlist.tracks.mapIndexed { idx, track ->
                DraggablePlaylistItem(sessionKey = "${track.id}_$idx", track = track)
            })
        }
    }

    val density = LocalDensity.current
    var itemHeightPx by remember { mutableFloatStateOf(with(density) { 60.dp.toPx() }) }

    val listState = rememberLazyListState()

    // Auto-scroll when dragging near viewport bounds
    LaunchedEffect(isDraggingActive) {
        if (isDraggingActive) {
            while (isActive && isDraggingActive) {
                val dragIdx = draggingIndex
                if (dragIdx != null) {
                    val targetLazyIndex = dragIdx + 1 // item 0 is the hero header
                    val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetLazyIndex }
                    if (itemInfo != null) {
                        itemHeightPx = itemInfo.size.toFloat().coerceAtLeast(40f)
                        val viewportHeight = listState.layoutInfo.viewportSize.height
                        val currentY = itemInfo.offset + dragOffsetY
                        val topThreshold = 140f
                        val bottomThreshold = viewportHeight - 160f
                        if (currentY < topThreshold) {
                            val scroll = ((topThreshold - currentY) / 8f).coerceIn(4f, 25f)
                            listState.scrollBy(-scroll)
                        } else if (currentY > bottomThreshold) {
                            val scroll = ((currentY - bottomThreshold) / 8f).coerceIn(4f, 25f)
                            listState.scrollBy(scroll)
                        }
                    }
                }
                delay(16)
            }
        }
    }
    val configuration = LocalConfiguration.current
    val heroHeight = (configuration.screenHeightDp.dp / 3).coerceIn(240.dp, 360.dp)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val rawBg = MaterialTheme.colorScheme.background
    val appBg = if (rawBg == Color.Transparent || rawBg.alpha < 0.1f) {
        Color(0xFF070505)
    } else {
        rawBg
    }

    // Geometry of title transition (precomputed in pixels for zero-recomposition graphicsLayer execution)
    val pinnedY = statusBarTop + 15.dp
    val pinnedX = 84.dp
    val initialX = 20.dp
    val initialY = heroHeight - 110.dp

    val pinnedYPx = with(density) { pinnedY.toPx() }
    val pinnedXPx = with(density) { pinnedX.toPx() }
    val initialXPx = with(density) { initialX.toPx() }
    val initialYPx = with(density) { initialY.toPx() }
    val dockDistancePx = (initialYPx - pinnedYPx).coerceAtLeast(1f)

    // Detect when user scrolled to bottom (so last element is completely above miniplayer)
    val isAtBottom by remember {
        derivedStateOf {
            if (!listState.canScrollBackward && !listState.canScrollForward) {
                val lastItem = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
                val lastItemBottom = lastItem.offset + lastItem.size
                lastItemBottom < (listState.layoutInfo.viewportEndOffset - 300)
            } else {
                !listState.canScrollForward
            }
        }
    }

    LaunchedEffect(isAtBottom) {
        onBottomReachedChanged?.invoke(isAtBottom)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appBg)
    ) {
        // 1. Scrollable Content
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            // Hero Photo Section (Top 1/3rd of viewport)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .clipToBounds()
                ) {
                    val heroImageUrl = playlist.imageUri.takeIf { !it.isNullOrBlank() }
                        ?: localTracks.firstOrNull()?.track?.thumbnailUrl
                    if (!heroImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = heroImageUrl,
                            contentDescription = playlist.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val scrollPx = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 10000f
                                    translationY = scrollPx * 0.35f // Parallax depth
                                },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Gradient transition overlay covering the entirety of the bottom portion of the photo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.00f to Color.Transparent,
                                    0.15f to Color.Transparent,
                                    0.35f to appBg.copy(alpha = 0.35f),
                                    0.55f to appBg.copy(alpha = 0.70f),
                                    0.75f to appBg.copy(alpha = 0.95f),
                                    0.88f to appBg,
                                    1.00f to appBg
                                )
                            )
                    )

                    // Hero Bottom Row: placeholder on left + subtitle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left bottom: layout spacer for floating title + subtitle + optional edit
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            // Reserve height for the floating animated title (which can have up to 2 lines)
                            Spacer(modifier = Modifier.height(72.dp))
                            Spacer(modifier = Modifier.height(4.dp))

                            val canEdit = isUserCreated && playlist.id != "liked_music" && playlist.id != "history"
                            val subtitleText = if (canEdit) {
                                "${localTracks.size} tracks • Hold handle to reorder"
                            } else {
                                "${localTracks.size} tracks"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.graphicsLayer {
                                    val scrollPx = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 10000f
                                    val curY = (initialYPx - scrollPx).coerceAtLeast(pinnedYPx)
                                    val curProgress = ((initialYPx - curY) / dockDistancePx).coerceIn(0f, 1f)
                                    alpha = (1f - curProgress * 1.5f).coerceIn(0f, 1f)
                                }
                            ) {
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                            blurRadius = 4f
                                        )
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (canEdit) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.linearGradient(
                                                    listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
                                                ),
                                                shape = CircleShape
                                            )
                                            .clickable { onEditPlaylist() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Playlist",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Playlist tracks section
            if (localTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This playlist is empty.\nLong press any song to add it here!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = localTracks,
                    key = { _, itemWrapper -> itemWrapper.sessionKey },
                    contentType = { _, _ -> "playlist_track" }
                ) { index, itemWrapper ->
                    val track = itemWrapper.track
                    val isCurrent = playingTrackId == track.id
                    val isDragging = draggingIndex == index
                    val isTargeted = isDragging || (heldIndex == index)

                    val zoomScale by animateFloatAsState(
                        targetValue = if (isTargeted) 1.05f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "card_zoom"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (!isDragging) Modifier.animateItem() else Modifier)
                            .padding(horizontal = if (isTargeted) 8.dp else 0.dp, vertical = 2.dp)
                            .zIndex(if (isTargeted) 15f else 1f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                scaleX = zoomScale
                                scaleY = zoomScale
                            }
                            .clickable { onTrackClick(track, localTracks.map { it.track }) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTargeted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f) else Color.Transparent,
                        tonalElevation = if (isTargeted) 6.dp else 0.dp,
                        shadowElevation = if (isTargeted) 8.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = if (isUserCreated) 8.dp else 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isUserCreated) {
                                // Drag handle with multi-slot continuous drag gesture
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .pointerInput(itemWrapper.sessionKey) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    val currentIdx = localTracks.indexOfFirst { it.sessionKey == itemWrapper.sessionKey }
                                                    if (currentIdx != -1) {
                                                        isDraggingActive = true
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
                                                        val targetIdx = (currentDragIdx + deltaSlots).coerceIn(0, localTracks.lastIndex)
                                                        if (targetIdx != currentDragIdx) {
                                                            val moved = localTracks.removeAt(currentDragIdx)
                                                            localTracks.add(targetIdx, moved)
                                                            dragOffsetY -= (targetIdx - currentDragIdx) * itemHeightPx
                                                            draggingIndex = targetIdx
                                                            heldIndex = targetIdx
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    val finalTracks = localTracks.map { it.track }
                                                    heldIndex = null
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                    isDraggingActive = false
                                                    onUpdateTracks?.invoke(finalTracks) ?: onReorderTracks(0, 0)
                                                },
                                                onDragCancel = {
                                                    val finalTracks = localTracks.map { it.track }
                                                    heldIndex = null
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                    isDraggingActive = false
                                                    onUpdateTracks?.invoke(finalTracks)
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Hold to reorder",
                                        tint = if (isTargeted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(24.dp)
                            )

                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!track.thumbnailUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = track.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Track info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isTrackDownloaded(track)) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Downloaded",
                                            tint = NothingRed,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${track.artist} • ${track.audioFormat.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Three-dot menu per track
                            var showTrackMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showTrackMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Track options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showTrackMenu,
                                    onDismissRequest = { showTrackMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Play Now") },
                                        onClick = {
                                            showTrackMenu = false
                                            onTrackClick(track, localTracks.map { it.track })
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("More Options") },
                                        onClick = {
                                            showTrackMenu = false
                                            onTrackLongClick(track)
                                        }
                                    )
                                    if (isUserCreated) {
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Remove from Playlist", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showTrackMenu = false
                                                onRemoveTrack(track)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. TopBar gradient background (always present with smooth gradient feathering)
        val topBarHeight = statusBarTop + 68.dp
        val topBarGradient = remember(appBg) {
            Brush.verticalGradient(
                0.00f to appBg,
                0.55f to appBg.copy(alpha = 0.85f),
                0.80f to appBg.copy(alpha = 0.45f),
                1.00f to Color.Transparent
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    val scrollPx = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 10000f
                    val prog = (scrollPx / dockDistancePx).coerceIn(0f, 1f)
                    alpha = 0.75f + prog * 0.25f
                }
                .background(topBarGradient)
        )

        // 3. Circular Glassmorphism Back Button (pinned at top left with increased spacing on either side)
        GlassBackButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = statusBarTop + 8.dp, start = 20.dp)
                .align(Alignment.TopStart)
        )

        // 4. Circular Glassmorphism Options Button & Menu (pinned at top right)
        Box(
            modifier = Modifier
                .padding(top = statusBarTop + 8.dp, end = 20.dp)
                .align(Alignment.TopEnd)
        ) {
            val optBorder = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.32f)
                    )
                )
            )
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { showMenu = true },
                shape = CircleShape,
                color = Color(0x3D1A1A1A),
                border = optBorder,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Playlist Options",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (isUserCreated) {
                    DropdownMenuItem(
                        text = { Text("Edit Playlist") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onEditPlaylist()
                        }
                    )
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = { Text("Export Playlist Locally") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        showMenu = false
                        onExportLocally()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share / Export to Cloud") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        showMenu = false
                        onExportShare()
                    }
                )
                if (isUserCreated) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete Playlist", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeletePlaylist()
                        }
                    )
                }
            }
        }

        // 5. Title text:
        // Hero multi-line title (maxLines = 2 when unscrolled, dissolves as it reaches dock)
        val maxHeroWidth = configuration.screenWidthDp.dp - initialX - 24.dp
        val maxDockedWidth = (configuration.screenWidthDp.dp - pinnedX - 76.dp).coerceAtLeast(100.dp)

        Text(
            text = playlist.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.75f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                    blurRadius = 6f
                )
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .widthIn(max = maxHeroWidth)
                .graphicsLayer {
                    val scrollPx = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 10000f
                    val curY = (initialYPx - scrollPx).coerceAtLeast(pinnedYPx)
                    val curProgress = ((initialYPx - curY) / dockDistancePx).coerceIn(0f, 1f)
                    val curX = initialXPx + (pinnedXPx - initialXPx) * curProgress
                    val curScale = 1.0f - (curProgress * 0.28f)
                    val curAlpha = (1f - (curProgress - 0.50f) / 0.30f).coerceIn(0f, 1f)

                    translationX = curX
                    translationY = curY
                    scaleX = curScale
                    scaleY = curScale
                    alpha = curAlpha
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        )

        // Docked single-line title with ellipsis (strictly 1 line with ... on topbar after scrolling)
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .widthIn(max = maxDockedWidth)
                .graphicsLayer {
                    val scrollPx = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 10000f
                    val curY = (initialYPx - scrollPx).coerceAtLeast(pinnedYPx)
                    val curProgress = ((initialYPx - curY) / dockDistancePx).coerceIn(0f, 1f)
                    val curX = initialXPx + (pinnedXPx - initialXPx) * curProgress
                    val curScale = 1.0f - (curProgress * 0.28f)
                    val curAlpha = ((curProgress - 0.50f) / 0.30f).coerceIn(0f, 1f)

                    translationX = curX
                    translationY = curY
                    scaleX = curScale
                    scaleY = curScale
                    alpha = curAlpha
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        )
    }
}
