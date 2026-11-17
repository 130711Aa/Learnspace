package com.lernspace.app.core.supabase

import android.util.Log
import com.lernspace.app.core.ai.GeneratedQuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
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
    const val SUPABASE_URL = "https://bcmpfokmuegoomcnpxis.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJjbXBmb2ttdWVnb29tY25weGlzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQ4OTczNTIsImV4cCI6MjExMDQ3MzM1Mn0.wKvG5K90z3l7JY02u5AvAnx_SENfEjuBQk69pkU7HOA"

    /**
     * Fetch questions from Supabase with their associated question_options.
     * Optionally filtered by subjectName, topicTitle, subtopicTitle, or difficulty.
     */
    suspend fun fetchQuestions(
        subjectName: String? = null,
        topicTitle: String? = null,
        subtopicTitle: String? = null,
        difficulty: String? = null
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

            val urlString = "$SUPABASE_URL/rest/v1/questions?${queryParams.joinToString("&")}"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 20000
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream?.let { InputStreamReader(it).readText() } ?: "HTTP $responseCode"
                return@withContext Result.failure(Exception("Gagal mengambil soal dari Supabase ($responseCode): $errorStream"))
            }

            val responseBody = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val jsonArray = JSONArray(responseBody)
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

            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "fetchQuestions error: ${e.message}", e)
            Result.failure(e)
        }
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

        try {
            var uploadedCount = 0

            for (q in questions) {
                val questionId = UUID.randomUUID().toString()

                // 1. Insert Question
                val questionJson = JSONObject().apply {
                    put("id", questionId)
                    if (!subjectCode.isNullOrBlank()) put("subject_code", subjectCode)
                    put("subject_name", subjectName.ifBlank { "Umum" })
                    put("topic_title", topicTitle.ifBlank { "Materi Umum" })
                    put("subtopic_title", subtopicTitle.ifBlank { "Latihan Mandiri" })
                    put("question_text", q.question)
                    put("question_type", "MULTIPLE_CHOICE")
                    put("difficulty", difficulty)
                    put("explanation", q.explanation)
                    put("tags", JSONArray().apply {
                        put(subjectName.ifBlank { "Umum" })
                        put(topicTitle.ifBlank { "Materi" })
                        if (subtopicTitle.isNotBlank()) put(subtopicTitle)
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
