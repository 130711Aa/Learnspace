package com.lernspace.app.ui.workspace.math

import android.graphics.RectF
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.common.RecognitionResult
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class EqualsTriggerMatch(
    val equationStrokes: List<Stroke>,
    val equalsStrokes: List<Stroke>,
    val answerStartX: Float,
    val answerTopY: Float,
    val charHeight: Float
)

data class MathRecognitionOutcome(
    val recognizedText: String,
    val solveResult: MathSolveResult,
    val usedDigitalInk: Boolean
)

/**
 * 100% Offline Handwritten Math Recognizer + Nebo-style '=' Trigger Detector.
 * Uses ML Kit Digital Ink (offline neural ink model after initial ~20 MB download)
 * with a built-in 0-MB geometric fallback so basic digits & operators work immediately.
 */
object HandwrittenMathRecognizer {

    private var recognizer: DigitalInkRecognizer? = null
    private var modelIdentifier: DigitalInkRecognitionModelIdentifier? = null
    private var inkModel: DigitalInkRecognitionModel? = null

    var isInkModelDownloaded by mutableStateOf(false)
        private set
    var isInkModelDownloading by mutableStateOf(false)
        private set

    private fun ensureInitialized() {
        if (recognizer != null) return
        try {
            val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
                ?: DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")
                ?: return
            modelIdentifier = id
            val model = DigitalInkRecognitionModel.builder(id).build()
            inkModel = model
            recognizer = DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(model)
                    .setMaxResultCount(20)
                    .build()
            )
        } catch (e: Exception) {
            Log.w("MathInk", "Init failed: ${e.message}")
        }
    }

    suspend fun checkOrDownloadInkModel() = withContext(Dispatchers.IO) {
        ensureInitialized()
        val model = inkModel ?: return@withContext
        val remoteManager = RemoteModelManager.getInstance()
        val alreadyDownloaded = awaitTask(remoteManager.isModelDownloaded(model)) ?: false
        withContext(Dispatchers.Main) {
            isInkModelDownloaded = alreadyDownloaded
        }
        if (!alreadyDownloaded && !isInkModelDownloading) {
            withContext(Dispatchers.Main) { isInkModelDownloading = true }
            val ok = awaitTask(
                remoteManager.download(model, DownloadConditions.Builder().build())
            )
            withContext(Dispatchers.Main) {
                isInkModelDownloading = false
                isInkModelDownloaded = (ok != null) || (awaitTask(remoteManager.isModelDownloaded(model)) == true)
            }
        }
    }

    /**
     * Detects if the most recently drawn strokes end with an '=' sign (2 parallel horizontal strokes),
     * and extracts the horizontal equation cluster to the left of '='.
     */
    fun detectEqualsTrigger(allStrokes: List<Stroke>): EqualsTriggerMatch? {
        if (allStrokes.size < 3) return null
        val s2 = allStrokes[allStrokes.size - 1]
        val s1 = allStrokes[allStrokes.size - 2]

        val b1 = strokeBounds(s1) ?: return null
        val b2 = strokeBounds(s2) ?: return null

        if (!isHorizontalBar(b1) || !isHorizontalBar(b2)) return null

        val w1 = b1.width()
        val w2 = b2.width()
        val avgW = (w1 + w2) * 0.5f
        val ratio = max(w1, w2) / max(1f, min(w1, w2))
        if (ratio > 2.4f) return null

        // Horizontal overlap check
        val overlap = min(b1.right, b2.right) - max(b1.left, b2.left)
        if (overlap < min(w1, w2) * 0.40f) return null

        // Vertical separation check (not overlapping same line, not miles apart)
        val topBar = if (b1.centerY() < b2.centerY()) b1 else b2
        val botBar = if (b1.centerY() < b2.centerY()) b2 else b1
        val centerGap = botBar.centerY() - topBar.centerY()
        if (centerGap < 6f || centerGap > max(70f, avgW * 1.45f)) return null

        val eqBounds = RectF(
            min(b1.left, b2.left),
            min(b1.top, b2.top),
            max(b1.right, b2.right),
            max(b1.bottom, b2.bottom)
        )

        // Collect all strokes on the same line to the left of '='
        val candidates = mutableListOf<Pair<Stroke, RectF>>()
        for (i in 0 until allStrokes.size - 2) {
            val st = allStrokes[i]
            val b = strokeBounds(st) ?: continue
            // Must be largely to the left of '=' right edge
            if (b.centerX() < eqBounds.left + avgW * 0.25f) {
                candidates.add(st to b)
            }
        }
        if (candidates.isEmpty()) return null

        // Estimate line height from strokes near '=' vertically
        val eqCenterY = eqBounds.centerY()
        val bandTolerance = max(95f, avgW * 2.6f)
        val sameBand = candidates.filter { (_, b) ->
            abs(b.centerY() - eqCenterY) <= bandTolerance
        }.sortedByDescending { it.second.right }

        if (sameBand.isEmpty()) return null

        val medianH = sameBand.map { it.second.height() }.sorted().let { it[it.size / 2] }.coerceIn(32f, 130f)
        val maxGap = max(140f, medianH * 3.4f)

        val picked = mutableListOf<Pair<Stroke, RectF>>()
        var frontierLeft = eqBounds.left
        for (pair in sameBand) {
            val (_, b) = pair
            // Connected if its right edge is within maxGap of the current leftmost equation frontier
            if (frontierLeft - b.right <= maxGap) {
                picked.add(pair)
                frontierLeft = min(frontierLeft, b.left)
            } else {
                break
            }
        }

        if (picked.isEmpty()) return null

        val equationStrokes = picked.map { it.first }.reversed()
        val sortedHeights = picked.map { it.second.height() }
            .filter { it > max(20f, medianH * 0.55f) }
            .sorted()
        val refCharHeight = if (sortedHeights.isNotEmpty()) {
            val p75 = sortedHeights[(sortedHeights.size * 3 / 4).coerceAtMost(sortedHeights.lastIndex)]
            (p75 * 1.12f).coerceIn(58f, 165f)
        } else {
            (medianH * 1.15f).coerceIn(58f, 165f)
        }

        val answerStartX = eqBounds.right + max(24f, refCharHeight * 0.42f)
        val answerTopY = eqCenterY - refCharHeight * 0.5f

        return EqualsTriggerMatch(
            equationStrokes = equationStrokes,
            equalsStrokes = listOf(s1, s2),
            answerStartX = answerStartX,
            answerTopY = answerTopY,
            charHeight = refCharHeight
        )
    }

    private fun isHorizontalBar(b: RectF): Boolean {
        val w = b.width()
        val h = b.height()
        return w in 14f..280f && h <= max(18f, w * 0.45f)
    }

    suspend fun recognizeAndSolve(strokes: List<Stroke>): MathRecognitionOutcome? {
        if (strokes.isEmpty()) return null

        // 1. Try ML Kit Digital Ink if downloaded
        ensureInitialized()
        val model = inkModel
        val rec = recognizer
        if (model != null && rec != null) {
            val downloaded = withContext(Dispatchers.IO) {
                awaitTask(RemoteModelManager.getInstance().isModelDownloaded(model)) == true
            }
            withContext(Dispatchers.Main) { isInkModelDownloaded = downloaded }
            if (downloaded) {
                val ink = buildMlKitInk(strokes)
                val ctx = RecognitionContext.builder()
                    .setPreContext("0123456789+-*/=()x.")
                    .build()
                val result = withContext(Dispatchers.IO) {
                    awaitTask(rec.recognize(ink, ctx))
                }
                if (result != null) {
                    val outcome = pickBestMathCandidate(result)
                    if (outcome != null) return outcome
                }
            } else {
                // Kick off background download quietly for future offline use
                checkOrDownloadInkModel()
            }
        }

        // 2. Fallback: 0-MB built-in geometric math recognizer
        val fallbackExpr = recognizeGeometricFallback(strokes) ?: return null
        val solved = MathExpressionEvaluator.solve(fallbackExpr) ?: return null
        return MathRecognitionOutcome(
            recognizedText = fallbackExpr,
            solveResult = solved,
            usedDigitalInk = false
        )
    }

    private fun pickBestMathCandidate(result: RecognitionResult): MathRecognitionOutcome? {
        // First pass: check exact candidates
        for (candidate in result.candidates) {
            val text = candidate.text.trim()
            if (MathExpressionEvaluator.isPlausibleMathExpression(text)) {
                val solved = MathExpressionEvaluator.solve(text)
                if (solved != null) {
                    return MathRecognitionOutcome(text, solved, usedDigitalInk = true)
                }
            }
        }
        // Second pass: coerce common handwriting digit/operator lookalikes
        for (candidate in result.candidates) {
            val coerced = coerceHandwritingLookalikes(candidate.text)
            if (MathExpressionEvaluator.isPlausibleMathExpression(coerced)) {
                val solved = MathExpressionEvaluator.solve(coerced)
                if (solved != null) {
                    return MathRecognitionOutcome(coerced, solved, usedDigitalInk = true)
                }
            }
        }
        return null
    }

    private fun coerceHandwritingLookalikes(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (i in raw.indices) {
            val ch = raw[i]
            val prevIsDigitOrOp = i > 0 && (raw[i - 1].isDigit() || raw[i - 1] in "+-*/(). ")
            val nextIsDigitOrOp = i < raw.lastIndex && (raw[i + 1].isDigit() || raw[i + 1] in "+-*/(). ")
            val mapped = when (ch) {
                'z', 'Z' -> '2'
                's', 'S' -> '5'
                'o', 'O' -> '0'
                'l', 'I' -> '1'
                'B' -> '8'
                'q', 'g' -> '9'
                'x', 'X' -> if (prevIsDigitOrOp && nextIsDigitOrOp) '*' else ch
                else -> ch
            }
            sb.append(mapped)
        }
        return sb.toString()
    }

    private fun buildMlKitInk(strokes: List<Stroke>): Ink {
        val builder = Ink.builder()
        val temp = StrokeInput()
        var timeOffsetMs = 0L

        for (stroke in strokes) {
            val inputs = stroke.inputs
            val count = inputs.size
            if (count == 0) continue

            val strokeBuilder = Ink.Stroke.builder()
            var lastT = 0L
            for (i in 0 until count) {
                inputs.populate(i, temp)
                val t = timeOffsetMs + temp.elapsedTimeMillis
                lastT = max(lastT, temp.elapsedTimeMillis)
                strokeBuilder.addPoint(Ink.Point.create(temp.x, temp.y, t))
            }
            builder.addStroke(strokeBuilder.build())
            timeOffsetMs += lastT + 20L
        }
        return builder.build()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 0-MB BUILT-IN GEOMETRIC / $1-STYLE FALLBACK RECOGNIZER (Basic Arithmetic)
    // ─────────────────────────────────────────────────────────────────────────
    private fun recognizeGeometricFallback(strokes: List<Stroke>): String? {
        val boxes = strokes.mapNotNull { st ->
            val b = strokeBounds(st) ?: return@mapNotNull null
            Triple(st, b, extractStrokePoints(st))
        }.sortedBy { it.second.left }

        if (boxes.isEmpty()) return null

        // Group intersecting or vertically overlapping strokes into symbol clusters (e.g. '+' or '×' or '4' or '=')
        val clusters = mutableListOf<MutableList<Triple<Stroke, RectF, List<Offset>>>>()
        for (item in boxes) {
            val target = clusters.firstOrNull { cluster ->
                val cb = clusterBounds(cluster)
                val xOverlap = min(cb.right, item.second.right) - max(cb.left, item.second.left)
                val minW = min(cb.width(), item.second.width()).coerceAtLeast(1f)
                xOverlap > minW * 0.35f
            }
            if (target != null) {
                target.add(item)
            } else {
                clusters.add(mutableListOf(item))
            }
        }

        val tokens = StringBuilder()
        for (cluster in clusters) {
            val ch = classifyCluster(cluster) ?: return null
            tokens.append(ch)
        }
        return tokens.toString()
    }

    private fun classifyCluster(cluster: List<Triple<Stroke, RectF, List<Offset>>>): Char? {
        val cb = clusterBounds(cluster)
        val w = cb.width().coerceAtLeast(1f)
        val h = cb.height().coerceAtLeast(1f)

        if (cluster.size == 2) {
            val b1 = cluster[0].second
            val b2 = cluster[1].second
            val horiz1 = isHorizontalBar(b1)
            val horiz2 = isHorizontalBar(b2)
            if (horiz1 && horiz2) return '='
            val vert1 = b1.height() > b1.width() * 1.4f
            val vert2 = b2.height() > b2.width() * 1.4f
            if ((horiz1 && vert2) || (horiz2 && vert1)) return '+'
            // Two diagonal strokes -> 'x' / '*'
            if (!horiz1 && !horiz2 && !vert1 && !vert2) return '*'
            return '4' // 2-stroke '4'
        }

        if (cluster.size >= 3) {
            // ÷ has 1 horizontal bar + 2 dots
            val horizCount = cluster.count { isHorizontalBar(it.second) }
            if (horizCount == 1 && cluster.size == 3) return '/'
            return null
        }

        // Single stroke
        val pts = cluster[0].third
        if (pts.size < 2) return '.'
        if (w < 14f && h < 14f) return '.'
        if (isHorizontalBar(cb)) return '-'
        if (h > w * 2.5f) {
            // Straight vertical or diagonal '/'
            val start = pts.first()
            val end = pts.last()
            return if (abs(end.x - start.x) > h * 0.38f) '/' else '1'
        }

        // Template matching against normalized digit templates
        return matchDigitTemplate(pts)
    }

    private val digitTemplates: List<Pair<Char, List<Offset>>> by lazy {
        listOf(
            '0' to resampleNormalized(listOf(0.5f to 0.0f, 0.1f to 0.25f, 0.0f to 0.5f, 0.1f to 0.8f, 0.5f to 1.0f, 0.9f to 0.8f, 1.0f to 0.5f, 0.9f to 0.2f, 0.5f to 0.0f)),
            '1' to resampleNormalized(listOf(0.2f to 0.2f, 0.5f to 0.0f, 0.5f to 1.0f)),
            '2' to resampleNormalized(listOf(0.1f to 0.2f, 0.35f to 0.0f, 0.8f to 0.05f, 0.9f to 0.3f, 0.5f to 0.65f, 0.0f to 1.0f, 1.0f to 1.0f)),
            '3' to resampleNormalized(listOf(0.1f to 0.1f, 0.5f to 0.0f, 0.9f to 0.2f, 0.5f to 0.48f, 0.9f to 0.72f, 0.5f to 1.0f, 0.1f to 0.9f)),
            '4' to resampleNormalized(listOf(0.7f to 0.0f, 0.0f to 0.65f, 1.0f to 0.65f, 0.7f to 0.65f, 0.7f to 1.0f)),
            '5' to resampleNormalized(listOf(0.9f to 0.0f, 0.2f to 0.0f, 0.15f to 0.45f, 0.65f to 0.45f, 0.95f to 0.7f, 0.6f to 1.0f, 0.1f to 0.9f)),
            '6' to resampleNormalized(listOf(0.75f to 0.05f, 0.3f to 0.0f, 0.05f to 0.4f, 0.05f to 0.85f, 0.5f to 1.0f, 0.95f to 0.8f, 0.7f to 0.5f, 0.1f to 0.6f)),
            '7' to resampleNormalized(listOf(0.0f to 0.0f, 1.0f to 0.0f, 0.35f to 1.0f)),
            '8' to resampleNormalized(listOf(0.5f to 0.48f, 0.15f to 0.25f, 0.5f to 0.0f, 0.85f to 0.25f, 0.5f to 0.48f, 0.1f to 0.75f, 0.5f to 1.0f, 0.9f to 0.75f, 0.5f to 0.48f)),
            '9' to resampleNormalized(listOf(0.85f to 0.3f, 0.5f to 0.5f, 0.1f to 0.3f, 0.45f to 0.0f, 0.9f to 0.2f, 0.85f to 0.7f, 0.45f to 1.0f)),
            '(' to resampleNormalized(listOf(0.8f to 0.0f, 0.2f to 0.3f, 0.15f to 0.5f, 0.2f to 0.7f, 0.8f to 1.0f)),
            ')' to resampleNormalized(listOf(0.2f to 0.0f, 0.8f to 0.3f, 0.85f to 0.5f, 0.8f to 0.7f, 0.2f to 1.0f))
        )
    }

    private fun matchDigitTemplate(pts: List<Offset>): Char? {
        val norm = resampleNormalized(pts.map { it.x to it.y })
        var bestChar: Char? = null
        var bestDist = Float.MAX_VALUE
        for ((ch, tmpl) in digitTemplates) {
            var d = 0f
            for (i in norm.indices) {
                d += hypot(norm[i].x - tmpl[i].x, norm[i].y - tmpl[i].y)
            }
            if (d < bestDist) {
                bestDist = d
                bestChar = ch
            }
        }
        return if (bestDist < 12f) bestChar else null
    }

    private fun resampleNormalized(raw: List<Pair<Float, Float>>, n: Int = 24): List<Offset> {
        val pts = raw.map { Offset(it.first, it.second) }
        if (pts.size < 2) return List(n) { Offset.Zero }

        var totalLen = 0f
        for (i in 1 until pts.size) {
            totalLen += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
        }
        if (totalLen < 1e-4f) return List(n) { Offset.Zero }

        val step = totalLen / (n - 1)
        val resampled = mutableListOf(pts.first())
        var accum = 0f
        var i = 1
        val work = pts.toMutableList()
        while (i < work.size && resampled.size < n) {
            val d = hypot(work[i].x - work[i - 1].x, work[i].y - work[i - 1].y)
            if (accum + d >= step) {
                val t = (step - accum) / d
                val nx = work[i - 1].x + t * (work[i].x - work[i - 1].x)
                val ny = work[i - 1].y + t * (work[i].y - work[i - 1].y)
                val np = Offset(nx, ny)
                resampled.add(np)
                work.add(i, np)
                accum = 0f
            } else {
                accum += d
            }
            i++
        }
        while (resampled.size < n) resampled.add(pts.last())

        val minX = resampled.minOf { it.x }
        val maxX = resampled.maxOf { it.x }
        val minY = resampled.minOf { it.y }
        val maxY = resampled.maxOf { it.y }
        val scale = max(maxX - minX, maxY - minY).coerceAtLeast(1e-3f)
        return resampled.map { Offset((it.x - minX) / scale, (it.y - minY) / scale) }
    }

    private fun clusterBounds(cluster: List<Triple<Stroke, RectF, List<Offset>>>): RectF {
        var l = Float.MAX_VALUE
        var t = Float.MAX_VALUE
        var r = -Float.MAX_VALUE
        var b = -Float.MAX_VALUE
        for ((_, rect, _) in cluster) {
            if (rect.left < l) l = rect.left
            if (rect.top < t) t = rect.top
            if (rect.right > r) r = rect.right
            if (rect.bottom > b) b = rect.bottom
        }
        return RectF(l, t, r, b)
    }

    private fun strokeBounds(stroke: Stroke): RectF? {
        val inputs = stroke.inputs
        val count = inputs.size
        if (count == 0) return null
        val temp = StrokeInput()
        inputs.populate(0, temp)
        var minX = temp.x
        var maxX = temp.x
        var minY = temp.y
        var maxY = temp.y
        for (i in 1 until count) {
            inputs.populate(i, temp)
            if (temp.x < minX) minX = temp.x
            if (temp.x > maxX) maxX = temp.x
            if (temp.y < minY) minY = temp.y
            if (temp.y > maxY) maxY = temp.y
        }
        return RectF(minX, minY, maxX, maxY)
    }

    private fun extractStrokePoints(stroke: Stroke): List<Offset> {
        val inputs = stroke.inputs
        val temp = StrokeInput()
        return List(inputs.size) { i ->
            inputs.populate(i, temp)
            Offset(temp.x, temp.y)
        }
    }

    private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T? {
        return suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }.addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
        }
    }
}
