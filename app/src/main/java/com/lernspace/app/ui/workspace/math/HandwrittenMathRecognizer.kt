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
    val usedDigitalInk: Boolean,
    val activeVariables: Map<String, Double> = emptyMap()
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
        if (ratio > 2.8f) return null

        // Horizontal overlap check
        val overlap = min(b1.right, b2.right) - max(b1.left, b2.left)
        if (overlap < min(w1, w2) * 0.35f) return null

        // Vertical separation check (two distinct parallel bars, not identical stroke, not miles apart)
        val centerGap = abs(b1.centerY() - b2.centerY())
        if (centerGap < 4f || centerGap > max(75f, avgW * 1.55f)) return null

        val horizDiff = abs(b1.centerX() - b2.centerX())
        if (horizDiff > max(28f, avgW * 0.75f)) return null

        val eqBounds = RectF(
            min(b1.left, b2.left),
            min(b1.top, b2.top),
            max(b1.right, b2.right),
            max(b1.bottom, b2.bottom)
        )
        val eqCenterY = eqBounds.centerY()

        // Collect all candidate strokes drawn prior to '=' largely to the left of '='
        val candidates = mutableListOf<Pair<Stroke, RectF>>()
        for (i in 0 until allStrokes.size - 2) {
            val st = allStrokes[i]
            val b = strokeBounds(st) ?: continue
            if (b.centerX() < eqBounds.left + avgW * 0.25f) {
                candidates.add(st to b)
            }
        }
        if (candidates.isEmpty()) return null

        // Estimate line height from strokes physically near '=' on the same line
        val nearby = candidates.filter { (_, b) ->
            b.right >= eqBounds.left - 220f && abs(b.centerY() - eqCenterY) <= 75f
        }
        val pool = if (nearby.isNotEmpty()) nearby else candidates
        val medianH = pool.map { it.second.height() }.sorted().let { it[it.size / 2] }.coerceIn(24f, 130f)

        // Strict vertical band to prevent pulling in strokes from notes/lines above or below
        val bandTolerance = max(45f, medianH * 1.15f)
        val sameBand = candidates.filter { (_, b) ->
            abs(b.centerY() - eqCenterY) <= bandTolerance &&
                b.bottom >= eqBounds.top - medianH * 0.5f &&
                b.top <= eqBounds.bottom + medianH * 0.5f
        }.sortedByDescending { it.second.right }

        if (sameBand.isEmpty()) return null

        val maxGap = max(65f, medianH * 2.0f)
        val pickedStrokesSet = LinkedHashSet<Stroke>()
        var frontierLeft = eqBounds.left

        for (i in sameBand.indices) {
            val (st, b) = sameBand[i]
            // Stop if there is a wide horizontal gap between terms
            if (frontierLeft - b.right > maxGap) break

            // Stop if we encounter a preceding '=' sign (e.g. from an earlier equation on the line)
            if (i + 1 < sameBand.size && isHorizontalBar(b)) {
                val nextB = sameBand[i + 1].second
                if (isHorizontalBar(nextB) && abs(b.centerX() - nextB.centerX()) <= max(24f, b.width() * 0.6f) &&
                    abs(b.centerY() - nextB.centerY()) in 4f..50f
                ) {
                    // Preceding '=' encountered: stop walk leftward
                    break
                }
            }

            pickedStrokesSet.add(st)
            frontierLeft = min(frontierLeft, b.left)
        }

        if (pickedStrokesSet.isEmpty()) return null

        // Maintain original chronological stroke order for ML Kit accuracy
        val equationStrokes = allStrokes.filter { it in pickedStrokesSet }
        val refCharHeight = medianH.coerceIn(28f, 110f)

        val answerStartX = eqBounds.right + max(16f, refCharHeight * 0.38f)
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
        return w in 10f..350f && h <= max(30f, w * 0.65f) && w >= h * 0.85f
    }

    suspend fun recognizeAndSolve(
        strokes: List<Stroke>,
        externalVariables: Map<String, Double> = emptyMap()
    ): MathRecognitionOutcome? {
        if (strokes.isEmpty()) return null

        val lines = groupStrokesIntoLines(strokes)
        if (lines.size >= 2) {
            val mergedVars = externalVariables.toMutableMap()
            // Resolve earlier lines as variable declarations when user lassos multiple lines (e.g. x=5 and 2x+1)
            for (i in 0 until lines.lastIndex) {
                val subChunks = splitLineByWideHorizontalGaps(lines[i])
                for (chunk in subChunks) {
                    val candidates = recognizeMathLayoutCandidates(chunk, mergedVars)
                    extractAssignmentsFromCandidates(candidates, mergedVars)
                }
            }
            val lastOutcome = recognizeSingleLineAndSolve(lines.last(), mergedVars)
            if (lastOutcome != null) return lastOutcome
        }

        return recognizeSingleLineAndSolve(strokes, externalVariables)
    }

    /**
     * Scans prior/other lines on the same page for variable declarations such as "x = 5", "a = 1/2", "y = 2x + 1".
     * Uses fast geometric filtering so non-math handwriting paragraphs don't trigger ML Kit calls.
     */
    suspend fun extractVariableDeclarations(
        allStrokes: List<Stroke>,
        excludeStrokes: Set<Stroke> = emptySet(),
        beforeBottomY: Float? = null
    ): Map<String, Double> {
        val candidateStrokes = allStrokes.filter { it !in excludeStrokes }
        if (candidateStrokes.size < 2) return emptyMap()

        val lines = groupStrokesIntoLines(candidateStrokes)
        val vars = LinkedHashMap<String, Double>()
        for (line in lines) {
            if (beforeBottomY != null) {
                val lb = strokesBounds(line) ?: continue
                if (lb.centerY() > beforeBottomY + 28f) continue
            }
            val chunks = splitLineByWideHorizontalGaps(line)
            for (chunk in chunks) {
                if (!lineLooksLikeAssignment(chunk)) continue
                val candidates = recognizeMathLayoutCandidates(chunk, vars)
                extractAssignmentsFromCandidates(candidates, vars)
            }
        }
        Log.d("MathInk", "Extracted page variables: $vars")
        return vars
    }

    private fun extractAssignmentsFromCandidates(
        candidates: List<String>,
        targetVars: MutableMap<String, Double>
    ) {
        // Pass 1: exact candidate text (also supports comma/semicolon-separated "x=5, y=3")
        for (cand in candidates) {
            val multi = MathExpressionEvaluator.parseVariableAssignments(cand, targetVars)
            if (multi.isNotEmpty()) {
                targetVars.putAll(multi)
                return
            }
            val single = MathExpressionEvaluator.parseVariableAssignment(cand, targetVars)
            if (single != null) {
                targetVars[single.first] = single.second
                return
            }
        }
        // Pass 2: coerce RHS digit lookalikes (e.g. "x = S" -> "x = 5") without altering LHS variable name
        for (cand in candidates) {
            val eqIdx = cand.indexOf('=')
            if (eqIdx <= 0) continue
            val lhs = cand.substring(0, eqIdx).trim()
            val rhs = coerceHandwritingLookalikes(cand.substring(eqIdx + 1), targetVars.keys)
            val fixed = "$lhs=$rhs"
            val single = MathExpressionEvaluator.parseVariableAssignment(fixed, targetVars)
            if (single != null) {
                targetVars[single.first] = single.second
                return
            }
        }
    }

    private suspend fun recognizeSingleLineAndSolve(
        strokes: List<Stroke>,
        variables: Map<String, Double>
    ): MathRecognitionOutcome? {
        val modelReady = ensureDigitalInkReady()
        if (modelReady) {
            val candidates = recognizeMathLayoutCandidates(strokes, variables)
            Log.d("MathInk", "Candidates ($variables): $candidates")
            for (text in candidates) {
                if (MathExpressionEvaluator.isPlausibleMathExpression(text, variables)) {
                    val solved = MathExpressionEvaluator.solve(text, variables)
                    if (solved != null) {
                        return MathRecognitionOutcome(text, solved, usedDigitalInk = true, activeVariables = variables)
                    }
                }
            }
            for (text in candidates) {
                val coerced = coerceHandwritingLookalikes(text, variables.keys)
                if (MathExpressionEvaluator.isPlausibleMathExpression(coerced, variables)) {
                    val solved = MathExpressionEvaluator.solve(coerced, variables)
                    if (solved != null) {
                        return MathRecognitionOutcome(coerced, solved, usedDigitalInk = true, activeVariables = variables)
                    }
                }
            }
        }

        // Fallback: 0-MB built-in geometric math recognizer (including spatial fractions/powers)
        val fallbackExpr = recognizeLayoutGeometricFallback(strokes) ?: return null
        val solved = MathExpressionEvaluator.solve(fallbackExpr, variables) ?: return null
        return MathRecognitionOutcome(
            recognizedText = fallbackExpr,
            solveResult = solved,
            usedDigitalInk = false,
            activeVariables = variables
        )
    }

    private suspend fun ensureDigitalInkReady(): Boolean {
        ensureInitialized()
        val model = inkModel ?: return false
        if (recognizer == null) return false
        val downloaded = withContext(Dispatchers.IO) {
            awaitTask(RemoteModelManager.getInstance().isModelDownloaded(model)) == true
        }
        withContext(Dispatchers.Main) { isInkModelDownloaded = downloaded }
        if (!downloaded) {
            checkOrDownloadInkModel()
        }
        return downloaded
    }

    /**
     * Combines direct ML Kit Digital Ink recognition with 2D spatial parsing
     * (stacked fractions & superscript powers) so written fractions like a/b and
     * powers like 2^3 or x^2 recognize reliably without mangling unbroken handwriting.
     */
    private suspend fun recognizeMathLayoutCandidates(
        strokes: List<Stroke>,
        variables: Map<String, Double>,
        depth: Int = 0
    ): List<String> {
        if (strokes.isEmpty()) return emptyList()
        val results = LinkedHashSet<String>()

        // 1. Primary: Direct unbroken ML Kit candidates!
        val rawCandidates = recognizeRawInkCandidates(strokes, variables)
        results.addAll(rawCandidates)

        // 2. If depth < 2, generate spatial parsing candidates (stacked fractions and superscripts)
        // This augments the candidate list without overriding or corrupting raw unbroken handwriting.
        if (depth < 2) {
            val fracSegment = detectStackedFraction(strokes)
            if (fracSegment != null) {
                val leftExpr = if (fracSegment.leftStrokes.isNotEmpty()) {
                    recognizeBestSegmentText(fracSegment.leftStrokes, variables, depth + 1)
                } else ""
                val numExpr = recognizeBestSegmentText(fracSegment.numeratorStrokes, variables, depth + 1)
                val denExpr = recognizeBestSegmentText(fracSegment.denominatorStrokes, variables, depth + 1)
                val rightExpr = if (fracSegment.rightStrokes.isNotEmpty()) {
                    recognizeBestSegmentText(fracSegment.rightStrokes, variables, depth + 1)
                } else ""

                if (!numExpr.isNullOrBlank() && !denExpr.isNullOrBlank()) {
                    val composed = buildString {
                        if (!leftExpr.isNullOrBlank()) append(leftExpr)
                        append("((").append(numExpr).append(")/(").append(denExpr).append("))")
                        if (!rightExpr.isNullOrBlank()) append(rightExpr)
                    }
                    results.add(composed)
                }
            }

            val supSplit = detectSuperscriptSplit(strokes)
            if (supSplit != null) {
                val baseExpr = recognizeBestSegmentText(supSplit.baseStrokes, variables, depth + 1)
                val expExpr = recognizeBestSegmentText(supSplit.exponentStrokes, variables, depth + 1)
                val restExpr = if (supSplit.trailingStrokes.isNotEmpty()) {
                    recognizeBestSegmentText(supSplit.trailingStrokes, variables, depth + 1)
                } else ""
                if (!baseExpr.isNullOrBlank() && !expExpr.isNullOrBlank()) {
                    val composed = buildString {
                        append(baseExpr).append("^(").append(expExpr).append(')')
                        if (!restExpr.isNullOrBlank()) append(restExpr)
                    }
                    results.add(composed)
                }
            }
        }

        return results.toList()
    }

    private suspend fun recognizeBestSegmentText(
        strokes: List<Stroke>,
        variables: Map<String, Double>,
        depth: Int
    ): String? {
        if (strokes.isEmpty()) return null
        val candidates = recognizeMathLayoutCandidates(strokes, variables, depth)
        for (c in candidates) {
            val norm = MathExpressionEvaluator.normalizeMathString(c, variables.keys)
            if (norm.isNotEmpty() && MathExpressionEvaluator.tryEvaluate(norm.trimEnd('='), variables) != null) {
                return norm
            }
        }
        val firstPlausible = candidates.firstOrNull {
            MathExpressionEvaluator.isPlausibleMathExpression(it, variables) ||
                it.trim().let { t -> t.length == 1 && (t[0].isDigit() || t[0].isLetter()) }
        }
        if (firstPlausible != null) return firstPlausible.trim()
        val coerced = candidates.firstOrNull()?.let { coerceHandwritingLookalikes(it, variables.keys) }
        if (!coerced.isNullOrBlank()) return coerced.trim()
        return recognizeLayoutGeometricFallback(strokes)
    }

    private suspend fun recognizeRawInkCandidates(
        strokes: List<Stroke>,
        @Suppress("UNUSED_PARAMETER") variables: Map<String, Double>
    ): List<String> {
        val rec = recognizer ?: return emptyList()
        val ink = buildMlKitInk(strokes)
        val ctx = RecognitionContext.builder()
            .setPreContext("")
            .build()
        val result = withContext(Dispatchers.IO) {
            awaitTask(rec.recognize(ink, ctx))
        } ?: return emptyList()
        return result.candidates.mapNotNull { it.text.trim().takeIf { t -> t.isNotEmpty() } }
    }

    private data class FractionSplit(
        val leftStrokes: List<Stroke>,
        val numeratorStrokes: List<Stroke>,
        val barStroke: Stroke,
        val denominatorStrokes: List<Stroke>,
        val rightStrokes: List<Stroke>
    )

    private data class SuperscriptSplit(
        val baseStrokes: List<Stroke>,
        val exponentStrokes: List<Stroke>,
        val trailingStrokes: List<Stroke>
    )

    private fun detectStackedFraction(strokes: List<Stroke>): FractionSplit? {
        if (strokes.size < 3) return null
        val items = strokes.mapNotNull { st -> strokeBounds(st)?.let { st to it } }
        if (items.size < 3) return null

        // A fraction bar MUST be a clean, wide horizontal stroke
        val bars = items.filter { (_, b) ->
            val w = b.width()
            val h = b.height()
            w >= 24f && h <= max(18f, w * 0.28f) && w >= h * 3.0f
        }.sortedByDescending { it.second.width() }

        for ((barStroke, barBox) in bars) {
            // Check that this bar is NOT part of an '=' sign (another parallel bar of similar width and position)
            val hasParallelBar = items.any { (otherSt, ob) ->
                otherSt !== barStroke &&
                abs(ob.width() - barBox.width()) <= max(18f, barBox.width() * 0.45f) &&
                abs(ob.centerX() - barBox.centerX()) <= max(18f, barBox.width() * 0.35f) &&
                abs(ob.centerY() - barBox.centerY()) in 4f..max(60f, barBox.width() * 1.2f)
            }
            if (hasParallelBar) continue

            // Check that no vertical stroke cuts through the bar (e.g. '+' sign or '4')
            val isIntersected = items.any { (otherSt, ob) ->
                otherSt !== barStroke &&
                ob.height() > barBox.height() * 2.5f &&
                ob.left >= barBox.left - 4f && ob.right <= barBox.right + 4f &&
                ob.top < barBox.centerY() && ob.bottom > barBox.centerY()
            }
            if (isIntersected) continue

            val padX = max(8f, barBox.width() * 0.10f)
            val spanLeft = barBox.left - padX
            val spanRight = barBox.right + padX

            val above = mutableListOf<Stroke>()
            val below = mutableListOf<Stroke>()
            val left = mutableListOf<Stroke>()
            val right = mutableListOf<Stroke>()

            for ((st, b) in items) {
                if (st === barStroke) continue
                val cx = b.centerX()
                val cy = b.centerY()
                val overlapsBarX = cx in spanLeft..spanRight ||
                    (min(b.right, barBox.right) - max(b.left, barBox.left)) > b.width() * 0.40f

                if (overlapsBarX && b.bottom <= barBox.centerY() + 4f) {
                    above.add(st)
                } else if (overlapsBarX && b.top >= barBox.centerY() - 4f) {
                    below.add(st)
                } else if (b.right <= barBox.left) {
                    left.add(st)
                } else if (b.left >= barBox.right) {
                    right.add(st)
                }
            }

            if (above.isEmpty() || below.isEmpty()) continue

            // Neither above nor below can be an '=' sign
            val hasEqualAbove = above.size >= 2 && itemsLookLikeEquals(above)
            val hasEqualBelow = below.size >= 2 && itemsLookLikeEquals(below)
            if (hasEqualAbove || hasEqualBelow) continue

            return FractionSplit(
                leftStrokes = left,
                numeratorStrokes = above,
                barStroke = barStroke,
                denominatorStrokes = below,
                rightStrokes = right
            )
        }
        return null
    }

    private fun itemsLookLikeEquals(strokes: List<Stroke>): Boolean {
        val items = strokes.mapNotNull { st -> strokeBounds(st)?.let { st to it } }
        val bars = items.filter { isHorizontalBar(it.second) }
        if (bars.size < 2) return false
        for (i in bars.indices) {
            for (j in i + 1 until bars.size) {
                val b1 = bars[i].second
                val b2 = bars[j].second
                val wAvg = (b1.width() + b2.width()) * 0.5f
                val vGap = abs(b1.centerY() - b2.centerY())
                val xDiff = abs(b1.centerX() - b2.centerX())
                if (vGap in 4f..max(60f, wAvg * 1.3f) && xDiff <= max(24f, wAvg * 0.6f)) {
                    return true
                }
            }
        }
        return false
    }

    private fun detectSuperscriptSplit(strokes: List<Stroke>): SuperscriptSplit? {
        if (strokes.size < 2) return null
        val clusters = buildHorizontalClusters(strokes)
        if (clusters.size < 2) return null

        for (i in 0 until clusters.lastIndex) {
            val base = clusters[i]
            val baseH = base.bounds.height()
            val baseW = base.bounds.width()
            // Base must look like a real digit/variable/paren, not a flat minus/equals/dot
            if (baseH < 20f || (isClusterFlatBar(base) && baseW > baseH * 1.8f)) continue

            val next = clusters[i + 1]
            val nextH = next.bounds.height()
            val nextW = next.bounds.width()
            if (next.bounds.left < base.bounds.centerX() - 4f) continue
            if (nextH < 6f && nextW < 6f) continue // ignore tiny ink dots
            if (nextH > baseH * 0.78f) continue
            // Never mistake '-', '=', or '+' operators near the middle for a raised exponent
            if (isClusterFlatBar(next) || isClusterPlusOrEquals(next)) continue

            val clearlyRaised = next.bounds.centerY() <= base.bounds.top + baseH * 0.38f &&
                next.bounds.bottom <= base.bounds.top + baseH * 0.56f
            if (!clearlyRaised) continue

            // Consume contiguous superscript clusters (e.g. ^12 or ^(-2))
            var expEndIdx = i + 1
            val expTopLimit = base.bounds.top + baseH * 0.46f
            while (expEndIdx + 1 < clusters.size) {
                val cand = clusters[expEndIdx + 1]
                if (cand.bounds.centerY() <= expTopLimit && cand.bounds.bottom <= base.bounds.top + baseH * 0.60f) {
                    expEndIdx++
                } else {
                    break
                }
            }

            val baseStrokes = clusters.subList(0, i + 1).flatMap { it.strokes }
            val expStrokes = clusters.subList(i + 1, expEndIdx + 1).flatMap { it.strokes }
            val trailingStrokes = if (expEndIdx + 1 < clusters.size) {
                clusters.subList(expEndIdx + 1, clusters.size).flatMap { it.strokes }
            } else emptyList()

            if (baseStrokes.isNotEmpty() && expStrokes.isNotEmpty()) {
                return SuperscriptSplit(baseStrokes, expStrokes, trailingStrokes)
            }
        }
        return null
    }

    private data class StrokeCluster(val strokes: List<Stroke>, val bounds: RectF)

    private fun buildHorizontalClusters(strokes: List<Stroke>): List<StrokeCluster> {
        val items = strokes.mapNotNull { st -> strokeBounds(st)?.let { st to it } }
            .sortedBy { it.second.left }
        val raw = mutableListOf<Pair<MutableList<Stroke>, RectF>>()
        for ((st, box) in items) {
            val matchIdx = raw.indexOfLast { (_, rb) ->
                val horizOverlap = min(rb.right, box.right) - max(rb.left, box.left)
                val minW = max(8f, min(rb.width(), box.width()))
                // Merge intersecting/stacked parts of the same symbol (e.g. '+' bars, '4', 't', 'x')
                // only if their vertical extents also overlap or sit close directly above/below
                val vertOverlap = min(rb.bottom, box.bottom) - max(rb.top, box.top)
                horizOverlap > minW * 0.32f && vertOverlap > -18f
            }
            if (matchIdx >= 0) {
                raw[matchIdx].first.add(st)
                raw[matchIdx].second.union(box)
            } else {
                raw.add(mutableListOf(st) to RectF(box))
            }
        }
        return raw.sortedBy { it.second.left }.map { StrokeCluster(it.first, it.second) }
    }

    private fun isClusterFlatBar(cluster: StrokeCluster): Boolean =
        cluster.strokes.size == 1 && isHorizontalBar(cluster.bounds)

    private fun isClusterPlusOrEquals(cluster: StrokeCluster): Boolean {
        if (cluster.strokes.size != 2) return false
        val b0 = strokeBounds(cluster.strokes[0]) ?: return false
        val b1 = strokeBounds(cluster.strokes[1]) ?: return false
        val h0 = isHorizontalBar(b0)
        val h1 = isHorizontalBar(b1)
        if (h0 && h1) return true
        val v0 = b0.height() > b0.width() * 1.4f
        val v1 = b1.height() > b1.width() * 1.4f
        return (h0 && v1) || (h1 && v0)
    }

    private fun groupStrokesIntoLines(strokes: List<Stroke>): List<List<Stroke>> {
        val items = strokes.mapNotNull { st -> strokeBounds(st)?.let { st to it } }
            .sortedBy { it.second.centerY() }
        if (items.isEmpty()) return emptyList()

        val tallHeights = items.map { it.second.height() }.filter { it > 20f }.sorted()
        val medianH = if (tallHeights.isNotEmpty()) tallHeights[tallHeights.size / 2].coerceIn(24f, 160f) else 48f

        val groups = mutableListOf<Pair<MutableList<Stroke>, RectF>>()
        for ((st, box) in items) {
            val target = groups.lastOrNull()
            if (target != null) {
                val gb = target.second
                val verticalGap = max(box.top - gb.bottom, gb.top - box.bottom)
                val horizOverlap = min(gb.right, box.right) - max(gb.left, box.left)
                val hasStackedBar = isHorizontalBar(box) ||
                    target.first.any { prev -> strokeBounds(prev)?.let { isHorizontalBar(it) && it.width() >= 26f } == true }
                val sameLine = when {
                    // Vertically overlapping or tiny gap <= 10px -> same row
                    verticalGap <= 10f -> true
                    // Keep stacked fraction numerators/bars/denominators grouped cleanly
                    hasStackedBar && horizOverlap > 0f && verticalGap <= min(26f, medianH * 0.34f) -> true
                    else -> false
                }
                if (sameLine) {
                    target.first.add(st)
                    gb.union(box)
                    continue
                }
            }
            groups.add(mutableListOf(st) to RectF(box))
        }
        return groups.map { it.first }
    }

    private fun splitLineByWideHorizontalGaps(line: List<Stroke>): List<List<Stroke>> {
        if (line.size < 2) return listOf(line)
        val items = line.mapNotNull { st -> strokeBounds(st)?.let { st to it } }
            .sortedBy { it.second.left }
        if (items.size < 2) return listOf(line)

        val tallHeights = items.map { it.second.height() }.filter { it > 18f }.sorted()
        val medianH = if (tallHeights.isNotEmpty()) tallHeights[tallHeights.size / 2].coerceIn(24f, 140f) else 48f
        val gapThreshold = max(95f, medianH * 1.65f)

        val chunks = mutableListOf<Pair<MutableList<Stroke>, Float>>()
        for ((st, box) in items) {
            val last = chunks.lastOrNull()
            if (last != null && box.left - last.second <= gapThreshold) {
                last.first.add(st)
                chunks[chunks.lastIndex] = last.first to max(last.second, box.right)
            } else {
                chunks.add(mutableListOf(st) to box.right)
            }
        }
        return chunks.map { it.first }
    }

    private fun strokesBounds(strokes: List<Stroke>): RectF? {
        var acc: RectF? = null
        for (st in strokes) {
            val b = strokeBounds(st) ?: continue
            if (acc == null) acc = RectF(b) else acc.union(b)
        }
        return acc
    }

    private fun lineLooksLikeAssignment(line: List<Stroke>): Boolean {
        if (line.size !in 3..45) return false
        val items = line.mapNotNull { st -> strokeBounds(st)?.let { st to it } }
        val bars = items.filter { isHorizontalBar(it.second) }
        if (bars.size < 2) return false
        for (i in bars.indices) {
            for (j in i + 1 until bars.size) {
                val b1 = bars[i].second
                val b2 = bars[j].second
                val wAvg = (b1.width() + b2.width()) * 0.5f
                val vGap = abs(b1.centerY() - b2.centerY())
                val xDiff = abs(b1.centerX() - b2.centerX())
                if (vGap in 4f..max(65f, wAvg * 1.4f) && xDiff <= max(32f, wAvg * 0.8f)) {
                    val eqLeft = min(b1.left, b2.left)
                    val eqRight = max(b1.right, b2.right)
                    val hasLeft = items.any { it.second.centerX() < eqLeft + wAvg * 0.2f }
                    val hasRight = items.any { it.second.centerX() > eqRight - wAvg * 0.2f }
                    if (hasLeft && hasRight) return true
                }
            }
        }
        return false
    }

    private fun recognizeLayoutGeometricFallback(strokes: List<Stroke>): String? {
        val frac = detectStackedFraction(strokes)
        if (frac != null) {
            val l = if (frac.leftStrokes.isNotEmpty()) recognizeLayoutGeometricFallback(frac.leftStrokes) else ""
            val n = recognizeLayoutGeometricFallback(frac.numeratorStrokes) ?: return null
            val d = recognizeLayoutGeometricFallback(frac.denominatorStrokes) ?: return null
            val r = if (frac.rightStrokes.isNotEmpty()) recognizeLayoutGeometricFallback(frac.rightStrokes) else ""
            return buildString {
                if (!l.isNullOrBlank()) append(l)
                append("((").append(n).append(")/(").append(d).append("))")
                if (!r.isNullOrBlank()) append(r)
            }
        }
        val sup = detectSuperscriptSplit(strokes)
        if (sup != null) {
            val b = recognizeLayoutGeometricFallback(sup.baseStrokes) ?: return null
            val e = recognizeLayoutGeometricFallback(sup.exponentStrokes) ?: return null
            val r = if (sup.trailingStrokes.isNotEmpty()) recognizeLayoutGeometricFallback(sup.trailingStrokes) else ""
            return buildString {
                append(b).append("^(").append(e).append(')')
                if (!r.isNullOrBlank()) append(r)
            }
        }
        return recognizeGeometricFallback(strokes)
    }

    fun coerceHandwritingLookalikes(raw: String, declaredVars: Set<String> = emptySet()): String {
        if (raw.isBlank()) return raw
        val lowerVars = declaredVars.map { it.lowercase() }.toSet()

        // If the expression contains an assignment like "z = 5" or "s = 10", protect the LHS variable name
        val eqIdx = raw.indexOf('=')
        val lhs = if (eqIdx > 0) raw.substring(0, eqIdx) else ""
        val rhs = if (eqIdx > 0) raw.substring(eqIdx) else raw

        fun fixSegment(str: String, isLhsOfAssignment: Boolean): String {
            if (isLhsOfAssignment) return str
            val sb = StringBuilder(str.length)
            for (i in str.indices) {
                val ch = str[i]
                val lower = ch.lowercaseChar().toString()
                if (lower in lowerVars) {
                    sb.append(ch)
                    continue
                }
                // Do not corrupt built-in function names or constants (sqrt, sin, cos, tan, log, ln, abs, pi, e)
                val partOfAlphaWord = (i > 0 && str[i - 1].isLetter() && str[i - 1] !in "xX") ||
                    (i < str.lastIndex && str[i + 1].isLetter() && str[i + 1] !in "xX")
                if (partOfAlphaWord) {
                    sb.append(ch)
                    continue
                }

                val prevIsDigit = i > 0 && (str[i - 1].isDigit() || str[i - 1] == '.')
                val nextIsDigit = i < str.lastIndex && (str[i + 1].isDigit() || str[i + 1] == '.')

                // Only coerce letter lookalikes to digits when adjacent to digits or inside numbers.
                // NEVER turn 'x' or 'X' into '*' here; our parser supports implicit multiplication (e.g. 2x, xy)
                // and equation solving with variable 'x' naturally!
                val mapped = when (ch) {
                    'o', 'O' -> if (prevIsDigit || nextIsDigit) '0' else ch
                    'l', 'I' -> if (prevIsDigit || nextIsDigit) '1' else ch
                    's', 'S' -> if (prevIsDigit || nextIsDigit) '5' else ch
                    'z', 'Z' -> if (prevIsDigit || nextIsDigit) '2' else ch
                    'B' -> if (prevIsDigit || nextIsDigit) '8' else ch
                    'q', 'g' -> if (prevIsDigit || nextIsDigit) '9' else ch
                    else -> ch
                }
                sb.append(mapped)
            }
            return sb.toString()
        }

        return if (eqIdx > 0) {
            fixSegment(lhs, isLhsOfAssignment = true) + fixSegment(rhs, isLhsOfAssignment = false)
        } else {
            fixSegment(raw, isLhsOfAssignment = false)
        }
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
