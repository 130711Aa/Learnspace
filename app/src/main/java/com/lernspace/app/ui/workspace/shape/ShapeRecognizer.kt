package com.lernspace.app.ui.workspace.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.lernspace.app.ui.workspace.model.GeometricShapeType
import kotlin.math.*

object ShapeRecognizer {

    /**
     * Creates an AndroidX Ink [Stroke] from an ordered list of [Offset] coordinates.
     */
    fun createStrokeFromPoints(points: List<Offset>, brush: Brush): Stroke {
        require(points.size >= 2) { "Need at least 2 points to create a stroke" }
        val batch = MutableStrokeInputBatch()
        points.forEachIndexed { i, pt ->
            val input = StrokeInput()
            input.update(
                x = pt.x,
                y = pt.y,
                elapsedTimeMillis = (i * 12).toLong(),
                toolType = InputToolType.STYLUS,
                strokeUnitLengthCm = 0.0026f,
                pressure = 0.5f
            )
            batch.add(input)
        }
        return Stroke(brush, batch)
    }

    /**
     * Translates an existing [Stroke] by (dx, dy), preserving all pressure and timing attributes.
     */
    fun translateStroke(stroke: Stroke, dx: Float, dy: Float): Stroke {
        val oldInputs = stroke.inputs
        val newBatch = MutableStrokeInputBatch()
        val temp = StrokeInput()
        for (i in 0 until oldInputs.size) {
            oldInputs.populate(i, temp)
            val updated = StrokeInput()
            updated.update(
                x = temp.x + dx,
                y = temp.y + dy,
                elapsedTimeMillis = temp.elapsedTimeMillis,
                toolType = temp.toolType,
                strokeUnitLengthCm = temp.strokeUnitLengthCm,
                pressure = temp.pressure,
                tiltRadians = temp.tiltRadians,
                orientationRadians = temp.orientationRadians
            )
            newBatch.add(updated)
        }
        return Stroke(stroke.brush, newBatch)
    }

    /**
     * Extracts a list of Offset points from an AndroidX Ink Stroke.
     */
    fun extractPoints(stroke: Stroke): List<Offset> {
        val inputs = stroke.inputs
        val list = ArrayList<Offset>(inputs.size)
        val temp = StrokeInput()
        for (i in 0 until inputs.size) {
            inputs.populate(i, temp)
            list.add(Offset(temp.x, temp.y))
        }
        return list
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GEOMETRIC ALGORITHMS: Douglas-Peucker & Shoelace Area
    // ─────────────────────────────────────────────────────────────────────────

    private fun perpendicularDistance(pt: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lineLengthSq = dx * dx + dy * dy
        if (lineLengthSq == 0f) return (pt - lineStart).getDistance()
        val num = abs(dy * pt.x - dx * pt.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x)
        return num / sqrt(lineLengthSq)
    }

    fun douglasPeucker(points: List<Offset>, epsilon: Float): List<Offset> {
        if (points.size < 3) return points

        var maxDist = 0f
        var maxIndex = 0
        val start = points.first()
        val end = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], start, end)
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        return if (maxDist > epsilon) {
            val left = douglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val right = douglasPeucker(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    fun computePolygonArea(points: List<Offset>): Float {
        if (points.size < 3) return 0f
        var sum = 0f
        for (i in points.indices) {
            val j = (i + 1) % points.size
            sum += points[i].x * points[j].y - points[j].x * points[i].y
        }
        return abs(sum) / 2f
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHAPE SAMPLING UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    fun sampleLine(p1: Offset, p2: Offset, numPoints: Int = 14): List<Offset> {
        val points = mutableListOf<Offset>()
        for (i in 0 until numPoints) {
            val t = i.toFloat() / (numPoints - 1)
            points.add(Offset(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t))
        }
        return points
    }

    fun sampleRectangle(minX: Float, minY: Float, maxX: Float, maxY: Float, pointsPerEdge: Int = 10): List<Offset> {
        val points = mutableListOf<Offset>()
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(minX + (maxX - minX) * t, minY))
        }
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(maxX, minY + (maxY - minY) * t))
        }
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(maxX - (maxX - minX) * t, maxY))
        }
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(minX, maxY - (maxY - minY) * t))
        }
        points.add(Offset(minX, minY))
        return points
    }

    fun sampleCircle(cx: Float, cy: Float, rx: Float, ry: Float, numPoints: Int = 48): List<Offset> {
        val points = mutableListOf<Offset>()
        for (i in 0..numPoints) {
            val angle = (i.toFloat() / numPoints) * 2f * Math.PI.toFloat()
            points.add(Offset(cx + rx * cos(angle), cy + ry * sin(angle)))
        }
        return points
    }

    fun sampleTriangle(p1: Offset, p2: Offset, p3: Offset, pointsPerEdge: Int = 12): List<Offset> {
        val points = mutableListOf<Offset>()
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t))
        }
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(p2.x + (p3.x - p2.x) * t, p2.y + (p3.y - p2.y) * t))
        }
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(p3.x + (p1.x - p3.x) * t, p3.y + (p1.y - p3.y) * t))
        }
        points.add(p1)
        return points
    }

    fun sampleArrow(start: Offset, end: Offset): List<Offset> {
        val points = mutableListOf<Offset>()
        points.addAll(sampleLine(start, end, 12))

        val dx = end.x - start.x
        val dy = end.y - start.y
        val angle = atan2(dy, dx)
        val headLength = minOf(36f, hypot(dx, dy) * 0.35f)
        val headAngle = Math.toRadians(30.0).toFloat()

        val leftArm = Offset(
            end.x - headLength * cos(angle - headAngle),
            end.y - headLength * sin(angle - headAngle)
        )
        val rightArm = Offset(
            end.x - headLength * cos(angle + headAngle),
            end.y - headLength * sin(angle + headAngle)
        )

        points.addAll(sampleLine(end, leftArm, 5))
        points.addAll(sampleLine(end, rightArm, 5))
        return points
    }

    fun generateGeometricShape(
        type: GeometricShapeType,
        start: Offset,
        end: Offset,
        brush: Brush
    ): Stroke {
        val minX = minOf(start.x, end.x)
        val maxX = maxOf(start.x, end.x)
        val minY = minOf(start.y, end.y)
        val maxY = maxOf(start.y, end.y)
        val width = maxOf(20f, maxX - minX)
        val height = maxOf(20f, maxY - minY)

        val points = when (type) {
            GeometricShapeType.LINE -> sampleLine(start, end)
            GeometricShapeType.RECTANGLE -> sampleRectangle(minX, minY, maxX, maxY)
            GeometricShapeType.CIRCLE -> {
                val cx = (minX + maxX) / 2f
                val cy = (minY + maxY) / 2f
                sampleCircle(cx, cy, width / 2f, height / 2f)
            }
            GeometricShapeType.TRIANGLE -> {
                val pTop = Offset((minX + maxX) / 2f, minY)
                val pBottomRight = Offset(maxX, maxY)
                val pBottomLeft = Offset(minX, maxY)
                sampleTriangle(pTop, pBottomRight, pBottomLeft)
            }
            GeometricShapeType.ARROW -> sampleArrow(start, end)
        }
        return createStrokeFromPoints(points, brush)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACCURATE SHAPE RECOGNITION (HOLD-TO-REFINE)
    // ─────────────────────────────────────────────────────────────────────────

    fun refineStroke(stroke: Stroke): Stroke? {
        val rawPoints = extractPoints(stroke)
        if (rawPoints.size < 6) return null

        val start = rawPoints.first()
        val end = rawPoints.last()

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        var totalPathLength = 0f
        for (i in 0 until rawPoints.size) {
            val p = rawPoints[i]
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
            if (i > 0) {
                totalPathLength += (p - rawPoints[i - 1]).getDistance()
            }
        }

        val width = maxX - minX
        val height = maxY - minY
        val diagonal = hypot(width, height)
        if (diagonal < 35f || totalPathLength < 40f) return null

        val startEndDist = (end - start).getDistance()

        // ── 1. STRAIGHT LINE RECOGNITION ──
        // If start-to-end distance is a large portion of the total path length, it's open & straight!
        val straightness = startEndDist / totalPathLength
        if (straightness > 0.80f && startEndDist > 45f && (startEndDist / diagonal > 0.60f)) {
            var snappedEnd = end
            // Snap to horizontal if angle is within ~8 degrees
            if (abs(end.y - start.y) < 0.12f * startEndDist) {
                snappedEnd = Offset(end.x, start.y)
            }
            // Snap to vertical if angle is within ~8 degrees
            else if (abs(end.x - start.x) < 0.12f * startEndDist) {
                snappedEnd = Offset(start.x, end.y)
            }
            // Snap to 45 degree diagonal
            else if (abs(abs(end.x - start.x) - abs(end.y - start.y)) < 0.14f * startEndDist) {
                val signX = if (end.x >= start.x) 1f else -1f
                val signY = if (end.y >= start.y) 1f else -1f
                val avgLen = (abs(end.x - start.x) + abs(end.y - start.y)) / 2f
                snappedEnd = Offset(start.x + signX * avgLen, start.y + signY * avgLen)
            }
            return createStrokeFromPoints(sampleLine(start, snappedEnd), stroke.brush)
        }

        // ── 2. CLOSED GEOMETRIC SHAPES (Triangle, Rectangle, Circle/Oval) ──
        val isClosed = (startEndDist / diagonal < 0.50f) || (startEndDist < 80f)
        if (isClosed) {
            val cx = (minX + maxX) / 2f
            val cy = (minY + maxY) / 2f
            val boxArea = maxOf(1f, width * height)
            val polyArea = computePolygonArea(rawPoints)
            val areaRatio = polyArea / boxArea // Triangle: <= 0.60 | Circle: ~0.785 | Box: >= 0.78

            // Simplify using Douglas-Peucker to identify distinct corner vertices
            val simplified = douglasPeucker(rawPoints, epsilon = diagonal * 0.08f)
            val cornerCount = maxOf(3, simplified.size - 1)

            // Measure circular variance
            val rx = width / 2f
            val ry = height / 2f
            var sumNormalizedDistDiff = 0f
            for (p in rawPoints) {
                val dxNorm = (p.x - cx) / maxOf(1f, rx)
                val dyNorm = (p.y - cy) / maxOf(1f, ry)
                val distNorm = sqrt(dxNorm * dxNorm + dyNorm * dyNorm)
                sumNormalizedDistDiff += abs(distNorm - 1f)
            }
            val avgCircleDeviation = sumNormalizedDistDiff / rawPoints.size

            // ── CHECK A: CIRCLE / OVAL ──
            // Circle has smooth curve, low variance from ellipse formula (avgCircleDeviation < 0.18),
            // and areaRatio around pi/4 (~0.785)
            if (avgCircleDeviation < 0.18f && areaRatio in 0.60f..0.92f) {
                val isNearSquare = abs(width - height) / maxOf(width, height) < 0.22f
                val finalRx = if (isNearSquare) (rx + ry) / 2f else rx
                val finalRy = if (isNearSquare) (rx + ry) / 2f else ry
                return createStrokeFromPoints(sampleCircle(cx, cy, finalRx, finalRy), stroke.brush)
            }

            // ── CHECK B: TRIANGLE ──
            // Key invariant: Triangle area can NEVER exceed 0.50-0.62 of its bounding box!
            // Rectangle has areaRatio > 0.70, so areaRatio <= 0.65 is mathematically a Triangle!
            if (areaRatio <= 0.65f) {
                // Find 3 vertices that maximize the inscribed triangle area
                val candidates = if (simplified.size >= 3) {
                    simplified
                } else {
                    rawPoints.filterIndexed { idx, _ -> idx % maxOf(1, rawPoints.size / 16) == 0 }
                }

                var bestArea = -1f
                var bestCorners: List<Offset>? = null

                for (i in 0 until candidates.size) {
                    for (j in i + 1 until candidates.size) {
                        for (k in j + 1 until candidates.size) {
                            val pA = candidates[i]
                            val pB = candidates[j]
                            val pC = candidates[k]
                            val triArea = abs((pB.x - pA.x) * (pC.y - pA.y) - (pC.x - pA.x) * (pB.y - pA.y)) / 2f
                            if (triArea > bestArea) {
                                bestArea = triArea
                                bestCorners = listOf(pA, pB, pC)
                            }
                        }
                    }
                }

                val pts = (bestCorners ?: listOf(
                    Offset(cx, minY),
                    Offset(maxX, maxY),
                    Offset(minX, maxY)
                )).toMutableList()

                // Straighten nearly horizontal base or vertical edges
                for (i in 0..2) {
                    val j = (i + 1) % 3
                    if (abs(pts[i].y - pts[j].y) < maxOf(8f, height * 0.08f)) {
                        val avgY = (pts[i].y + pts[j].y) / 2f
                        pts[i] = Offset(pts[i].x, avgY)
                        pts[j] = Offset(pts[j].x, avgY)
                    }
                    if (abs(pts[i].x - pts[j].x) < maxOf(8f, width * 0.08f)) {
                        val avgX = (pts[i].x + pts[j].x) / 2f
                        pts[i] = Offset(avgX, pts[i].y)
                        pts[j] = Offset(avgX, pts[j].y)
                    }
                }

                return createStrokeFromPoints(sampleTriangle(pts[0], pts[1], pts[2]), stroke.brush)
            }

            // ── CHECK C: RECTANGLE / SQUARE ──
            // Rectangle fills almost the entire bounding box (areaRatio >= 0.68)
            if (areaRatio >= 0.68f) {
                val isSquare = abs(width - height) / maxOf(width, height) < 0.18f
                val finalW = if (isSquare) maxOf(width, height) else width
                val finalH = if (isSquare) maxOf(width, height) else height
                val fMinX = if (isSquare) cx - finalW / 2f else minX
                val fMaxX = if (isSquare) cx + finalW / 2f else maxX
                val fMinY = if (isSquare) cy - finalH / 2f else minY
                val fMaxY = if (isSquare) cy + finalH / 2f else maxY
                return createStrokeFromPoints(sampleRectangle(fMinX, fMinY, fMaxX, fMaxY), stroke.brush)
            }
        }

        return null
    }
}
