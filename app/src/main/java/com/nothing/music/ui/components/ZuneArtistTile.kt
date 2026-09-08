package com.nothing.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nothing.music.model.Artist

enum class ZuneTileSize {
    WIDE,    // Panoramic tile
    SQUARE,  // Standard square mosaic tile
    TALL     // Vertical showcase tile
}

@Composable
fun ZuneArtistTile(
    artist: Artist,
    tileSize: ZuneTileSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "zune_scale"
    )

    val heightDp: Dp = when (tileSize) {
        ZuneTileSize.WIDE -> 160.dp
        ZuneTileSize.TALL -> 210.dp
        ZuneTileSize.SQUARE -> 140.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                while (true) {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Dynamic abstract Zune-style base gradient (placeholder & ambient fallback)
        val gradientColors = getZuneGradient(artist.name)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
        )

        // Backdrop Imagery (Artist photo)
        if (!artist.thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Zune Shadow Overlay for crystal clear text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33000000),
                            Color(0xDD000000)
                        ),
                        startY = 0f
                    )
                )
        )

        // Bold Zune Typography Overlay with Artist Avatar on Title
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            if (!artist.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = artist.thumbnailUrl,
                    contentDescription = artist.name,
                    modifier = Modifier
                        .size(if (tileSize == ZuneTileSize.WIDE) 44.dp else 36.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (!artist.subtitle.isNullOrEmpty()) {
                    Text(
                        text = artist.subtitle.uppercase(),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = Color(0xCCFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = artist.name,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = if (tileSize == ZuneTileSize.WIDE) 22.sp else 17.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    lineHeight = if (tileSize == ZuneTileSize.WIDE) 26.sp else 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getZuneGradient(seed: String): List<Color> {
    val h = kotlin.math.abs(seed.hashCode())
    val palettes = listOf(
        listOf(Color(0xFF2C3E50), Color(0xFF000000)),
        listOf(Color(0xFF8E0E00), Color(0xFF1F1C18)),
        listOf(Color(0xFF114357), Color(0xFFF29492)),
        listOf(Color(0xFF232526), Color(0xFF414345)),
        listOf(Color(0xFF16222A), Color(0xFF3A6073)),
        listOf(Color(0xFF1F4037), Color(0xFF99F2C8)),
        listOf(Color(0xFF4B1248), Color(0xFFF0C27B)),
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )
    return palettes[h % palettes.size]
}

