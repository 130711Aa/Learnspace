package com.lernspace.app.core.supabase

import android.util.Log
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

data class SupabaseOption(
    val id: String = "",
    val optionKey: String,
    val optionText: String,
    val isCorrect: Boolean,
    val orderIndex: Int = 0
)

data class SupabaseQuestion(
    val id: String = "",
    val subjectCode: String? = null,
    val subjectName: String,
    val topicTitle: String,
    val questionText: String,
    val questionType: String = "MULTIPLE_CHOICE",
    val difficulty: String = "MEDIUM",
    val explanation: String? = null,
    val tags: List<String> = emptyList(),
    val options: List<SupabaseOption> = emptyList()
)

data class QuizSubmissionAnswer(
    val questionId: String,
    val selectedOptionId: String?,
    val isCorrect: Boolean,
    val timeSpentSeconds: Int = 0
)

data class QuizSubmission(
    val title: String,
    val subjectName: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val score: Double,
    val durationSeconds: Int,
    val answers: List<QuizSubmissionAnswer> = emptyList()
)

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    const val BASE_URL = "https://bcmpfokmuegoomcnpxis.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJjbXBmb2ttdWVnb29tY25weGlzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQ4OTczNTIsImV4cCI6MjExMDQ3MzM1Mn0.wKvG5K90z3l7JY02u5AvAnx_SENfEjuBQk69pkU7HOA"

    suspend fun fetchQuestions(
        subjectName: String? = null,
        difficulty: String? = null,
        limit: Int = 30
    ): Result<List<SupabaseQuestion>> = withContext(Dispatchers.IO) {
        try {
            val queryParams = mutableListOf(
                "select=" + URLEncoder.encode("*,question_options(*)", "UTF-8"),
                "order=" + URLEncoder.encode("created_at.desc", "UTF-8"),
                "limit=$limit"
            )
            if (!subjectName.isNullOrBlank() && subjectName != "Semua") {
                queryParams.add("subject_name=eq." + URLEncoder.encode(subjectName, "UTF-8"))
            }
            if (!difficulty.isNullOrBlank() && difficulty != "Semua") {
                queryParams.add("difficulty=eq." + URLEncoder.encode(difficulty, "UTF-8"))
            }

            val urlStr = "$BASE_URL/rest/v1/questions?" + queryParams.joinToString("&")
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val respText = reader.readText()
                reader.close()
                val jsonArr = JSONArray(respText)
                val list = mutableListOf<SupabaseQuestion>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val optArr = obj.optJSONArray("question_options") ?: JSONArray()
                    val options = mutableListOf<SupabaseOption>()
                    for (j in 0 until optArr.length()) {
                        val optObj = optArr.getJSONObject(j)
                        options.add(
                            SupabaseOption(
                                id = optObj.optString("id"),
                                optionKey = optObj.optString("option_key"),
                                optionText = optObj.optString("option_text"),
                                isCorrect = optObj.optBoolean("is_correct", false),
                                orderIndex = optObj.optInt("order_index", j)
                            )
                        )
                    }
                    options.sortBy { it.orderIndex }

                    val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                    val tags = mutableListOf<String>()
                    for (k in 0 until tagsArr.length()) {
                        tags.add(tagsArr.getString(k))
                    }

                    list.add(
                        SupabaseQuestion(
                            id = obj.optString("id"),
                            subjectCode = obj.optString("subject_code").takeIf { it.isNotBlank() },
                            subjectName = obj.optString("subject_name"),
                            topicTitle = obj.optString("topic_title"),
                            questionText = obj.optString("question_text"),
                            questionType = obj.optString("question_type", "MULTIPLE_CHOICE"),
                            difficulty = obj.optString("difficulty", "MEDIUM"),
                            explanation = obj.optString("explanation").takeIf { it.isNotBlank() },
                            tags = tags,
                            options = options
                        )
                    )
                }
                Result.success(list)
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                Result.failure(Exception("Supabase error: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching questions from Supabase", e)
            Result.failure(e)
        }
    }

    suspend fun submitQuizSession(submission: QuizSubmission): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sessUrl = URL("$BASE_URL/rest/v1/quiz_sessions")
            val sessConn = (sessUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val sessJson = JSONObject().apply {
                put("title", submission.title)
                put("subject_name", submission.subjectName)
                put("total_questions", submission.totalQuestions)
                put("correct_answers", submission.correctAnswers)
                put("score", submission.score)
                put("duration_seconds", submission.durationSeconds)
                val utcFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                put("completed_at", utcFormat.format(java.util.Date()))
            }

            OutputStreamWriter(sessConn.outputStream).use { it.write(sessJson.toString()) }

            val sessCode = sessConn.responseCode
            if (sessCode !in 200..299) {
                val err = sessConn.errorStream?.bufferedReader()?.readText() ?: "HTTP $sessCode"
                return@withContext Result.failure(Exception("Gagal simpan sesi kuis: $err"))
            }

            val sessResp = sessConn.inputStream.bufferedReader().readText()
            val createdSessArr = JSONArray(sessResp)
            val sessionId = createdSessArr.getJSONObject(0).optString("id")

            if (submission.answers.isNotEmpty() && sessionId.isNotBlank()) {
                val ansUrl = URL("$BASE_URL/rest/v1/quiz_answers")
                val ansConn = (ansUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("apikey", ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $ANON_KEY")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val ansArr = JSONArray()
                for (a in submission.answers) {
                    ansArr.put(JSONObject().apply {
                        put("session_id", sessionId)
                        put("question_id", a.questionId)
                        if (a.selectedOptionId != null) put("selected_option_id", a.selectedOptionId)
                        put("is_correct", a.isCorrect)
                        put("time_spent_seconds", a.timeSpentSeconds)
                    })
                }
                OutputStreamWriter(ansConn.outputStream).use { it.write(ansArr.toString()) }
                ansConn.responseCode
            }

            Result.success(sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting quiz session", e)
            Result.failure(e)
        }
    }

    suspend fun insertQuestion(
        subjectName: String,
        topicTitle: String,
        questionText: String,
        difficulty: String,
        explanation: String,
        options: List<Pair<String, Boolean>>,
        subjectCode: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val qUrl = URL("$BASE_URL/rest/v1/questions")
            val qConn = (qUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val qJson = JSONObject().apply {
                if (!subjectCode.isNullOrBlank()) put("subject_code", subjectCode)
                put("subject_name", subjectName)
                put("topic_title", topicTitle)
                put("question_text", questionText)
                put("question_type", "MULTIPLE_CHOICE")
                put("difficulty", difficulty)
                if (explanation.isNotBlank()) put("explanation", explanation)
            }
            OutputStreamWriter(qConn.outputStream).use { it.write(qJson.toString()) }

            val code = qConn.responseCode
            if (code !in 200..299) {
                val err = qConn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                return@withContext Result.failure(Exception("Gagal simpan soal: $err"))
            }

            val resp = qConn.inputStream.bufferedReader().readText()
            val questionId = JSONArray(resp).getJSONObject(0).optString("id")

            if (options.isNotEmpty() && questionId.isNotBlank()) {
                val optUrl = URL("$BASE_URL/rest/v1/question_options")
                val optConn = (optUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("apikey", ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $ANON_KEY")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val optArr = JSONArray()
                val keys = listOf("A", "B", "C", "D", "E")
                options.forEachIndexed { idx, (text, isCorrect) ->
                    optArr.put(JSONObject().apply {
                        put("question_id", questionId)
                        put("option_key", keys.getOrElse(idx) { "${idx + 1}" })
                        put("option_text", text)
                        put("is_correct", isCorrect)
                        put("order_index", idx + 1)
                    })
                }
                OutputStreamWriter(optConn.outputStream).use { it.write(optArr.toString()) }
                optConn.responseCode
            }

            Result.success(questionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting question", e)
            Result.failure(e)
        }
    }
}
