package com.wally.musesick.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wally.musesick.model.Album
import com.wally.musesick.model.Artist
import com.wally.musesick.model.Track
import com.wally.musesick.repository.YouTubeRepository
import com.wally.musesick.ui.components.GlassBackButton
import com.wally.musesick.ui.theme.NothingRed

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistDetailScreen(
    artist: Artist,
    albums: List<Album>,
    songs: List<Track>,
    isLoading: Boolean,
    playingTrackId: String? = null,
    isPlaying: Boolean = false,
    onBack: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSongClick: (Track, List<Track>) -> Unit,
    onSongLongClick: ((Track) -> Unit)? = null,
    onSeeMoreSongs: () -> Unit,
    onPlay: () -> Unit = {},
    onShufflePlay: () -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onBottomReachedChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAllAlbums by remember { mutableStateOf(false) }
    val displayedAlbums = if (showAllAlbums) albums else albums.take(4)
    val displayedSongs = songs.take(5)

    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val heroHeight = (configuration.screenHeightDp.dp / 3).coerceIn(240.dp, 360.dp)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val rawBg = MaterialTheme.colorScheme.background
    val appBg = if (rawBg == Color.Transparent || rawBg.alpha < 0.1f) {
        if (isDark) Color(0xFF070505) else Color(0xFFF7F5FA)
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
                    // Artist Photo
                    if (!artist.thumbnailUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = artist.name,
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
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

                            if (!artist.subtitle.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = artist.subtitle,
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

            if (isLoading && albums.isEmpty() && songs.isEmpty()) {
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
                                text = "Loading ${artist.name}...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // --- ALBUMS SECTION ---
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Albums (${albums.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                if (albums.isEmpty() && !isLoading) {
                    item {
                        Text(
                            text = "No albums found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(displayedAlbums.chunked(2), key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (album in pair) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AlbumCard(album = album, onClick = { onAlbumClick(album) })
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (albums.size > 4) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { showAllAlbums = !showAllAlbums },
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showAllAlbums) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (showAllAlbums) "Show Fewer Albums" else "See More Albums (${albums.size})"
                                    )
                                }
                            }
                        }
                    }
                }

                // --- POPULAR SONGS SECTION ---
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Popular Songs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = onSeeMoreSongs,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "See More",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "See More Songs",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (songs.isEmpty() && !isLoading) {
                    item {
                        Text(
                            text = "No songs found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = displayedSongs,
                        key = { index, song -> "${song.id}_$index" },
                        contentType = { _, _ -> "song" }
                    ) { index, song ->
                        val isCurrent = playingTrackId == song.id
                        SongListItem(
                            track = song,
                            index = index + 1,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onClick = { onSongClick(song, songs) },
                            onLongClick = { onSongLongClick?.invoke(song) }
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

        // 4. Circular Glassmorphism Favorite Button (pinned at top right)
        if (onToggleFavorite != null) {
            val favBorder = androidx.compose.foundation.BorderStroke(
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
                    .padding(top = statusBarTop + 8.dp, end = 20.dp)
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onToggleFavorite() },
                shape = CircleShape,
                color = Color(0x3D1A1A1A),
                border = favBorder,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                        tint = if (isFavorite) Color(0xFFD71921) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. Title text:
        // Hero multi-line title (maxLines = 2 when unscrolled, dissolves as it reaches dock)
        val maxHeroWidth = configuration.screenWidthDp.dp - initialX - 24.dp
        val maxDockedWidth = (configuration.screenWidthDp.dp - pinnedX - 76.dp).coerceAtLeast(100.dp)

        Text(
            text = artist.name,
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
            text = artist.name,
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

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!album.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = YouTubeRepository.toLowResThumbnailUrl(album.thumbnailUrl) ?: album.thumbnailUrl,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!album.year.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = album.year,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    track: Track,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDownloaded: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number or playing indicator
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Thumbnail
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val thumbUrl = track.lowResThumbnailUrl ?: track.thumbnailUrl
            if (!thumbUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
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
                if (isDownloaded) {
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
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (track.durationMs > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = track.formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
