package com.wally.musesick.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wally.musesick.ui.theme.NothingBorder
import com.wally.musesick.ui.theme.NothingCard
import com.wally.musesick.ui.theme.NothingCardElevated
import com.wally.musesick.ui.theme.NothingRed
import com.wally.musesick.ui.theme.NothingTextMuted
import com.wally.musesick.ui.theme.NothingTextPrimary
import com.wally.musesick.ui.theme.NothingTextSecondary

@Composable
fun NothingPillButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasRedDot: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) NothingCardElevated else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) NothingTextPrimary else NothingBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasRedDot || isSelected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) NothingRed else NothingTextMuted)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = if (isSelected) NothingTextPrimary else NothingTextSecondary
            )
        }
    }
}

@Composable
fun NothingCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    isPrimary: Boolean = false,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPrimary) NothingTextPrimary else NothingCard)
            .border(
                width = 1.dp,
                color = if (isPrimary) NothingTextPrimary else NothingBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPrimary) Color.Black else NothingTextPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}



@Composable
fun NothingFormatBadge(
    label: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isHighlighted) Color(0xFF2A0F12) else Color(0xFF181818))
            .border(
                width = 1.dp,
                color = if (isHighlighted) NothingRed else NothingBorder,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp,
            color = if (isHighlighted) NothingRed else NothingTextSecondary
        )
    }
}

val GlassBorderBrush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.45f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.32f)
    )
)
val GlassBorderStroke = BorderStroke(1.dp, GlassBorderBrush)

@Composable
fun GlassmorphicCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 26.dp,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    isGlassmorphism: Boolean = true
) {
    val baseBg = if (isGlassmorphism) Color(0x3D1A1A1A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    } else baseBg

    val borderStroke = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else if (isGlassmorphism) {
        GlassBorderStroke
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }

    val iconTint = if (!enabled) {
        tint.copy(alpha = 0.4f)
    } else {
        tint
    }

    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = bg,
        border = borderStroke,
        tonalElevation = if (isGlassmorphism) 0.dp else 3.dp,
        shadowElevation = if (isGlassmorphism) 8.dp else 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun GlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color? = null,
    borderColor: Color? = null,
    isGlassmorphism: Boolean = true
) {
    val bg = containerColor ?: if (isGlassmorphism) Color(0x3D1A1A1A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
    val borderStroke = if (borderColor != null) {
        BorderStroke(1.dp, borderColor)
    } else if (isGlassmorphism) {
        GlassBorderStroke
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = bg,
        border = borderStroke,
        tonalElevation = if (isGlassmorphism) 0.dp else 3.dp,
        shadowElevation = if (isGlassmorphism) 8.dp else 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}



