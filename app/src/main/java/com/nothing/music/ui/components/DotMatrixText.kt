package com.nothing.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nothing.music.ui.theme.NothingTextPrimary

@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.dp,
    spacing: Dp = 1.2.dp,
    activeColor: Color = NothingTextPrimary,
    inactiveColor: Color = Color.Transparent
) {
    val upper = text.uppercase()
    val charWidthDots = 5
    val charHeightDots = 7
    val charGapDots = 2

    val totalCols = upper.length * (charWidthDots + charGapDots) - charGapDots
    if (totalCols <= 0) return

    val totalWidth = (totalCols * (dotSize.value + spacing.value) - spacing.value).dp
    val totalHeight = (charHeightDots * (dotSize.value + spacing.value) - spacing.value).dp

    Canvas(
        modifier = modifier
            .width(totalWidth)
            .height(totalHeight)
    ) {
        val dotRadiusPx = (dotSize.toPx() / 2f)
        val stepX = dotSize.toPx() + spacing.toPx()
        val stepY = dotSize.toPx() + spacing.toPx()

        var colOffset = 0
        for (ch in upper) {
            val pattern = get5x7Pattern(ch)
            for (row in 0 until 7) {
                for (col in 0 until 5) {
                    val isDotOn = ((pattern[row] shr (4 - col)) and 1) == 1
                    val cx = (colOffset + col) * stepX + dotRadiusPx
                    val cy = row * stepY + dotRadiusPx

                    if (isDotOn) {
                        drawCircle(
                            color = activeColor,
                            radius = dotRadiusPx,
                            center = Offset(cx, cy)
                        )
                    } else if (inactiveColor != Color.Transparent) {
                        drawCircle(
                            color = inactiveColor,
                            radius = dotRadiusPx * 0.5f,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
            colOffset += (charWidthDots + charGapDots)
        }
    }
}

private fun get5x7Pattern(c: Char): IntArray {
    return when (c) {
        'A' -> intArrayOf(0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001)
        'B' -> intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110)
        'C' -> intArrayOf(0b01111, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b01111)
        'D' -> intArrayOf(0b11100, 0b10010, 0b10001, 0b10001, 0b10001, 0b10010, 0b11100)
        'E' -> intArrayOf(0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111)
        'F' -> intArrayOf(0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000)
        'G' -> intArrayOf(0b01111, 0b10000, 0b10000, 0b10011, 0b10001, 0b10001, 0b01110)
        'H' -> intArrayOf(0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001)
        'I' -> intArrayOf(0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110)
        'J' -> intArrayOf(0b00001, 0b00001, 0b00001, 0b00001, 0b10001, 0b10001, 0b01110)
        'K' -> intArrayOf(0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001)
        'L' -> intArrayOf(0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111)
        'M' -> intArrayOf(0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001)
        'N' -> intArrayOf(0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001)
        'O' -> intArrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110)
        'P' -> intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000)
        'Q' -> intArrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101)
        'R' -> intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001)
        'S' -> intArrayOf(0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110)
        'T' -> intArrayOf(0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100)
        'U' -> intArrayOf(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110)
        'V' -> intArrayOf(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100)
        'W' -> intArrayOf(0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001)
        'X' -> intArrayOf(0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001)
        'Y' -> intArrayOf(0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100)
        'Z' -> intArrayOf(0b11111, 0b00010, 0b00100, 0b01000, 0b10000, 0b10000, 0b11111)
        '0' -> intArrayOf(0b01110, 0b10011, 0b10101, 0b10101, 0b11001, 0b10001, 0b01110)
        '1' -> intArrayOf(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110)
        '2' -> intArrayOf(0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111)
        '3' -> intArrayOf(0b01110, 0b10001, 0b00001, 0b00110, 0b00001, 0b10001, 0b01110)
        '4' -> intArrayOf(0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010)
        '5' -> intArrayOf(0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110)
        '6' -> intArrayOf(0b01110, 0b10000, 0b11110, 0b10001, 0b10001, 0b10001, 0b01110)
        '7' -> intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000)
        '8' -> intArrayOf(0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110)
        '9' -> intArrayOf(0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00001, 0b01110)
        ':' -> intArrayOf(0b00000, 0b00100, 0b00000, 0b00000, 0b00100, 0b00000, 0b00000)
        '.' -> intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b01100, 0b01100)
        '-' -> intArrayOf(0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000)
        '/' -> intArrayOf(0b00001, 0b00010, 0b00100, 0b00100, 0b01000, 0b10000, 0b00000)
        '[' -> intArrayOf(0b01110, 0b01000, 0b01000, 0b01000, 0b01000, 0b01000, 0b01110)
        ']' -> intArrayOf(0b01110, 0b00010, 0b00010, 0b00010, 0b00010, 0b00010, 0b01110)
        ' ' -> intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000)
        else -> intArrayOf(0b00000, 0b00100, 0b00100, 0b00000, 0b00100, 0b00100, 0b00000)
    }
}

