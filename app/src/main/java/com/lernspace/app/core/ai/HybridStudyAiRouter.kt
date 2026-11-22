package com.lernspace.app.core.ai

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

data class HybridFlashcardResult(
    val cards: List<GeneratedFlashcard>,
    val providerLabel: String
)

/**
 * Routes AI study requests to the configured Gemini API model with clear status reporting.
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
        if (!GeminiApiClient.isConfigured()) {
            return Result.failure(
                IllegalStateException("Gemini API Key belum diatur. Silakan atur API Key di menu Settings.")
            )
        }

        try {
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
            return Result.failure(
                geminiRes.exceptionOrNull() ?: IllegalStateException("Gagal menghubungi Gemini API.")
            )
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                isUsingLocalFallback = false
                statusMessage = ""
            }
        }
    }
}
