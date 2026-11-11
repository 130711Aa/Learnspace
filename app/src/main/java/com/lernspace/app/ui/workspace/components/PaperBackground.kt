package com.lernspace.app.ui.workspace.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.lernspace.app.ui.workspace.model.CanvasConstants
import com.lernspace.app.ui.workspace.model.PaperStyle
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun PaperBackground(
    style: PaperStyle,
    scaleProvider: () -> Float = { 1f },
    panXProvider: () -> Float = { 0f },
    panYProvider: () -> Float = { 0f },
    isPaged: Boolean = true,
    pageWidth: Float = CanvasConstants.PAGE_WIDTH,
    pageHeight: Float = CanvasConstants.PAGE_HEIGHT,
    isDarkPaper: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dotPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).also { p ->
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
        }
    }
    var dotCoords = remember { FloatArray(4096) }

    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
        val scale = scaleProvider()
        val panX = panXProvider()
        val panY = panYProvider()

        clipRect(0f, 0f, size.width, size.height) {
            // Colors adaptive to Light / Dark Paper mode
            val deskColor = if (isDarkPaper) Color(0xFF091426) else Color(0xFFE2E8F0)
            val sheetColor = if (isDarkPaper) Color(0xFF131822) else Color(0xFFFCFCFD)
            val shadowColor = if (isDarkPaper) Color(0x66000000) else Color(0x1F0F172A)
            val borderColor = if (isDarkPaper) Color(0xFF263245) else Color(0xFFCBD5E1)
            val marginLineColor = if (isDarkPaper) Color(0xFFEF4444) else Color(0xFFFCA5A5)
            val ruledLineColor = if (isDarkPaper) Color(0xFF242F42) else Color(0xFFE2E8F0)
            val gridLineColor = if (isDarkPaper) Color(0xFF202A3C) else Color(0xFFEEF2F6)
            val dotColor = if (isDarkPaper) Color(0xFF475569) else Color(0xFFCBD5E1)
            val infiniteBgColor = if (isDarkPaper) Color(0xFF131822) else Color(0xFFFAFAFA)
            val blankInfiniteColor = if (isDarkPaper) Color(0xFF131822) else Color.White

            if (isPaged) {
                // Desk background (desk canvas surrounding the physical page sheet)
                drawRect(deskColor)

                val sheetLeft = panX
                val sheetTop = panY
                val sheetWidth = pageWidth * scale
                val sheetHeight = pageHeight * scale
                val sheetRight = sheetLeft + sheetWidth
                val sheetBottom = sheetTop + sheetHeight

                // Drop shadow behind the paper sheet
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(sheetLeft + 4f, sheetTop + 6f),
                    size = Size(sheetWidth, sheetHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Physical paper sheet
                drawRoundRect(
                    color = sheetColor,
                    topLeft = Offset(sheetLeft, sheetTop),
                    size = Size(sheetWidth, sheetHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Sheet border
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(sheetLeft, sheetTop),
                    size = Size(sheetWidth, sheetHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 1.5f)
                )

                // Visible bounds intersection with sheet
                val visLeft = maxOf(sheetLeft, 0f)
                val visRight = minOf(sheetRight, size.width)
                val visTop = maxOf(sheetTop, 0f)
                val visBottom = minOf(sheetBottom, size.height)
                if (visRight <= visLeft || visBottom <= visTop) return@clipRect

                // Draw Paper Template STRICTLY within visible sheet region
                when (style) {
                    PaperStyle.RULED -> {
                        val marginScreenX = sheetLeft + (CanvasConstants.RULED_MARGIN_X * scale)
                        if (marginScreenX in visLeft..visRight) {
                            drawLine(
                                color = marginLineColor,
                                start = Offset(marginScreenX, visTop),
                                end = Offset(marginScreenX, visBottom),
                                strokeWidth = (2.2f * scale).coerceIn(1.5f, 4f)
                            )
                        }

                        val lineStepScreen = CanvasConstants.RULED_LINE_SPACING * scale
                        if (lineStepScreen > 2f) {
                            var y = CanvasConstants.RULED_FIRST_LINE_Y
                            while (y < pageHeight - 40f) {
                                val lineScreenY = sheetTop + (y * scale)
                                if (lineScreenY in visTop..visBottom) {
                                    drawLine(
                                        color = ruledLineColor,
                                        start = Offset(visLeft, lineScreenY),
                                        end = Offset(visRight, lineScreenY),
                                        strokeWidth = 1.2f
                                    )
                                }
                                y += CanvasConstants.RULED_LINE_SPACING
                            }
                        }
                    }

                    PaperStyle.GRID -> {
                        val step = CanvasConstants.GRID_SPACING * scale
                        if (step > 4f) {
                            val firstCol = maxOf(1, ceil(((visLeft - sheetLeft) / step).toDouble()).toInt())
                            var x = sheetLeft + firstCol * step
                            while (x < visRight) {
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(x, visTop),
                                    end = Offset(x, visBottom),
                                    strokeWidth = 1f
                                )
                                x += step
                            }
                            val firstRow = maxOf(1, ceil(((visTop - sheetTop) / step).toDouble()).toInt())
                            var y = sheetTop + firstRow * step
                            while (y < visBottom) {
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(visLeft, y),
                                    end = Offset(visRight, y),
                                    strokeWidth = 1f
                                )
                                y += step
                            }
                        }
                    }

                    PaperStyle.DOTTED -> {
                        val step = CanvasConstants.DOT_SPACING * scale
                        if (step > 8f) {
                            val firstCol = maxOf(1, ceil(((visLeft - sheetLeft) / step).toDouble()).toInt())
                            val firstRow = maxOf(1, ceil(((visTop - sheetTop) / step).toDouble()).toInt())
                            val maxCols = ((visRight - (sheetLeft + firstCol * step)) / step).toInt().coerceAtLeast(0) + 1
                            val maxRows = ((visBottom - (sheetTop + firstRow * step)) / step).toInt().coerceAtLeast(0) + 1
                            val neededFloats = maxCols * maxRows * 2
                            if (neededFloats > 0) {
                                if (dotCoords.size < neededFloats) {
                                    dotCoords = FloatArray(neededFloats)
                                }
                                var count = 0
                                var x = sheetLeft + firstCol * step
                                while (x < visRight) {
                                    var y = sheetTop + firstRow * step
                                    while (y < visBottom) {
                                        if (count + 1 < dotCoords.size) {
                                            dotCoords[count++] = x
                                            dotCoords[count++] = y
                                        }
                                        y += step
                                    }
                                    x += step
                                }
                                if (count >= 2) {
                                    dotPaint.color = dotColor.toArgb()
                                    dotPaint.strokeWidth = (1.5f * scale).coerceIn(1f, 3f) * 2f
                                    drawContext.canvas.nativeCanvas.drawPoints(dotCoords, 0, count, dotPaint)
                                }
                            }
                        }
                    }

                    PaperStyle.BLANK -> Unit
                }
            } else {
                // Infinite Scratchpad Mode: Endless surface without sheet borders
                when (style) {
                    PaperStyle.RULED -> {
                        drawRect(infiniteBgColor)
                        val marginX = (CanvasConstants.RULED_MARGIN_X * scale) + panX
                        if (marginX in 0f..size.width) {
                            drawLine(
                                color = marginLineColor,
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
                                        color = ruledLineColor,
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
                        drawRect(infiniteBgColor)
                        val spacing = CanvasConstants.GRID_SPACING * scale
                        if (spacing > 4f) {
                            var x = ((panX % spacing) + spacing) % spacing
                            while (x < size.width) {
                                drawLine(gridLineColor, Offset(x, 0f), Offset(x, size.height), 1f)
                                x += spacing
                            }
                            var y = ((panY % spacing) + spacing) % spacing
                            while (y < size.height) {
                                drawLine(gridLineColor, Offset(0f, y), Offset(size.width, y), 1f)
                                y += spacing
                            }
                        }
                    }
                    PaperStyle.DOTTED -> {
                        drawRect(infiniteBgColor)
                        val spacing = CanvasConstants.DOT_SPACING * scale
                        if (spacing > 8f) {
                            val startX = ((panX % spacing) + spacing) % spacing
                            val startY = ((panY % spacing) + spacing) % spacing
                            val maxCols = (size.width / spacing).toInt() + 2
                            val maxRows = (size.height / spacing).toInt() + 2
                            val neededFloats = maxCols * maxRows * 2
                            if (dotCoords.size < neededFloats) {
                                dotCoords = FloatArray(neededFloats)
                            }
                            var count = 0
                            var x = startX
                            while (x < size.width) {
                                var y = startY
                                while (y < size.height) {
                                    if (count + 1 < dotCoords.size) {
                                        dotCoords[count++] = x
                                        dotCoords[count++] = y
                                    }
                                    y += spacing
                                }
                                x += spacing
                            }
                            if (count >= 2) {
                                dotPaint.color = dotColor.toArgb()
                                dotPaint.strokeWidth = (1.5f * scale).coerceIn(1f, 3f) * 2f
                                drawContext.canvas.nativeCanvas.drawPoints(dotCoords, 0, count, dotPaint)
                            }
                        }
                    }
                    PaperStyle.BLANK -> {
                        drawRect(blankInfiniteColor)
                    }
                }
            }
        }
    }
}
