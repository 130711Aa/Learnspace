package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.workspace.model.PaperStyle

@Composable
fun PaperBackground(
    style: PaperStyle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        when (style) {
            PaperStyle.RULED -> {
                // Warm paper background
                drawRect(Color(0xFFFAFAFA))

                // Red margin line on the left (typical notebook margin)
                val marginX = 72.dp.toPx()
                drawLine(
                    color = Color(0xFFFCA5A5),
                    start = Offset(marginX, 0f),
                    end = Offset(marginX, size.height),
                    strokeWidth = 2f
                )

                // Ruled horizontal lines
                val lineSpacing = 32.dp.toPx()
                var currentY = 56.dp.toPx()
                while (currentY < size.height) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, currentY),
                        end = Offset(size.width, currentY),
                        strokeWidth = 1f
                    )
                    currentY += lineSpacing
                }
            }

            PaperStyle.GRID -> {
                drawRect(Color(0xFFFAFAFA))
                val spacing = 28.dp.toPx()
                var x = spacing
                while (x < size.width) {
                    drawLine(
                        color = Color(0xFFEEF2F6),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += spacing
                }
                var y = spacing
                while (y < size.height) {
                    drawLine(
                        color = Color(0xFFEEF2F6),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += spacing
                }
            }

            PaperStyle.DOTTED -> {
                drawRect(Color(0xFFFAFAFA))
                val spacing = 28.dp.toPx()
                var x = spacing
                while (x < size.width) {
                    var y = spacing
                    while (y < size.height) {
                        drawCircle(
                            color = Color(0xFFCBD5E1),
                            radius = 1.5f,
                            center = Offset(x, y)
                        )
                        y += spacing
                    }
                    x += spacing
                }
            }

            PaperStyle.BLANK -> {
                drawRect(Color.White)
            }
        }
    }
}

