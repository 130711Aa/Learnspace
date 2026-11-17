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
    val displayContent: String
        get() = cleanContentForDisplay(content, flashcards, isStreaming)

    companion object {
        fun cleanContentForDisplay(
            rawText: String,
            flashcards: List<GeneratedFlashcard>? = null,
            isStreaming: Boolean = false
        ): String {
            if (rawText.isBlank()) return ""
            var text = rawText

            // If not streaming or if closing bracket exists, strip flashcard JSON array from visible text
            val startIdx = text.indexOf('[')
            val endIdx = text.lastIndexOf(']')
            if (startIdx != -1 && (endIdx > startIdx || !isStreaming)) {
                val potentialJson = if (endIdx > startIdx) text.substring(startIdx, endIdx + 1) else text.substring(startIdx)
                if (potentialJson.contains("\"front\"") || potentialJson.contains("\"back\"")) {
                    text = text.substring(0, startIdx).trimEnd()
                }
            }

            // Strip ```json ... ``` code blocks
            text = text.replace(Regex("""```(?:json)?\s*\[\s*\{[\s\S]*?\}\s*\]\s*```"""), "").trimEnd()

            // Convert common inline HTML tags to clean Markdown
            text = text.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            text = text.replace(Regex("""</?(?:b|strong)>""", RegexOption.IGNORE_CASE), "**")
            text = text.replace(Regex("""</?(?:i|em)>""", RegexOption.IGNORE_CASE), "*")
            text = text.replace(Regex("""</?code>""", RegexOption.IGNORE_CASE), "`")

            // Strip structural and container HTML tags (div, p, span, details, summary, table, etc.)
            text = text.replace(Regex("""</?(?:div|span|p|details|summary|table|thead|tbody|tr|td|th|ul|ol|li)[^>]*>""", RegexOption.IGNORE_CASE), "")

            // Strip any remaining XML/HTML tags
            text = text.replace(Regex("""<[a-zA-Z/][^>]*>"""), "")

            text = text.trim()

            // If user only got flashcards and no other text, provide a clean summary message
            if (text.isBlank() && !flashcards.isNullOrEmpty()) {
                return "✨ Berhasil membuat **${flashcards.size} Flashcard** dari materi halaman ini. Klik tombol di bawah untuk menyimpannya ke Deck."
            }

            return text
        }

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
