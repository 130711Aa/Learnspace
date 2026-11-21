package com.lernspace.app.core.supabase

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lernspace.app.core.ai.GeneratedQuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Data model for an option in Supabase question_options table.
 */
data class BankQuestionOption(
    val id: String = UUID.randomUUID().toString(),
    val questionId: String,
    val optionKey: String,
    val optionText: String,
    val isCorrect: Boolean,
    val orderIndex: Int
)

/**
 * Data model for a question stored in Supabase questions table.
 */
data class BankQuestion(
    val id: String = UUID.randomUUID().toString(),
    val subjectCode: String? = null,
    val subjectName: String,
    val topicTitle: String,
    val subtopicTitle: String? = null,
    val questionText: String,
    val questionType: String = "MULTIPLE_CHOICE",
    val difficulty: String = "MEDIUM",
    val explanation: String? = null,
    val tags: List<String> = emptyList(),
    val options: List<BankQuestionOption> = emptyList(),
    val createdAt: String? = null
)

/**
 * Clean, lightweight REST client for Supabase Bank Soal (Questions & Options)
 * using standard HttpURLConnection without external heavy dependencies.
 */
object SupabaseBankSoalClient {

    private const val TAG = "SupabaseBankSoal"
    private const val CACHE_FILE_NAME = "bank_soal_offline_cache.json"

    const val SUPABASE_URL = "https://bcmpfokmuegoomcnpxis.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJjbXBmb2ttdWVnb29tY25weGlzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQ4OTczNTIsImV4cCI6MjExMDQ3MzM1Mn0.wKvG5K90z3l7JY02u5AvAnx_SENfEjuBQk69pkU7HOA"

    private var appContext: Context? = null

    /**
     * Observable state indicating if the last loaded question list came from offline cache.
     */
    var isOperatingOffline by mutableStateOf(false)
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Fetch questions from Supabase with their associated question_options.
     * Supports pagination via [limit] and [offset].
     * If network connection fails, automatically falls back to offline cached questions.
     */
    suspend fun fetchQuestions(
        subjectName: String? = null,
        topicTitle: String? = null,
        subtopicTitle: String? = null,
        difficulty: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<BankQuestion>> = withContext(Dispatchers.IO) {
        try {
            val queryParams = mutableListOf("select=*,question_options(*)", "order=created_at.desc")

            if (!subjectName.isNullOrBlank() && subjectName != "Semua") {
                queryParams.add("subject_name=eq.${URLEncoder.encode(subjectName, "UTF-8")}")
            }
            if (!topicTitle.isNullOrBlank() && topicTitle != "Semua") {
                queryParams.add("topic_title=eq.${URLEncoder.encode(topicTitle, "UTF-8")}")
            }
            if (!subtopicTitle.isNullOrBlank() && subtopicTitle != "Semua") {
                queryParams.add("subtopic_title=eq.${URLEncoder.encode(subtopicTitle, "UTF-8")}")
            }
            if (!difficulty.isNullOrBlank() && difficulty != "Semua") {
                queryParams.add("difficulty=eq.${URLEncoder.encode(difficulty, "UTF-8")}")
            }
            queryParams.add("limit=$limit")
            if (offset > 0) {
                queryParams.add("offset=$offset")
            }

            val urlString = "$SUPABASE_URL/rest/v1/questions?${queryParams.joinToString("&")}"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 12000
                readTimeout = 15000
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream?.let { InputStreamReader(it).readText() } ?: "HTTP $responseCode"
                throw Exception("Supabase HTTP $responseCode: $errorStream")
            }

            val responseBody = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val questions = parseQuestionsJson(responseBody)

            // Cache successfully fetched questions to disk for offline access
            if (questions.isNotEmpty()) {
                saveQuestionsToCache(questions)
            }
            isOperatingOffline = false

            Result.success(questions)
        } catch (e: Exception) {
            Log.w(TAG, "fetchQuestions network failed: ${e.message}. Attempting offline cache fallback.")
            val cached = loadQuestionsFromCache()
            if (cached.isNotEmpty()) {
                isOperatingOffline = true
                var filtered = cached
                if (!subjectName.isNullOrBlank() && subjectName != "Semua") {
                    filtered = filtered.filter { it.subjectName.equals(subjectName, ignoreCase = true) }
                }
                if (!topicTitle.isNullOrBlank() && topicTitle != "Semua") {
                    filtered = filtered.filter { it.topicTitle.equals(topicTitle, ignoreCase = true) }
                }
                if (!subtopicTitle.isNullOrBlank() && subtopicTitle != "Semua") {
                    filtered = filtered.filter { it.subtopicTitle?.equals(subtopicTitle, ignoreCase = true) == true }
                }
                if (!difficulty.isNullOrBlank() && difficulty != "Semua") {
                    filtered = filtered.filter { it.difficulty.equals(difficulty, ignoreCase = true) }
                }
                Result.success(filtered)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun parseQuestionsJson(jsonString: String): List<BankQuestion> {
        val jsonArray = JSONArray(jsonString)
        val questions = mutableListOf<BankQuestion>()

        for (i in 0 until jsonArray.length()) {
            val qObj = jsonArray.getJSONObject(i)
            val qId = qObj.optString("id")
            val subjCode = qObj.optString("subject_code", "").takeIf { it.isNotBlank() }
            val subjName = qObj.optString("subject_name", "Umum")
            val topic = qObj.optString("topic_title", "Umum")
            val subtopic = qObj.optString("subtopic_title", "").takeIf { it.isNotBlank() }
            val text = qObj.optString("question_text", "")
            val type = qObj.optString("question_type", "MULTIPLE_CHOICE")
            val diff = qObj.optString("difficulty", "MEDIUM")
            val expl = qObj.optString("explanation", "").takeIf { it.isNotBlank() }
            val createdAt = qObj.optString("created_at", "")

            val tagsList = mutableListOf<String>()
            val tagsArr = qObj.optJSONArray("tags")
            if (tagsArr != null) {
                for (t in 0 until tagsArr.length()) {
                    tagsList.add(tagsArr.optString(t))
                }
            }

            val optionsList = mutableListOf<BankQuestionOption>()
            val optsArr = qObj.optJSONArray("question_options")
            if (optsArr != null) {
                for (o in 0 until optsArr.length()) {
                    val optObj = optsArr.getJSONObject(o)
                    optionsList.add(
                        BankQuestionOption(
                            id = optObj.optString("id", UUID.randomUUID().toString()),
                            questionId = optObj.optString("question_id", qId),
                            optionKey = optObj.optString("option_key", "${'A' + o}"),
                            optionText = optObj.optString("option_text", ""),
                            isCorrect = optObj.optBoolean("is_correct", false),
                            orderIndex = optObj.optInt("order_index", o + 1)
                        )
                    )
                }
            }
            optionsList.sortBy { it.orderIndex }

            questions.add(
                BankQuestion(
                    id = qId,
                    subjectCode = subjCode,
                    subjectName = subjName,
                    topicTitle = topic,
                    subtopicTitle = subtopic,
                    questionText = text,
                    questionType = type,
                    difficulty = diff,
                    explanation = expl,
                    tags = tagsList,
                    options = optionsList,
                    createdAt = createdAt
                )
            )
        }
        return questions
    }

    private fun saveQuestionsToCache(questions: List<BankQuestion>) {
        val ctx = appContext ?: return
        try {
            val rootArray = JSONArray()
            questions.forEach { q ->
                val qObj = JSONObject().apply {
                    put("id", q.id)
                    put("subject_code", q.subjectCode ?: "")
                    put("subject_name", q.subjectName)
                    put("topic_title", q.topicTitle)
                    put("subtopic_title", q.subtopicTitle ?: "")
                    put("question_text", q.questionText)
                    put("question_type", q.questionType)
                    put("difficulty", q.difficulty)
                    put("explanation", q.explanation ?: "")
                    put("created_at", q.createdAt ?: "")
                    put("tags", JSONArray(q.tags))

                    val optsArr = JSONArray()
                    q.options.forEach { opt ->
                        optsArr.put(
                            JSONObject().apply {
                                put("id", opt.id)
                                put("question_id", opt.questionId)
                                put("option_key", opt.optionKey)
                                put("option_text", opt.optionText)
                                put("is_correct", opt.isCorrect)
                                put("order_index", opt.orderIndex)
                            }
                        )
                    }
                    put("question_options", optsArr)
                }
                rootArray.put(qObj)
            }
            val cacheFile = File(ctx.filesDir, CACHE_FILE_NAME)
            cacheFile.writeText(rootArray.toString())
        } catch (e: Exception) {
            Log.e(TAG, "saveQuestionsToCache error: ${e.message}")
        }
    }

    fun loadQuestionsFromCache(): List<BankQuestion> {
        val ctx = appContext ?: return emptyList()
        return try {
            val cacheFile = File(ctx.filesDir, CACHE_FILE_NAME)
            if (!cacheFile.exists()) return emptyList()
            val text = cacheFile.readText()
            if (text.isBlank()) return emptyList()
            parseQuestionsJson(text)
        } catch (e: Exception) {
            Log.e(TAG, "loadQuestionsFromCache error: ${e.message}")
            emptyList()
        }
    }

    data class TaxonomyEntry(
        val subject: String,
        val topic: String,
        val subtopic: String? = null
    )

    private val inMemoryTaxonomies = mutableListOf<TaxonomyEntry>()

    /**
     * Fetches distinct academic taxonomies (Subject > Topic > Subtopic) currently in Supabase or local cache.
     * Used as grounding reference for Gemini to prevent duplicate or divergent concept labels.
     */
    suspend fun fetchExistingTaxonomies(): List<String> = withContext(Dispatchers.IO) {
        val results = linkedSetOf<String>()
        val parsedEntries = mutableListOf<TaxonomyEntry>()
        try {
            val urlString = "$SUPABASE_URL/rest/v1/questions?select=subject_name,topic_title,subtopic_title&limit=300"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 6000
                readTimeout = 8000
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val s = obj.optString("subject_name", "").trim()
                    val t = obj.optString("topic_title", "").trim()
                    val sub = obj.optString("subtopic_title", "").trim()
                    if (s.isNotBlank() && t.isNotBlank()) {
                        val subOrNull = if (sub.isNotBlank()) sub else null
                        parsedEntries.add(TaxonomyEntry(s, t, subOrNull))
                        if (subOrNull != null) {
                            results.add("$s > $t > $subOrNull")
                        } else {
                            results.add("$s > $t")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchExistingTaxonomies network error: ${e.message}")
        }

        // Fallback to local cache if offline or network returned empty
        if (results.isEmpty()) {
            val cached = loadQuestionsFromCache()
            for (q in cached) {
                val s = q.subjectName.trim()
                val t = q.topicTitle.trim()
                val sub = q.subtopicTitle?.trim().orEmpty()
                if (s.isNotBlank() && t.isNotBlank()) {
                    val subOrNull = if (sub.isNotBlank()) sub else null
                    parsedEntries.add(TaxonomyEntry(s, t, subOrNull))
                    if (subOrNull != null) results.add("$s > $t > $subOrNull") else results.add("$s > $t")
                }
            }
        }

        if (parsedEntries.isNotEmpty()) {
            synchronized(inMemoryTaxonomies) {
                inMemoryTaxonomies.clear()
                inMemoryTaxonomies.addAll(parsedEntries)
            }
        }
        results.toList()
    }

    /**
     * Normalizes Subject, Topic, and Subtopic names against existing questions in database or cache.
     * Uses fuzzy token overlap and Levenshtein similarity to snap typos, slight synonyms, or
     * missing words (e.g. "Sel Volta dan Elektrolisi" -> "Sel Volta dan Sel Elektrolisis").
     */
    fun normalizeTaxonomy(
        rawSubject: String,
        rawTopic: String,
        rawSubtopic: String,
        referenceQuestions: List<BankQuestion> = emptyList(),
        taxonomyList: List<String> = emptyList()
    ): Triple<String, String, String> {
        var finalSubject = rawSubject.trim()
        var finalTopic = rawTopic.trim()
        var finalSubtopic = rawSubtopic.trim()

        val entries: List<TaxonomyEntry> = when {
            referenceQuestions.isNotEmpty() -> {
                referenceQuestions.map { TaxonomyEntry(it.subjectName.trim(), it.topicTitle.trim(), it.subtopicTitle?.trim()) }
            }
            synchronized(inMemoryTaxonomies) { inMemoryTaxonomies.isNotEmpty() } -> {
                synchronized(inMemoryTaxonomies) { inMemoryTaxonomies.toList() }
            }
            taxonomyList.isNotEmpty() -> {
                taxonomyList.mapNotNull { line ->
                    val parts = line.split(">").map { it.trim() }
                    if (parts.size >= 2) {
                        TaxonomyEntry(parts[0], parts[1], parts.getOrNull(2))
                    } else null
                }
            }
            else -> {
                loadQuestionsFromCache().map { TaxonomyEntry(it.subjectName.trim(), it.topicTitle.trim(), it.subtopicTitle?.trim()) }
            }
        }

        if (entries.isEmpty()) {
            return Triple(finalSubject, finalTopic, finalSubtopic)
        }

        // 1. Normalize Subject
        val existingSubjects = entries.map { it.subject }.filter { it.isNotBlank() }.distinct()
        findCloseMatch(finalSubject, existingSubjects)?.let { finalSubject = it }

        // 2. Normalize Topic within matching Subject
        val subjectEntries = entries.filter { it.subject.equals(finalSubject, ignoreCase = true) }
        val candidateTopics = if (subjectEntries.isNotEmpty()) {
            subjectEntries.map { it.topic }.filter { it.isNotBlank() }.distinct()
        } else {
            entries.map { it.topic }.filter { it.isNotBlank() }.distinct()
        }
        findCloseMatch(finalTopic, candidateTopics)?.let { finalTopic = it }

        // 3. Normalize Subtopic within matching Subject & Topic
        if (finalSubtopic.isNotBlank()) {
            val topicEntries = subjectEntries.filter { it.topic.equals(finalTopic, ignoreCase = true) }
            val candidateSubtopics = if (topicEntries.isNotEmpty()) {
                topicEntries.mapNotNull { it.subtopic }.filter { it.isNotBlank() }.distinct()
            } else {
                entries.mapNotNull { it.subtopic }.filter { it.isNotBlank() }.distinct()
            }
            findCloseMatch(finalSubtopic, candidateSubtopics)?.let { finalSubtopic = it }
        }

        return Triple(finalSubject, finalTopic, finalSubtopic)
    }

    private fun findCloseMatch(target: String, candidates: List<String>): String? {
        if (target.isBlank() || candidates.isEmpty()) return null

        // Exact match case-insensitive
        candidates.firstOrNull { it.equals(target, ignoreCase = true) }?.let { return it }

        val targetTokens = tokenize(target)
        if (targetTokens.isEmpty()) return null

        var bestMatch: String? = null
        var highestScore = 0.0

        for (cand in candidates) {
            val candTokens = tokenize(cand)
            if (candTokens.isEmpty()) continue

            // Token overlap (stem/prefix matching of 4+ chars)
            val matchedTokens = targetTokens.count { t ->
                candTokens.any { c ->
                    t == c || (t.length >= 4 && c.length >= 4 && (t.startsWith(c.take(4)) || c.startsWith(t.take(4))))
                }
            }
            val unionCount = (targetTokens + candTokens).distinct().size
            val jaccard = if (unionCount > 0) matchedTokens.toDouble() / unionCount else 0.0

            val dist = levenshteinDistance(target.lowercase(), cand.lowercase())
            val maxLen = maxOf(target.length, cand.length)
            val levSim = if (maxLen > 0) 1.0 - (dist.toDouble() / maxLen) else 0.0

            val combined = maxOf(jaccard, levSim)
            if (combined > highestScore) {
                highestScore = combined
                bestMatch = cand
            }
        }

        // If >= 65% match, consider it a variation/typo of the existing canonical taxonomy
        return if (highestScore >= 0.65) bestMatch else null
    }

    private fun tokenize(text: String): Set<String> {
        val stopWords = setOf("dan", "di", "ke", "dari", "yang", "pada", "untuk", "dengan", "atau", "tentang")
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 && it !in stopWords }
            .toSet()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Upload newly generated AI quiz questions into Supabase questions and question_options.
     * Returns the count of uploaded questions.
     */
    suspend fun uploadGeneratedQuestions(
        questions: List<GeneratedQuizQuestion>,
        subjectName: String,
        topicTitle: String,
        subtopicTitle: String,
        difficulty: String = "MEDIUM",
        subjectCode: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (questions.isEmpty()) return@withContext Result.success(0)

        // Automatically normalize taxonomy against existing database rows before inserting
        val (normSubject, normTopic, normSubtopic) = normalizeTaxonomy(
            rawSubject = subjectName,
            rawTopic = topicTitle,
            rawSubtopic = subtopicTitle
        )

        try {
            var uploadedCount = 0

            for (q in questions) {
                val questionId = UUID.randomUUID().toString()

                // 1. Insert Question
                val questionJson = JSONObject().apply {
                    put("id", questionId)
                    if (!subjectCode.isNullOrBlank()) put("subject_code", subjectCode)
                    put("subject_name", normSubject.ifBlank { "Umum" })
                    put("topic_title", normTopic.ifBlank { "Materi Umum" })
                    put("subtopic_title", normSubtopic.ifBlank { "Latihan Mandiri" })
                    put("question_text", q.question)
                    put("question_type", "MULTIPLE_CHOICE")
                    put("difficulty", difficulty)
                    put("explanation", q.explanation)
                    put("tags", JSONArray().apply {
                        put(normSubject.ifBlank { "Umum" })
                        put(normTopic.ifBlank { "Materi" })
                        if (normSubtopic.isNotBlank()) put(normSubtopic)
                    })
                }

                val qArray = JSONArray().put(questionJson)
                val qSuccess = postJson("$SUPABASE_URL/rest/v1/questions", qArray.toString())
                if (!qSuccess) {
                    Log.w(TAG, "Failed inserting question: ${q.question.take(50)}")
                    continue
                }

                // 2. Insert Options
                val optionsArray = JSONArray()
                val optKeys = listOf("A", "B", "C", "D", "E")
                q.options.forEachIndexed { idx, optText ->
                    val optObj = JSONObject().apply {
                        put("id", UUID.randomUUID().toString())
                        put("question_id", questionId)
                        put("option_key", optKeys.getOrElse(idx) { "${'A' + idx}" })
                        put("option_text", optText)
                        put("is_correct", idx == q.correctIndex)
                        put("order_index", idx + 1)
                    }
                    optionsArray.put(optObj)
                }

                val optSuccess = postJson("$SUPABASE_URL/rest/v1/question_options", optionsArray.toString())
                if (optSuccess) {
                    uploadedCount++
                } else {
                    Log.w(TAG, "Failed inserting options for question $questionId")
                }
            }

            Result.success(uploadedCount)
        } catch (e: Exception) {
            Log.e(TAG, "uploadGeneratedQuestions error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Records a completed quiz session into Supabase quiz_sessions table.
     */
    suspend fun saveQuizSession(
        title: String,
        subjectName: String?,
        totalQuestions: Int,
        correctAnswers: Int,
        score: Double,
        durationSeconds: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sessionId = UUID.randomUUID().toString()
            val sessionJson = JSONObject().apply {
                put("id", sessionId)
                put("title", title)
                if (!subjectName.isNullOrBlank()) put("subject_name", subjectName)
                put("total_questions", totalQuestions)
                put("correct_answers", correctAnswers)
                put("score", score)
                put("duration_seconds", durationSeconds)
            }
            val arr = JSONArray().put(sessionJson)
            val success = postJson("$SUPABASE_URL/rest/v1/quiz_sessions", arr.toString())
            if (success) {
                Result.success(sessionId)
            } else {
                Result.failure(Exception("Gagal menyimpan riwayat kuis ke Supabase."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun postJson(urlString: String, jsonBody: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                Log.e(TAG, "POST error $code: $err")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "postJson exception: ${e.message}", e)
            false
        } finally {
            conn?.disconnect()
        }
    }
}
