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

    /**
     * Gets the axis-aligned bounding box of a Stroke.
     */
    fun getBoundingBox(stroke: Stroke): Rect {
        val inputs = stroke.inputs
        if (inputs.size == 0) return Rect.Zero
        val temp = StrokeInput()
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (i in 0 until inputs.size) {
            inputs.populate(i, temp)
            if (temp.x < minX) minX = temp.x
            if (temp.x > maxX) maxX = temp.x
            if (temp.y < minY) minY = temp.y
            if (temp.y > maxY) maxY = temp.y
        }
        return Rect(minX, minY, maxX, maxY)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHAPE SAMPLING UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    fun sampleLine(p1: Offset, p2: Offset, numPoints: Int = 12): List<Offset> {
        val points = mutableListOf<Offset>()
        for (i in 0 until numPoints) {
            val t = i.toFloat() / (numPoints - 1)
            points.add(Offset(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t))
        }
        return points
    }

    fun sampleRectangle(minX: Float, minY: Float, maxX: Float, maxY: Float, pointsPerEdge: Int = 8): List<Offset> {
        val points = mutableListOf<Offset>()
        // Top edge: (minX, minY) -> (maxX, minY)
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(minX + (maxX - minX) * t, minY))
        }
        // Right edge: (maxX, minY) -> (maxX, maxY)
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(maxX, minY + (maxY - minY) * t))
        }
        // Bottom edge: (maxX, maxY) -> (minX, maxY)
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(maxX - (maxX - minX) * t, maxY))
        }
        // Left edge: (minX, maxY) -> (minX, minY)
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(minX, maxY - (maxY - minY) * t))
        }
        // Close polygon
        points.add(Offset(minX, minY))
        return points
    }

    fun sampleCircle(cx: Float, cy: Float, rx: Float, ry: Float, numPoints: Int = 40): List<Offset> {
        val points = mutableListOf<Offset>()
        for (i in 0..numPoints) {
            val angle = (i.toFloat() / numPoints) * 2f * Math.PI.toFloat()
            points.add(Offset(cx + rx * cos(angle), cy + ry * sin(angle)))
        }
        return points
    }

    fun sampleTriangle(p1: Offset, p2: Offset, p3: Offset, pointsPerEdge: Int = 10): List<Offset> {
        val points = mutableListOf<Offset>()
        // Side 1: p1 -> p2
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t))
        }
        // Side 2: p2 -> p3
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(p2.x + (p3.x - p2.x) * t, p2.y + (p3.y - p2.y) * t))
        }
        // Side 3: p3 -> p1
        for (i in 0 until pointsPerEdge) {
            val t = i.toFloat() / pointsPerEdge
            points.add(Offset(p3.x + (p1.x - p3.x) * t, p3.y + (p1.y - p3.y) * t))
        }
        // Close polygon
        points.add(p1)
        return points
    }

    fun sampleArrow(start: Offset, end: Offset): List<Offset> {
        val points = mutableListOf<Offset>()
        // Main stem
        points.addAll(sampleLine(start, end, 10))

        // Arrowhead
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

        // Draw left barb then right barb
        points.addAll(sampleLine(end, leftArm, 4))
        points.addAll(sampleLine(end, rightArm, 4))
        return points
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPLICIT SHAPE CREATION (FOR SHAPE TOOL)
    // ─────────────────────────────────────────────────────────────────────────

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
    // AUTO-REFINE / SHAPE RECOGNITION (SMART AUTO-SNAP)
    // ─────────────────────────────────────────────────────────────────────────

    fun refineStroke(stroke: Stroke): Stroke? {
        val points = extractPoints(stroke)
        if (points.size < 6) return null

        val start = points.first()
        val end = points.last()

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        var totalPathLength = 0f
        for (i in 0 until points.size) {
            val p = points[i]
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
            if (i > 0) {
                totalPathLength += (p - points[i - 1]).getDistance()
            }
        }

        val width = maxX - minX
        val height = maxY - minY
        val diagonal = hypot(width, height)
        if (diagonal < 35f || totalPathLength < 40f) return null

        val startEndDist = (end - start).getDistance()

        // ── 1. Straight Line Recognition ──
        val straightness = startEndDist / totalPathLength
        if (straightness > 0.84f && startEndDist > 45f) {
            var snappedEnd = end
            // Snap to horizontal line if slope is very small
            if (abs(end.y - start.y) < 0.09f * startEndDist) {
                snappedEnd = Offset(end.x, start.y)
            }
            // Snap to vertical line if slope is very small
            else if (abs(end.x - start.x) < 0.09f * startEndDist) {
                snappedEnd = Offset(start.x, end.y)
            }
            return createStrokeFromPoints(sampleLine(start, snappedEnd), stroke.brush)
        }

        // ── 2. Closed Shapes (Circle / Oval, Rectangle, Triangle) ──
        val isClosed = startEndDist / diagonal < 0.38f || startEndDist < 60f
        if (isClosed) {
            val cx = (minX + maxX) / 2f
            val cy = (minY + maxY) / 2f
            val rx = width / 2f
            val ry = height / 2f

            // A. Test Circle / Oval fit
            var sumNormalizedDistDiff = 0f
            for (p in points) {
                val dxNorm = (p.x - cx) / maxOf(1f, rx)
                val dyNorm = (p.y - cy) / maxOf(1f, ry)
                val distNorm = sqrt(dxNorm * dxNorm + dyNorm * dyNorm)
                sumNormalizedDistDiff += abs(distNorm - 1f)
            }
            val avgCircleDeviation = sumNormalizedDistDiff / points.size

            if (avgCircleDeviation < 0.17f && rx > 15f && ry > 15f) {
                // If aspect ratio is close to 1:1, snap to perfect circle
                val isNearSquare = abs(width - height) / maxOf(width, height) < 0.22f
                val finalRx = if (isNearSquare) (rx + ry) / 2f else rx
                val finalRy = if (isNearSquare) (rx + ry) / 2f else ry
                return createStrokeFromPoints(sampleCircle(cx, cy, finalRx, finalRy), stroke.brush)
            }

            // B. Test Rectangle / Box fit
            // Check corner count and points proximity to bounding box perimeter
            var pointsNearEdge = 0
            val edgeTolerance = minOf(24f, diagonal * 0.10f)
            for (p in points) {
                val dLeft = abs(p.x - minX)
                val dRight = abs(p.x - maxX)
                val dTop = abs(p.y - minY)
                val dBottom = abs(p.y - maxY)
                val minDistToEdge = minOf(dLeft, dRight, dTop, dBottom)
                if (minDistToEdge <= edgeTolerance) {
                    pointsNearEdge++
                }
            }
            val edgeRatio = pointsNearEdge.toFloat() / points.size
            if (edgeRatio > 0.68f && width > 25f && height > 25f) {
                val isSquare = abs(width - height) / maxOf(width, height) < 0.15f
                val finalW = if (isSquare) maxOf(width, height) else width
                val finalH = if (isSquare) maxOf(width, height) else height
                val fMinX = if (isSquare) cx - finalW / 2f else minX
                val fMaxX = if (isSquare) cx + finalW / 2f else maxX
                val fMinY = if (isSquare) cy - finalH / 2f else minY
                val fMaxY = if (isSquare) cy + finalH / 2f else maxY
                return createStrokeFromPoints(sampleRectangle(fMinX, fMinY, fMaxX, fMaxY), stroke.brush)
            }

            // C. Test Triangle
            // Find apex (point closest to top center or furthest from opposite baseline)
            val topApex = Offset((minX + maxX) / 2f, minY)
            val botRight = Offset(maxX, maxY)
            val botLeft = Offset(minX, maxY)
            // If the user drew a roughly triangular form
            if (width > 30f && height > 30f && avgCircleDeviation > 0.20f) {
                // Check if top portion has much smaller horizontal spread than bottom
                val topPoints = points.filter { it.y < minY + height * 0.3f }
                val botPoints = points.filter { it.y > minY + height * 0.7f }
                if (topPoints.isNotEmpty() && botPoints.isNotEmpty()) {
                    val topWidth = (topPoints.maxOf { it.x } - topPoints.minOf { it.x })
                    val botWidth = (botPoints.maxOf { it.x } - botPoints.minOf { it.x })
                    if (topWidth < botWidth * 0.45f) {
                        return createStrokeFromPoints(sampleTriangle(topApex, botRight, botLeft), stroke.brush)
                    }
                }
            }
        }

        return null
    }
}

