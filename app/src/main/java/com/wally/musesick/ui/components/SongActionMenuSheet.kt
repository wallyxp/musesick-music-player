package com.wally.musesick.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wally.musesick.model.Track
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionMenuSheet(
    track: Track,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToExistingPlaylist: () -> Unit,
    onAddToNewPlaylist: () -> Unit,
    onDismiss: () -> Unit,
    isDownloaded: Boolean = false,
    downloadProgress: Float? = null,
    onDownload: () -> Unit = {},
    onRemoveDownload: () -> Unit = {},
    isCustomImageTheme: Boolean = false,
    customThemeImagePath: String? = null,
    customThemeImageRes: Int? = null,
    ambientBrush: Brush? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isCustomImageTheme || ambientBrush != null) Color.Black.copy(alpha = 0.98f) else MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xF8141414))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
            // Song Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.thumbnailUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        NothingFormatBadge(
                            label = track.audioFormat.label,
                            isHighlighted = track.isLocal
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.audioFormat.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp),
                color = Color.White.copy(alpha = 0.10f)
            )

            // Actions
            ActionMenuItem(
                icon = Icons.Default.PlayArrow,
                title = "Play now",
                onClick = {
                    onPlayNow()
                    onDismiss()
                }
            )

            ActionMenuItem(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                title = "Play next",
                onClick = {
                    onPlayNext()
                    onDismiss()
                }
            )

            ActionMenuItem(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = "Add to queue",
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp),
                color = Color.White.copy(alpha = 0.10f)
            )

            ActionMenuItem(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = "Add to existing playlist",
                onClick = {
                    onAddToExistingPlaylist()
                }
            )

            ActionMenuItem(
                icon = Icons.Default.Add,
                title = "Add to new playlist",
                onClick = {
                    onAddToNewPlaylist()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp),
                color = Color.White.copy(alpha = 0.10f)
            )

            if (isDownloaded) {
                ActionMenuItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Downloaded",
                    onClick = {
                        onRemoveDownload()
                        onDismiss()
                    }
                )
            } else if (downloadProgress != null) {
                ActionMenuItem(
                    icon = Icons.Default.Downloading,
                    title = "Downloading (${(downloadProgress * 100).toInt()}%)",
                    onClick = { }
                )
            } else {
                ActionMenuItem(
                    icon = Icons.Default.Download,
                    title = "Download song",
                    onClick = {
                        onDownload()
                        onDismiss()
                    }
                )
            }
        }
        } // end Box
    }
}

@Composable
private fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                1.dp,
                GlassBorderBrush,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.25f)
                            )
                        ),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
