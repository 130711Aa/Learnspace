package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.workspace.model.PaperStyle
import kotlin.math.floor

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

                // Left red notebook margin line (relative to world coordinate 72dp)
                val marginX = (72.dp.toPx() * scale) + panX
                if (marginX in 0f..size.width) {
                    drawLine(
                        color = Color(0xFFFCA5A5),
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, size.height),
                        strokeWidth = (2f * scale).coerceIn(1.5f, 4f)
                    )
                }

                // Ruled horizontal lines (relative to world coordinate origin)
                val firstLineY = (56.dp.toPx() * scale) + panY
                val lineSpacing = 32.dp.toPx() * scale
                if (lineSpacing > 4f) {
                    val kMin = floor(-firstLineY / lineSpacing).toInt()
                    var k = kMin
                    while (true) {
                        val y = firstLineY + (k * lineSpacing)
                        if (y > size.height) break
                        if (y >= 0f) {
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }
                        k++
                    }
                }
            }

            PaperStyle.GRID -> {
                drawRect(Color(0xFFFAFAFA))
                val spacing = 28.dp.toPx() * scale
                if (spacing > 4f) {
                    val firstX = panX
                    val kMinX = floor(-firstX / spacing).toInt()
                    var kx = kMinX
                    while (true) {
                        val x = firstX + (kx * spacing)
                        if (x > size.width) break
                        if (x >= 0f) {
                            drawLine(
                                color = Color(0xFFEEF2F6),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                        }
                        kx++
                    }

                    val firstY = panY
                    val kMinY = floor(-firstY / spacing).toInt()
                    var ky = kMinY
                    while (true) {
                        val y = firstY + (ky * spacing)
                        if (y > size.height) break
                        if (y >= 0f) {
                            drawLine(
                                color = Color(0xFFEEF2F6),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }
                        ky++
                    }
                }
            }

            PaperStyle.DOTTED -> {
                drawRect(Color(0xFFFAFAFA))
                val spacing = 28.dp.toPx() * scale
                if (spacing > 8f) {
                    val firstX = panX
                    val kMinX = floor(-firstX / spacing).toInt()
                    val firstY = panY
                    val kMinY = floor(-firstY / spacing).toInt()

                    var kx = kMinX
                    while (true) {
                        val x = firstX + (kx * spacing)
                        if (x > size.width) break
                        if (x >= 0f) {
                            var ky = kMinY
                            while (true) {
                                val y = firstY + (ky * spacing)
                                if (y > size.height) break
                                if (y >= 0f) {
                                    drawCircle(
                                        color = Color(0xFFCBD5E1),
                                        radius = (1.5f * scale).coerceIn(1f, 3f),
                                        center = Offset(x, y)
                                    )
                                }
                                ky++
                            }
                        }
                        kx++
                    }
                }
            }

            PaperStyle.BLANK -> {
                drawRect(Color.White)
            }
        }
    }
}
