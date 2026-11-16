package com.lernspace.app.ui.workspace.ai

import com.lernspace.app.core.ai.GeneratedFlashcard
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AiQuickAction(
    val title: String,
    val emoji: String,
    val defaultPrompt: String
) {
    SUMMARIZE(
        title = "Rangkum Halaman",
        emoji = "📝",
        defaultPrompt = "Rangkum seluruh materi pada halaman yang sedang saya buka ini secara terstruktur, lengkap dengan poin-poin penting, intisari materi, dan kesimpulan utamanya."
    ),
    KEY_CONCEPTS(
        title = "Konsep Kunci",
        emoji = "💡",
        defaultPrompt = "Identifikasi dan jelaskan konsep-konsep kunci, istilah penting, atau definisi utama pada halaman ini menggunakan analogi yang mudah dipahami mahasiswa."
    ),
    QUIZ(
        title = "Uji Pemahaman",
        emoji = "❓",
        defaultPrompt = "Berdasarkan materi pada halaman ini, berikan 1 pertanyaan konsep gaya Feynman / Socratic (analisis/pemahaman mendalam, bukan sekadar hafalan teks). Jangan langsung berikan kunci jawaban, tunggu jawaban saya lalu evaluasi."
    ),
    FLASHCARDS(
        title = "Buat Flashcards",
        emoji = "🃏",
        defaultPrompt = "Ekstrak 3 sampai 5 kartu tanya-jawab (Flashcards) dari materi di halaman ini. Format output dalam JSON array valid dengan struktur: [{\"front\": \"pertanyaan\", \"back\": \"jawaban\", \"hint\": \"petunjuk singkat\"}]. Selain JSON, sertakan ulasan singkat."
    ),
    FORMULAS(
        title = "Bedah Rumus",
        emoji = "📐",
        defaultPrompt = "Identifikasi semua rumus atau persamaan matematika/fisika/kimia di halaman ini. Tuliskan dalam format LaTeX bersih, lalu jelaskan arti setiap variabel, satuannya, dan kondisi berlakunya rumus tersebut."
    )
}

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val pageNumber: Int? = null,
    val isStreaming: Boolean = false,
    val hasError: Boolean = false,
    val flashcards: List<GeneratedFlashcard>? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun parseFlashcardsIfPresent(text: String): List<GeneratedFlashcard>? {
            return try {
                val startIdx = text.indexOf('[')
                val endIdx = text.lastIndexOf(']')
                if (startIdx != -1 && endIdx > startIdx) {
                    val jsonSub = text.substring(startIdx, endIdx + 1)
                    val arr = JSONArray(jsonSub)
                    val list = mutableListOf<GeneratedFlashcard>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val front = obj.optString("front", "").trim()
                        val back = obj.optString("back", "").trim()
                        val hint = obj.optString("hint", "").trim()
                        if (front.isNotBlank() && back.isNotBlank()) {
                            list.add(GeneratedFlashcard(front = front, back = back, hint = hint))
                        }
                    }
                    if (list.isNotEmpty()) list else null
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
}
