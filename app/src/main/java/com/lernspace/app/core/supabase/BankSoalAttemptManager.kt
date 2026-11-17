package com.lernspace.app.core.supabase

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONArray
import org.json.JSONObject

data class QuestionAttempt(
    val questionId: String,
    val selectedOptionIndex: Int,
    val isCorrect: Boolean,
    val timestamp: Long
)

/**
 * Manages the user's historical attempts and unlocks the "Collection Bucket"
 * (Koleksi & Pembahasan). Questions only show answers/explanations after being attempted!
 */
object BankSoalAttemptManager {

    private const val PREFS_NAME = "lernspace_bank_soal_attempts"
    private const val KEY_ATTEMPTS = "completed_attempts_json"

    private var prefs: SharedPreferences? = null
    val attempts = mutableStateMapOf<String, QuestionAttempt>()

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        loadFromPrefs(sp)
    }

    private fun loadFromPrefs(sp: SharedPreferences) {
        val jsonStr = sp.getString(KEY_ATTEMPTS, null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            attempts.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val qId = obj.getString("questionId")
                val optIdx = obj.getInt("selectedOptionIndex")
                val isCorrect = obj.getBoolean("isCorrect")
                val time = obj.optLong("timestamp", System.currentTimeMillis())
                attempts[qId] = QuestionAttempt(qId, optIdx, isCorrect, time)
            }
        } catch (_: Exception) { }
    }

    private fun saveToPrefs() {
        val sp = prefs ?: return
        val arr = JSONArray()
        attempts.values.forEach { att ->
            val obj = JSONObject().apply {
                put("questionId", att.questionId)
                put("selectedOptionIndex", att.selectedOptionIndex)
                put("isCorrect", att.isCorrect)
                put("timestamp", att.timestamp)
            }
            arr.put(obj)
        }
        sp.edit().putString(KEY_ATTEMPTS, arr.toString()).apply()
    }

    fun recordAttempt(questionId: String, selectedOptionIndex: Int, isCorrect: Boolean) {
        attempts[questionId] = QuestionAttempt(
            questionId = questionId,
            selectedOptionIndex = selectedOptionIndex,
            isCorrect = isCorrect,
            timestamp = System.currentTimeMillis()
        )
        saveToPrefs()
    }

    fun isAttempted(questionId: String): Boolean = attempts.containsKey(questionId)

    fun getAttempt(questionId: String): QuestionAttempt? = attempts[questionId]
}
