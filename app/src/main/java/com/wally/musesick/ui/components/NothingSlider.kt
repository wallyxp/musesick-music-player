package com.wally.musesick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.wally.musesick.ui.theme.NothingBorder
import com.wally.musesick.ui.theme.NothingRed

@Composable
fun NothingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isDragging by remember { mutableFloatStateOf(-1f) }

    val displayValue = if (isDragging >= 0f) isDragging else value.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(fraction)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (isDragging >= 0f) {
                            onValueChange(isDragging)
                            isDragging = -1f
                        }
                    },
                    onDragCancel = {
                        isDragging = -1f
                    },
                    onDrag = { change, _ ->
                        isDragging = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val centerY = size.height / 2f
        val trackHeight = 2.dp.toPx()
        val thumbRadius = 4.5.dp.toPx()

        // Background track
        drawLine(
            color = NothingBorder,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = trackHeight
        )

        // Active track (Nothing Red)
        val activeEnd = size.width * displayValue
        drawLine(
            color = NothingRed,
            start = Offset(0f, centerY),
            end = Offset(activeEnd, centerY),
            strokeWidth = trackHeight
        )

        // Scrubber thumb
        drawCircle(
            color = NothingRed,
            radius = thumbRadius,
            center = Offset(activeEnd, centerY)
        )
    }
}

