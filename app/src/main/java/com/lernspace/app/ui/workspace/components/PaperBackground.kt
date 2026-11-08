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
    scale: Float = 1f,
    panX: Float = 0f,
    panY: Float = 0f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        when (style) {
            PaperStyle.RULED -> {
                // Warm paper background
                drawRect(Color(0xFFFAFAFA))

                // Red margin line on the left (typical notebook margin)
                val marginX = (72.dp.toPx() * scale) + panX
                if (marginX in 0f..size.width) {
                    drawLine(
                        color = Color(0xFFFCA5A5),
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, size.height),
                        strokeWidth = (2f * scale).coerceIn(1.5f, 4f)
                    )
                }

                // Ruled horizontal lines
                val lineSpacing = 32.dp.toPx() * scale
                if (lineSpacing > 4f) {
                    val rem = panY % lineSpacing
                    val startY = if (rem > 0) rem - lineSpacing else rem
                    var currentY = startY
                    while (currentY < size.height) {
                        if (currentY >= 0f) {
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(0f, currentY),
                                end = Offset(size.width, currentY),
                                strokeWidth = 1f
                            )
                        }
                        currentY += lineSpacing
                    }
                }
            }

            PaperStyle.GRID -> {
                drawRect(Color(0xFFFAFAFA))
                val spacing = 28.dp.toPx() * scale
                if (spacing > 4f) {
                    val remX = panX % spacing
                    val startX = if (remX > 0) remX - spacing else remX
                    var x = startX
                    while (x < size.width) {
                        if (x >= 0f) {
                            drawLine(
                                color = Color(0xFFEEF2F6),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                        }
                        x += spacing
                    }

                    val remY = panY % spacing
                    val startY = if (remY > 0) remY - spacing else remY
                    var y = startY
                    while (y < size.height) {
                        if (y >= 0f) {
                            drawLine(
                                color = Color(0xFFEEF2F6),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }
                        y += spacing
                    }
                }
            }

            PaperStyle.DOTTED -> {
                drawRect(Color(0xFFFAFAFA))
                val spacing = 28.dp.toPx() * scale
                if (spacing > 8f) {
                    val remX = panX % spacing
                    val startX = if (remX > 0) remX - spacing else remX
                    var x = startX
                    while (x < size.width) {
                        if (x >= 0f) {
                            val remY = panY % spacing
                            val startY = if (remY > 0) remY - spacing else remY
                            var y = startY
                            while (y < size.height) {
                                if (y >= 0f) {
                                    drawCircle(
                                        color = Color(0xFFCBD5E1),
                                        radius = (1.5f * scale).coerceIn(1f, 3f),
                                        center = Offset(x, y)
                                    )
                                }
                                y += spacing
                            }
                        }
                        x += spacing
                    }
                }
            }

            PaperStyle.BLANK -> {
                drawRect(Color.White)
            }
        }
    }
}
