package com.nothing.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nothing.music.ui.theme.NothingBorder
import com.nothing.music.ui.theme.NothingRed
import com.nothing.music.ui.theme.NothingTextMuted
import com.nothing.music.ui.theme.NothingTextPrimary
import kotlin.random.Random

@Composable
fun NothingDotVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    maxDotsPerBar: Int = 8,
    dotSize: Dp = 3.dp,
    spacing: Dp = 2.dp
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(((dotSize + spacing) * maxDotsPerBar).value.dp)
    ) {
        val totalWidth = size.width
        val barWidth = dotSize.toPx()
        val stepX = totalWidth / (barCount + 1)
        val stepY = (dotSize.toPx() + spacing.toPx())
        val dotRadius = dotSize.toPx() / 2f

        for (bar in 0 until barCount) {
            val cx = stepX * (bar + 1)

            // Generate pseudo-random bar height based on sine + phase
            val baseVal = kotlin.math.sin((bar.toDouble() / barCount.toDouble() * Math.PI * 2.0) + phase.toDouble() * Math.PI * 2.0)
            val noise = kotlin.math.sin(bar.toDouble() * 3.7 + phase.toDouble() * 7.1)
            val normalized = if (isPlaying) {
                ((baseVal + noise + 2.0) / 4.0).toFloat().coerceIn(0.15f, 1.0f)
            } else {
                0.15f // resting flat baseline
            }

            val activeDots = (normalized * maxDotsPerBar).toInt().coerceAtLeast(1)

            for (dot in 0 until maxDotsPerBar) {
                val cy = size.height - (dot * stepY + dotRadius)
                val isLit = dot < activeDots

                val dotColor = when {
                    !isLit -> Color(0xFF1E1E1E)
                    dot >= maxDotsPerBar - 2 -> NothingRed // Top peak is Nothing Red
                    else -> NothingTextPrimary
                }

                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun NothingDiscGlyph(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 160.dp
) {
    val transition = rememberInfiniteTransition(label = "disc")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentAngle = if (isPlaying) rotation else 0f

    Canvas(modifier = modifier.size(sizeDp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 4.dp.toPx()

        // Outer dot ring
        val dotCount = 36
        for (i in 0 until dotCount) {
            val angleRad = Math.toRadians((i * (360.0 / dotCount) + currentAngle))
            val x = center.x + (radius * Math.cos(angleRad)).toFloat()
            val y = center.y + (radius * Math.sin(angleRad)).toFloat()

            val isRedDot = (i % 9 == 0)
            drawCircle(
                color = if (isRedDot) NothingRed else NothingBorder,
                radius = if (isRedDot) 3.5.dp.toPx() else 2.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Inner grooves
        val innerRadius1 = radius * 0.72f
        for (i in 0 until 24) {
            val angleRad = Math.toRadians((i * (360.0 / 24) - currentAngle))
            val x = center.x + (innerRadius1 * Math.cos(angleRad)).toFloat()
            val y = center.y + (innerRadius1 * Math.sin(angleRad)).toFloat()
            drawCircle(
                color = Color(0xFF2E2E2E),
                radius = 1.5.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Center hub
        drawCircle(
            color = Color(0xFF141414),
            radius = radius * 0.35f,
            center = center
        )

        // Center red spindle
        drawCircle(
            color = NothingRed,
            radius = radius * 0.12f,
            center = center
        )

        // Center spindle hole
        drawCircle(
            color = Color(0xFF000000),
            radius = radius * 0.05f,
            center = center
        )
    }
}

