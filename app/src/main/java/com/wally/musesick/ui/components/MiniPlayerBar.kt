package com.wally.musesick.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wally.musesick.model.PlaybackState

@Composable
fun MiniPlayerBar(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit = onClick,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return

    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }
    var nextClickTrigger by remember { mutableIntStateOf(0) }
    val nextButtonScale by animateFloatAsState(
        targetValue = if (nextClickTrigger > 0) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        finishedListener = {
            if (nextClickTrigger > 0) nextClickTrigger = 0
        },
        label = "mini_next_scale"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    verticalDragAccumulator += delta
                    if (verticalDragAccumulator < -45f) {
                        onSwipeUp()
                        verticalDragAccumulator = 0f
                    }
                },
                onDragStopped = { velocity ->
                    if (velocity < -180f || verticalDragAccumulator < -25f) {
                        onSwipeUp()
                    }
                    verticalDragAccumulator = 0f
                }
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            // Dynamic Progress Indicator Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.progressFraction)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail with crossfade
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = track.id,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "mini_thumb_transition"
                    ) { _ ->
                        val thumbUrl = track.lowResThumbnailUrl ?: track.thumbnailUrl
                        if (!thumbUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = null,
                                modifier = Modifier.size(46.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            NothingFormatBadge(
                                label = track.audioFormat.label,
                                isHighlighted = track.isLocal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist with smooth slide and fade
                AnimatedContent(
                    targetState = track.id,
                    transitionSpec = {
                        (slideInHorizontally { it / 3 } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(200)))
                    },
                    label = "mini_track_info_transition",
                    modifier = Modifier.weight(1f)
                ) { _ ->
                    Column {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${track.artist} • ${track.audioFormat.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause Button with Buffering state and animated icon morph
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    AnimatedContent(
                        targetState = Pair(state.isPlaying, state.isBuffering),
                        transitionSpec = {
                            (scaleIn(animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f)) + fadeIn(tween(150)))
                                .togetherWith(scaleOut(animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f)) + fadeOut(tween(150)))
                        },
                        label = "mini_play_pause_transition"
                    ) { (isPlaying, isBuffering) ->
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Next Button with tactile bounce animation
                IconButton(
                    onClick = {
                        nextClickTrigger++
                        onNext()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            scaleX = nextButtonScale
                            scaleY = nextButtonScale
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

