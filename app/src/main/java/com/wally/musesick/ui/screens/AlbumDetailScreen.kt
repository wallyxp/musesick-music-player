package com.wally.musesick.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wally.musesick.model.Album
import com.wally.musesick.model.Track
import com.wally.musesick.repository.YouTubeRepository
import com.wally.musesick.ui.components.GlassBackButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumDetailScreen(
    album: Album,
    tracks: List<Track>,
    isLoading: Boolean,
    playingTrackId: String? = null,
    isPlaying: Boolean = false,
    onBack: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onTrackLongClick: ((Track) -> Unit)? = null,
    onPlayAll: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onTrackClick(list.first(), list) },
    onShuffleAll: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onTrackClick(list.shuffled().first(), list.shuffled()) },
    onBottomReachedChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
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
            // Hero Photo Section (Top 1/3rd of the viewport)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .clipToBounds()
                ) {
                    val imageUrl = YouTubeRepository.toHighResThumbnailUrl(album.thumbnailUrl) ?: album.thumbnailUrl
                    if (!imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = album.title,
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
                                imageVector = Icons.Default.Album,
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
                        // Left bottom: layout spacer for floating title + subtitle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            // Reserve height for the floating animated title (which can have up to 2 lines)
                            Spacer(modifier = Modifier.height(72.dp))

                            val subtitle = album.artist + if (!album.year.isNullOrEmpty()) " • ${album.year}" else ""
                            if (subtitle.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                            blurRadius = 4f
                                        )
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.graphicsLayer {
                                        val scrollPx = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 10000f
                                        val curY = (initialYPx - scrollPx).coerceAtLeast(pinnedYPx)
                                        val curProgress = ((initialYPx - curY) / dockDistancePx).coerceIn(0f, 1f)
                                        alpha = (1f - curProgress * 1.5f).coerceIn(0f, 1f)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Tracks Header & List (or Loading state)
            if (isLoading && tracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading album tracks...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Tracks (${tracks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                if (tracks.isEmpty()) {
                    item {
                        Text(
                            text = "No tracks found for this album",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = tracks,
                        key = { index, track -> "${track.id}_$index" },
                        contentType = { _, _ -> "album_track" }
                    ) { index, track ->
                        val isCurrent = playingTrackId == track.id
                        SongListItem(
                            track = track,
                            index = index + 1,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onClick = { onTrackClick(track, tracks) },
                            onLongClick = { onTrackLongClick?.invoke(track) }
                        )
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

        // 4. Title text:
        // Hero multi-line title (maxLines = 2 when unscrolled, dissolves as it reaches dock)
        val maxHeroWidth = configuration.screenWidthDp.dp - initialX - 24.dp
        val maxDockedWidth = (configuration.screenWidthDp.dp - pinnedX - 24.dp).coerceAtLeast(100.dp)

        Text(
            text = album.title,
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
            text = album.title,
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
