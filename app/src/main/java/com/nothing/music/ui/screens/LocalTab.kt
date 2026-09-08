package com.nothing.music.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.music.model.AudioFormat
import com.nothing.music.model.PlaybackState
import com.nothing.music.model.Track
import com.nothing.music.ui.components.NothingFormatBadge
import com.nothing.music.ui.components.NothingPillButton
import com.nothing.music.ui.theme.NothingBorder
import com.nothing.music.ui.theme.NothingCard
import com.nothing.music.ui.theme.NothingCardElevated
import com.nothing.music.ui.theme.NothingRed
import com.nothing.music.ui.theme.NothingTextMuted
import com.nothing.music.ui.theme.NothingTextPrimary
import com.nothing.music.ui.theme.NothingTextSecondary

@Composable
fun LocalTab(
    allTracks: List<Track>,
    filteredTracks: List<Track>,
    isLoading: Boolean,
    selectedFilter: String,
    playbackState: PlaybackState,
    onFilterSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    onFilesPicked: (List<Uri>) -> Unit,
    onTrackSelect: (Track, List<Track>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Launcher for picking local audio files (.mp3, .m4a, .flac)
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onFilesPicked(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Action Bar: Pick Files & Rescan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pick audio files button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingCardElevated)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        filePicker.launch(arrayOf("audio/*", "audio/mpeg", "audio/mp4", "audio/flac"))
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Pick Files",
                        tint = NothingRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPEN AUDIO FILES",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = NothingTextPrimary
                    )
                }
            }

            // Rescan button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingCard)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan",
                    tint = NothingTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Format Filter Chips: ALL, FLAC, M4A, MP3
        val filters = listOf("ALL", "FLAC", "M4A", "MP3")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                val count = when (filter) {
                    "FLAC" -> allTracks.count { it.audioFormat == AudioFormat.FLAC }
                    "M4A" -> allTracks.count { it.audioFormat == AudioFormat.M4A }
                    "MP3" -> allTracks.count { it.audioFormat == AudioFormat.MP3 }
                    else -> allTracks.size
                }
                val isSelected = selectedFilter == filter
                NothingPillButton(
                    text = "$filter ($count)",
                    isSelected = isSelected,
                    hasRedDot = isSelected,
                    onClick = { onFilterSelect(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Supported formats status line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredTracks.size} AUDIO TRACKS",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = NothingTextMuted
            )
            Text(
                text = ".MP3 • .M4A • .FLAC",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = NothingRed
            )
        }

        // Local Track List or Empty State
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = NothingRed,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SCANNING STORAGE...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = NothingTextSecondary
                    )
                }
            }
        } else if (filteredTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF161616))
                            .border(1.dp, NothingBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = NothingTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO $selectedFilter FILES FOUND",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = NothingTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap 'OPEN AUDIO FILES' to pick .mp3, .m4a or .flac audios from your device storage",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = NothingTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTracks) { track ->
                    val isCurrent = playbackState.currentTrack?.id == track.id
                    LocalTrackItem(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && playbackState.isPlaying,
                        onClick = { onTrackSelect(track, filteredTracks) }
                    )
                }
            }
        }
    }
}

@Composable
fun LocalTrackItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color(0xFF161616) else NothingCard)
            .border(
                width = 1.dp,
                color = if (isCurrent) NothingRed else NothingBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio Format Badge
            NothingFormatBadge(
                label = track.audioFormat.label,
                isHighlighted = track.audioFormat == AudioFormat.FLAC || isCurrent,
                modifier = Modifier.width(44.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isCurrent) NothingTextPrimary else Color(0xFFE0E0E0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = NothingTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!track.sizeFormatted.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${track.sizeFormatted}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NothingTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Duration
            Text(
                text = track.formattedDuration,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = if (isCurrent) NothingRed else NothingTextMuted
            )
        }
    }
}

