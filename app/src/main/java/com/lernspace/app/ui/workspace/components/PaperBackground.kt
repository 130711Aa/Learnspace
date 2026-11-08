package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lernspace.app.ui.workspace.model.CanvasConstants
import com.lernspace.app.ui.workspace.model.PaperStyle
import kotlin.math.floor

@Composable
fun PaperBackground(
    style: PaperStyle,
    scale: Float = 1f,
    panX: Float = 0f,
    panY: Float = 0f,
    isPaged: Boolean = true,
    pageWidth: Float = CanvasConstants.PAGE_WIDTH,
    pageHeight: Float = CanvasConstants.PAGE_HEIGHT,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (isPaged) {
            // Desk background (desk canvas surrounding the physical page sheet)
            drawRect(Color(0xFFE2E8F0))

            val sheetLeft = panX
            val sheetTop = panY
            val sheetWidth = pageWidth * scale
            val sheetHeight = pageHeight * scale

            // Drop shadow behind the paper sheet
            drawRoundRect(
                color = Color(0x1F0F172A),
                topLeft = Offset(sheetLeft + 4f, sheetTop + 6f),
                size = Size(sheetWidth, sheetHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Pure physical white paper sheet
            drawRoundRect(
                color = Color(0xFFFCFCFD),
                topLeft = Offset(sheetLeft, sheetTop),
                size = Size(sheetWidth, sheetHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Sheet border
            drawRoundRect(
                color = Color(0xFFCBD5E1),
                topLeft = Offset(sheetLeft, sheetTop),
                size = Size(sheetWidth, sheetHeight),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 1.5f)
            )

            // Draw Paper Template STRICTLY within the sheet
            when (style) {
                PaperStyle.RULED -> {
                    // Left notebook red margin line (exact CanvasConstants.RULED_MARGIN_X)
                    val marginScreenX = sheetLeft + (CanvasConstants.RULED_MARGIN_X * scale)
                    if (marginScreenX in sheetLeft..(sheetLeft + sheetWidth)) {
                        drawLine(
                            color = Color(0xFFFCA5A5),
                            start = Offset(marginScreenX, sheetTop),
                            end = Offset(marginScreenX, sheetTop + sheetHeight),
                            strokeWidth = (2.2f * scale).coerceIn(1.5f, 4f)
                        )
                    }

                    // Horizontal ruled lines (exact CanvasConstants.RULED_FIRST_LINE_Y & RULED_LINE_SPACING)
                    var y = CanvasConstants.RULED_FIRST_LINE_Y
                    while (y < pageHeight - 40f) {
                        val lineScreenY = sheetTop + (y * scale)
                        if (lineScreenY in sheetTop..(sheetTop + sheetHeight)) {
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(sheetLeft, lineScreenY),
                                end = Offset(sheetLeft + sheetWidth, lineScreenY),
                                strokeWidth = 1.2f
                            )
                        }
                        y += CanvasConstants.RULED_LINE_SPACING
                    }
                }

                PaperStyle.GRID -> {
                    val step = CanvasConstants.GRID_SPACING * scale
                    if (step > 4f) {
                        var x = sheetLeft + step
                        while (x < sheetLeft + sheetWidth) {
                            drawLine(
                                color = Color(0xFFEEF2F6),
                                start = Offset(x, sheetTop),
                                end = Offset(x, sheetTop + sheetHeight),
                                strokeWidth = 1f
                            )
                            x += step
                        }
                        var y = sheetTop + step
                        while (y < sheetTop + sheetHeight) {
                            drawLine(
                                color = Color(0xFFEEF2F6),
                                start = Offset(sheetLeft, y),
                                end = Offset(sheetLeft + sheetWidth, y),
                                strokeWidth = 1f
                            )
                            y += step
                        }
                    }
                }

                PaperStyle.DOTTED -> {
                    val step = CanvasConstants.DOT_SPACING * scale
                    if (step > 8f) {
                        var x = sheetLeft + step
                        while (x < sheetLeft + sheetWidth) {
                            var y = sheetTop + step
                            while (y < sheetTop + sheetHeight) {
                                drawCircle(
                                    color = Color(0xFFCBD5E1),
                                    radius = (1.5f * scale).coerceIn(1f, 3f),
                                    center = Offset(x, y)
                                )
                                y += step
                            }
                            x += step
                        }
                    }
                }

                PaperStyle.BLANK -> {
                    // Crisp white sheet already drawn
                }
            }
        } else {
            // Infinite Scratchpad Mode: Endless surface without sheet borders
            when (style) {
                PaperStyle.RULED -> {
                    drawRect(Color(0xFFFAFAFA))
                    val marginX = (CanvasConstants.RULED_MARGIN_X * scale) + panX
                    if (marginX in 0f..size.width) {
                        drawLine(
                            color = Color(0xFFFCA5A5),
                            start = Offset(marginX, 0f),
                            end = Offset(marginX, size.height),
                            strokeWidth = (2f * scale).coerceIn(1.5f, 4f)
                        )
                    }
                    val lineSpacing = CanvasConstants.RULED_LINE_SPACING * scale
                    if (lineSpacing > 4f) {
                        val firstLineY = (CanvasConstants.RULED_FIRST_LINE_Y * scale) + panY
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
                    val spacing = CanvasConstants.GRID_SPACING * scale
                    if (spacing > 4f) {
                        var x = panX % spacing
                        while (x < size.width) {
                            drawLine(Color(0xFFEEF2F6), Offset(x, 0f), Offset(x, size.height), 1f)
                            x += spacing
                        }
                        var y = panY % spacing
                        while (y < size.height) {
                            drawLine(Color(0xFFEEF2F6), Offset(0f, y), Offset(size.width, y), 1f)
                            y += spacing
                        }
                    }
                }
                PaperStyle.DOTTED -> {
                    drawRect(Color(0xFFFAFAFA))
                    val spacing = CanvasConstants.DOT_SPACING * scale
                    if (spacing > 8f) {
                        var x = panX % spacing
                        while (x < size.width) {
                            var y = panY % spacing
                            while (y < size.height) {
                                drawCircle(Color(0xFFCBD5E1), (1.5f * scale).coerceIn(1f, 3f), Offset(x, y))
                                y += spacing
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
}
