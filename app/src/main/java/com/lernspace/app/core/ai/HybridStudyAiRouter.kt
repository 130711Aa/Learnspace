package com.lernspace.app.core.ai

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.IOException

data class HybridFlashcardResult(
    val cards: List<GeneratedFlashcard>,
    val providerLabel: String
)

/**
 * Routes AI study requests between Gemini Flash-Lite (when API key is set & online)
 * and the local offline SLM (llama.cpp GGUF) as an automatic fallback on network/rate-limit errors.
 */
object HybridStudyAiRouter {

    var statusMessage by mutableStateOf("")
        private set

    var isUsingLocalFallback by mutableStateOf(false)
        private set

    suspend fun generateFlashcards(
        context: Context,
        sourceText: String,
        topicTitle: String,
        maxCards: Int
    ): Result<HybridFlashcardResult> {
        val useGeminiFirst = GeminiApiClient.isConfigured() && GeminiApiClient.preferGeminiWhenOnline
        val hasLocalSlm = LocalSlmManager.hasReadyModel(context)

        try {
            if (useGeminiFirst) {
                withContext(Dispatchers.Main) {
                    isUsingLocalFallback = false
                    statusMessage = "Menghubungi ${GeminiApiClient.selectedModelId}..."
                }
                val geminiRes = GeminiApiClient.generateFlashcards(
                    sourceText = sourceText,
                    topicTitle = topicTitle,
                    maxCards = maxCards
                )
                if (geminiRes.isSuccess) {
                    return Result.success(
                        HybridFlashcardResult(
                            cards = geminiRes.getOrThrow(),
                            providerLabel = GeminiApiClient.selectedModelId
                        )
                    )
                }

                val err = geminiRes.exceptionOrNull()
                val canFallback = err is IOException ||
                    (err is GeminiHttpException && (err.statusCode == 429 || err.statusCode >= 500))

                if (canFallback && hasLocalSlm) {
                    withContext(Dispatchers.Main) {
                        isUsingLocalFallback = true
                        statusMessage = "Gemini tidak terjangkau, beralih ke SLM offline..."
                    }
                    val slmRes = LocalSlmManager.generateFlashcardsFromText(
                        context = context,
                        sourceText = sourceText,
                        topicTitle = topicTitle,
                        maxCards = maxCards
                    )
                    if (slmRes.isSuccess) {
                        return Result.success(
                            HybridFlashcardResult(
                                cards = slmRes.getOrThrow(),
                                providerLabel = "SLM Offline (Fallback: ${err?.message ?: "Offline"})"
                            )
                        )
                    }
                }
                return Result.failure(err ?: IllegalStateException("Gagal menghubungi Gemini API."))
            }

            if (hasLocalSlm) {
                withContext(Dispatchers.Main) {
                    isUsingLocalFallback = true
                    statusMessage = LocalSlmManager.statusMessage.ifBlank { "Menjalankan SLM offline..." }
                }
                return LocalSlmManager.generateFlashcardsFromText(
                    context = context,
                    sourceText = sourceText,
                    topicTitle = topicTitle,
                    maxCards = maxCards
                ).map { cards ->
                    HybridFlashcardResult(
                        cards = cards,
                        providerLabel = "SLM Offline (${LocalSlmManager.activeModelFileName ?: "GGUF"})"
                    )
                }
            }

            return Result.failure(
                IllegalStateException("Belum ada Gemini API Key atau model SLM offline yang aktif.")
            )
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                isUsingLocalFallback = false
                statusMessage = ""
            }
        }
    }
}
