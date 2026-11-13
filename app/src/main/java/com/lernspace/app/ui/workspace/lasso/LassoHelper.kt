package com.lernspace.app.ui.workspace.lasso

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.lernspace.app.ui.workspace.model.ImageElement
import com.lernspace.app.ui.workspace.model.TextBoxElement

data class LassoSelectionState(
    val selectedStrokes: MutableList<Stroke> = mutableListOf(),
    val selectedTextBoxes: MutableList<TextBoxElement> = mutableListOf(),
    val selectedImages: MutableList<ImageElement> = mutableListOf(),
    val bounds: Rect = Rect.Zero,
    val isRegionCapture: Boolean = false,
    val lassoPolygon: List<Offset> = emptyList()
) {
    val isEmpty: Boolean
        get() = !isRegionCapture && selectedStrokes.isEmpty() && selectedTextBoxes.isEmpty() && selectedImages.isEmpty()
}

object LassoHelper {

    /**
     * Determines whether a 2D point lies within an arbitrary polygon using ray-casting.
     */
    fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.y > point.y) != (pj.y > point.y) &&
                point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Tests if an AndroidX Ink Stroke is enclosed by or overlaps the lasso polygon.
     */
    fun isStrokeSelected(stroke: Stroke, polygon: List<Offset>): Boolean {
        val inputs = stroke.inputs
        val count = inputs.size
        if (count == 0) return false

        val temp = StrokeInput()

        // 1. Check midpoint / centroid
        var sumX = 0f
        var sumY = 0f
        val sampleStep = maxOf(1, count / 20)
        var sampledCount = 0
        var insideCount = 0

        for (i in 0 until count step sampleStep) {
            inputs.populate(i, temp)
            sumX += temp.x
            sumY += temp.y
            sampledCount++
            if (isPointInPolygon(Offset(temp.x, temp.y), polygon)) {
                insideCount++
            }
        }

        val centroid = Offset(sumX / sampledCount, sumY / sampledCount)
        if (isPointInPolygon(centroid, polygon)) return true

        // If at least 25% of points are inside the lasso loop, count as selected
        return insideCount.toFloat() / sampledCount >= 0.25f
    }

    /**
     * Tests if a TextBoxElement is inside the lasso polygon.
     */
    fun isTextBoxSelected(box: TextBoxElement, polygon: List<Offset>): Boolean {
        val center = Offset(box.x + box.width / 2f, box.y + box.height / 2f)
        if (isPointInPolygon(center, polygon)) return true
        val topLeft = Offset(box.x, box.y)
        val topRight = Offset(box.x + box.width, box.y)
        val bottomLeft = Offset(box.x, box.y + box.height)
        val bottomRight = Offset(box.x + box.width, box.y + box.height)
        return isPointInPolygon(topLeft, polygon) ||
                isPointInPolygon(topRight, polygon) ||
                isPointInPolygon(bottomLeft, polygon) ||
                isPointInPolygon(bottomRight, polygon)
    }

    /**
     * Tests if an ImageElement is inside or overlapped by the lasso polygon.
     */
    fun isImageSelected(img: ImageElement, polygon: List<Offset>): Boolean {
        if (polygon.isEmpty()) return false
        val center = Offset(img.x + img.width / 2f, img.y + img.height / 2f)
        if (isPointInPolygon(center, polygon)) return true
        val topLeft = Offset(img.x, img.y)
        val topRight = Offset(img.x + img.width, img.y)
        val bottomLeft = Offset(img.x, img.y + img.height)
        val bottomRight = Offset(img.x + img.width, img.y + img.height)
        if (isPointInPolygon(topLeft, polygon) ||
            isPointInPolygon(topRight, polygon) ||
            isPointInPolygon(bottomLeft, polygon) ||
            isPointInPolygon(bottomRight, polygon)
        ) return true

        // Check if any point of the lasso polygon lies inside the image bounding box
        val imgRect = Rect(img.x, img.y, img.x + img.width, img.y + img.height)
        return polygon.any { imgRect.contains(it) }
    }

    /**
     * Checks if a stroke overlaps with a bounding box.
     */
    fun isStrokeInBounds(stroke: Stroke, bounds: Rect): Boolean {
        val inputs = stroke.inputs
        val count = inputs.size
        if (count == 0) return false
        val temp = StrokeInput()
        val step = maxOf(1, count / 12)
        for (i in 0 until count step step) {
            inputs.populate(i, temp)
            if (bounds.contains(Offset(temp.x, temp.y))) return true
        }
        return false
    }

    /**
     * Calculates the bounding box of a lasso polygon loop.
     */
    fun computePolygonBounds(polygon: List<Offset>, padding: Float = 10f): Rect? {
        if (polygon.size < 3) return null
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (pt in polygon) {
            if (pt.x < minX) minX = pt.x
            if (pt.x > maxX) maxX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.y > maxY) maxY = pt.y
        }

        if (minX > maxX || minY > maxY) return null
        return Rect(minX - padding, minY - padding, maxX + padding, maxY + padding)
    }

    /**
     * Calculates the tight axis-aligned bounding box enclosing all selected elements with extra padding.
     */
    fun computeCombinedBounds(
        strokes: List<Stroke>,
        textBoxes: List<TextBoxElement>,
        images: List<ImageElement>,
        padding: Float = 14f
    ): Rect? {
        if (strokes.isEmpty() && textBoxes.isEmpty() && images.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        val temp = StrokeInput()
        for (stroke in strokes) {
            val inputs = stroke.inputs
            for (i in 0 until inputs.size) {
                inputs.populate(i, temp)
                if (temp.x < minX) minX = temp.x
                if (temp.x > maxX) maxX = temp.x
                if (temp.y < minY) minY = temp.y
                if (temp.y > maxY) maxY = temp.y
            }
        }

        for (box in textBoxes) {
            if (box.x < minX) minX = box.x
            if (box.x + box.width > maxX) maxX = box.x + box.width
            if (box.y < minY) minY = box.y
            if (box.y + box.height > maxY) maxY = box.y + box.height
        }

        for (img in images) {
            if (img.x < minX) minX = img.x
            if (img.x + img.width > maxX) maxX = img.x + img.width
            if (img.y < minY) minY = img.y
            if (img.y + img.height > maxY) maxY = img.y + img.height
        }

        if (minX > maxX || minY > maxY) return null
        return Rect(minX - padding, minY - padding, maxX + padding, maxY + padding)
    }
}

