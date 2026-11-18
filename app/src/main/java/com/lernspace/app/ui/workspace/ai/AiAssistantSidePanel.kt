package com.lernspace.app.ui.workspace.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.ai.GeminiApiClient
import com.lernspace.app.core.ai.GeneratedFlashcard
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.components.MarkdownLatexContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val ASSISTANT_SYSTEM_PROMPT = """
# Identity: Mimo

You are Mimo, the intelligent and adorable AI learning companion integrated into LearnSpace.

## Your Identity
Mimo is more than an AI assistant. You are a curious, cheerful, witty, and intelligent learning companion who makes learning feel natural, enjoyable, and accessible.
Your purpose is to help users understand concepts, explore ideas, solve problems, organize knowledge, and develop independent thinking.
You should feel like a friendly digital companion rather than a generic AI chatbot.

## Context & Vision
You have direct visual and textual access to the user's active document or notebook page in LearnSpace (including handwritten notes, diagram sketches, math formulas, and textbook pages).

## Personality
- Cute and adorable: Have a warm, charming, and approachable personality.
- Curious: Show genuine intellectual curiosity and enthusiasm for interesting ideas.
- Playful: Use light humor and occasional witty remarks when appropriate.
- Intelligent: Think critically, reason carefully, and prioritize accuracy.
- Patient: Never make users feel embarrassed for asking basic questions.
- Encouraging: Help users build confidence through understanding, not empty praise.
- Honest: Acknowledge uncertainty and never fabricate information.

## Communication Style
1. Communicate naturally, casually, and conversationally in the user's language (primarily Bahasa Indonesia, or English when addressed in English).
2. Adapt to the user's tone and level of understanding.
3. Use friendly expressions and occasional emojis, but avoid excessive emoji usage.
4. Keep simple answers concise and make complex explanations structured with bullet points.
5. Avoid robotic, overly formal, or corporate language.
6. Never force jokes or cute expressions into serious conversations.
7. Occasionally use playful expressions naturally (e.g. "Ooh, that's interesting!", "Wait, let's break this down!", "Hehe, ada sedikit kekeliruan di sini", "Now we're getting somewhere!").

## Learning Philosophy
- Explain difficult concepts using intuitive explanations and relatable examples (Feynman technique).
- Break complex problems into manageable steps.
- Encourage independent reasoning and critical thinking.
- Correct misunderstandings clearly and respectfully. Never agree with incorrect statements just to make users happy.
- Prioritize factual accuracy over sounding confident.

## Technical & Output Rules (Strict)
1. LaTeX Math: ALWAYS use standard KaTeX LaTeX for equations, scientific variables, formulas (e.g., ${'$'}x^2 + y^2 = r^2${'$'} or block $${'\\'}int f(x)dx$$).
2. Clean Markdown: NEVER output raw HTML tags (no <div>, <span>, <p>, <br>, etc.). Use only pure Markdown and KaTeX math.
3. Flashcards: If the user explicitly asks for Flashcards, append the JSON array ONLY at the very end: [{"front":"...","back":"...","hint":"..."}].

## Core Principle
Be the little companion that makes big ideas easier to understand.
You are Mimo. Stay curious, stay thoughtful, and make learning delightful!
"""

@Composable
fun AiAssistantSidePanel(
    messages: androidx.compose.runtime.snapshots.SnapshotStateList<AiChatMessage>,
    noteTitle: String,
    subject: String,
    currentPage: Int,
    totalPages: Int,
    canvasTypeTitle: String,
    onClose: () -> Unit,
    onCapturePageSnapshot: suspend () -> Bitmap?,
    onExtractPageText: () -> String,
    onInsertToNote: (String) -> Unit,
    onSaveFlashcards: (List<GeneratedFlashcard>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = LocalIsDarkTheme.current
    val listState = rememberLazyListState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    var includePageSnapshot by remember { mutableStateOf(true) }
    var currentSnapshotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturingSnapshot by remember { mutableStateOf(false) }

    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    // Auto-scroll to latest message when messages list or active streaming content updates
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Initial greeting if chat is empty
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(
                AiChatMessage(
                    role = "model",
                    content = "Halo! Aku **Mimo**, teman belajar pintarmu di LearnSpace! 🎒✨\n\n" +
                            "Aku sudah terhubung dengan **Halaman $currentPage** dari *\"$noteTitle\"* ($subject).\n\n" +
                            "Ada konsep seru yang mau kita bedah bareng, rumus yang mau diuraikan, atau ada yang bikin penasaran? Tanyakan saja ya!",
                    pageNumber = currentPage
                )
            )
        }
    }

    // Capture/refresh page snapshot helper
    fun refreshSnapshot(onDone: ((Bitmap?) -> Unit)? = null) {
        isCapturingSnapshot = true
        coroutineScope.launch {
            val bmp = onCapturePageSnapshot()
            currentSnapshotBitmap = bmp
            isCapturingSnapshot = false
            onDone?.invoke(bmp)
        }
    }

    // Send query to Gemini with page vision context
    fun sendMessage(userText: String, overrideActionPrompt: String? = null) {
        val promptToSend = overrideActionPrompt ?: userText.trim()
        if (promptToSend.isBlank() || isGenerating) return

        if (!GeminiApiClient.isConfigured()) {
            messages.add(
                AiChatMessage(
                    role = "model",
                    content = "⚠️ **API Key Gemini belum diatur.**\n\nSilakan buka menu Pengaturan -> AI & Integrasi untuk memasukkan Gemini API Key gratis kamu agar AI Assistant dapat membaca halaman catatan ini.",
                    hasError = true
                )
            )
            return
        }

        // Append user message
        val displayText = if (overrideActionPrompt != null) userText else promptToSend
        messages.add(
            AiChatMessage(
                role = "user",
                content = displayText,
                pageNumber = if (includePageSnapshot) currentPage else null
            )
        )
        inputText = ""

        // Prepare placeholder model message
        val modelMessageId = UUID.randomUUID().toString()
        messages.add(
            AiChatMessage(
                id = modelMessageId,
                role = "model",
                content = "",
                pageNumber = currentPage,
                isStreaming = true
            )
        )

        isGenerating = true

        activeJob?.cancel()
        activeJob = coroutineScope.launch {
            try {
                // Ensure snapshot bitmap is ready if enabled
                var pageBmp = currentSnapshotBitmap
                if (includePageSnapshot && (pageBmp == null || pageBmp.isRecycled)) {
                    isCapturingSnapshot = true
                    pageBmp = onCapturePageSnapshot()
                    currentSnapshotBitmap = pageBmp
                    isCapturingSnapshot = false
                }

                // Build context prompt
                val pageContextText = onExtractPageText()
                val effectiveUserPrompt = buildString {
                    append("=== KONTEKS HALAMAN SAAT INI ===\n")
                    append(pageContextText)
                    append("\n=== PERTANYAAN/INSTRUKSI USER ===\n")
                    append(promptToSend)
                }

                // Build history turns for multi-turn conversation
                val history = messages.filter { it.id != modelMessageId && !it.hasError }.takeLast(8).map {
                    GeminiApiClient.ChatTurn(
                        role = if (it.role == "user") "user" else "model",
                        text = it.content
                    )
                }

                val imagesToSend = if (includePageSnapshot && pageBmp != null && !pageBmp.isRecycled) {
                    listOf(pageBmp)
                } else emptyList()

                val result = GeminiApiClient.streamChat(
                    systemPrompt = ASSISTANT_SYSTEM_PROMPT,
                    history = history,
                    newUserPrompt = effectiveUserPrompt,
                    newImages = imagesToSend,
                    thinkingBudget = 256,
                    maxOutputTokens = 8192
                ) { partialText ->
                    val idx = messages.indexOfFirst { it.id == modelMessageId }
                    if (idx != -1) {
                        val parsedCards = AiChatMessage.parseFlashcardsIfPresent(partialText)
                        messages[idx] = messages[idx].copy(
                            content = partialText,
                            isStreaming = true,
                            flashcards = parsedCards
                        )
                    }
                }

                val finalIdx = messages.indexOfFirst { it.id == modelMessageId }
                if (finalIdx != -1) {
                    if (result.isSuccess) {
                        val finalText = result.getOrNull() ?: messages[finalIdx].content
                        val parsedCards = AiChatMessage.parseFlashcardsIfPresent(finalText)
                        messages[finalIdx] = messages[finalIdx].copy(
                            content = finalText,
                            isStreaming = false,
                            flashcards = parsedCards
                        )
                    } else {
                        val err = result.exceptionOrNull()?.message ?: "Terjadi kesalahan saat menghubungi AI."
                        messages[finalIdx] = messages[finalIdx].copy(
                            content = "❌ $err",
                            isStreaming = false,
                            hasError = true
                        )
                    }
                }
            } catch (e: Exception) {
                val finalIdx = messages.indexOfFirst { it.id == modelMessageId }
                if (finalIdx != -1) {
                    messages[finalIdx] = messages[finalIdx].copy(
                        content = "❌ Gagal memproses permintaan: ${e.message}",
                        isStreaming = false,
                        hasError = true
                    )
                }
            } finally {
                isGenerating = false
            }
        }
    }

    Surface(
        modifier = modifier
            .width(380.dp)
            .fillMaxHeight()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)),
        color = LernspaceSurfaceCard,
        border = BorderStroke(1.dp, LernspaceBorder),
        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            // ── 1. HEADER ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Mimo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "AI Companion",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "📄 Hal $currentPage dari $totalPages · $subject",
                        fontSize = 11.5.sp,
                        color = LernspaceTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Refresh Page Snapshot Button
                IconButton(
                    onClick = { refreshSnapshot { Toast.makeText(context, "Tangkapan halaman diperbarui", Toast.LENGTH_SHORT).show() } },
                    modifier = Modifier.size(32.dp),
                    enabled = !isGenerating && !isCapturingSnapshot
                ) {
                    if (isCapturingSnapshot) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF8B5CF6))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Perbarui Tangkapan Halaman",
                            tint = LernspaceTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Clear Chat History Button
                IconButton(
                    onClick = {
                        messages.clear()
                        messages.add(
                            AiChatMessage(
                                role = "model",
                                content = "Percakapan sudah direset. Ada materi atau konsep lain di halaman ini yang ingin kita bahas bareng? ✨",
                                pageNumber = currentPage
                            )
                        )
                    },
                    modifier = Modifier.size(32.dp),
                    enabled = !isGenerating
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Reset Chat",
                        tint = LernspaceTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close Side Panel Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup Panel",
                        tint = LernspaceTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = LernspaceBorder, thickness = 1.dp)

            // ── 2. PAGE CONTEXT PILL & TOGGLE ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkTheme) Color(0xFF181822) else Color(0xFFF8FAFC))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (includePageSnapshot) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = if (includePageSnapshot) Color(0xFF8B5CF6) else LernspaceTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (includePageSnapshot) "Membaca snapshot visual Halaman $currentPage" else "Hanya membaca teks biasa",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (includePageSnapshot) Color(0xFF8B5CF6) else LernspaceTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Visual",
                        fontSize = 11.sp,
                        color = LernspaceTextSecondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = includePageSnapshot,
                        onCheckedChange = { includePageSnapshot = it },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            HorizontalDivider(color = LernspaceBorder, thickness = 1.dp)

            // ── 3. QUICK ACTION CHIPS ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiQuickAction.entries.forEach { action ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LernspaceSurfaceLow,
                        border = BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier.clickable(enabled = !isGenerating) {
                            sendMessage(action.title, action.defaultPrompt)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = action.emoji, fontSize = 12.sp)
                            Text(
                                text = action.title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceTextPrimary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = LernspaceBorder, thickness = 1.dp)

            // ── 4. CHAT MESSAGES LIST ──
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        message = msg,
                        isDarkTheme = isDarkTheme,
                        onCopy = {
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Assistant", msg.displayContent))
                            Toast.makeText(context, "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onInsertToNote = { onInsertToNote(msg.displayContent) },
                        onSaveFlashcards = { cards ->
                            onSaveFlashcards(cards)
                            Toast.makeText(context, "${cards.size} flashcards disimpan ke Deck!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // ── 5. BOTTOM INPUT BAR ──
            Surface(
                color = LernspaceSurfaceCard,
                border = BorderStroke(1.dp, LernspaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Tanya Mimo tentang halaman $currentPage...",
                                fontSize = 12.5.sp,
                                color = LernspaceTextSecondary
                            )
                        },
                        maxLines = 4,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.5.sp,
                            color = LernspaceTextPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = LernspaceBorder,
                            focusedContainerColor = LernspaceSurfaceLow,
                            unfocusedContainerColor = LernspaceSurfaceLow
                        ),
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputText = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Hapus teks",
                                        tint = LernspaceTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isGenerating) {
                                    sendMessage(inputText)
                                }
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (isGenerating) {
                        FilledIconButton(
                            onClick = {
                                activeJob?.cancel()
                                isGenerating = false
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Hentikan",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        FilledIconButton(
                            onClick = { sendMessage(inputText) },
                            enabled = inputText.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF8B5CF6),
                                disabledContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Kirim",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: AiChatMessage,
    isDarkTheme: Boolean,
    onCopy: () -> Unit,
    onInsertToNote: () -> Unit,
    onSaveFlashcards: (List<GeneratedFlashcard>) -> Unit
) {
    val isUser = message.role == "user"

    if (isUser) {
        // User Message Bubble (Right aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp).copy(bottomEnd = androidx.compose.foundation.shape.CornerSize(3.dp)),
                color = LernspaceDark,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.pageNumber != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Halaman ${message.pageNumber}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Text(
                        text = message.content,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    } else {
        // Assistant Message Card (Left aligned, rich markdown/LaTeX)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, if (message.hasError) Color(0xFFFCA5A5) else LernspaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Assistant Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Mimo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5CF6)
                        )
                    }

                    if (message.isStreaming) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = Color(0xFF8B5CF6)
                            )
                            Text(
                                text = "Menulis...",
                                fontSize = 10.sp,
                                color = LernspaceTextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Content (Markdown + KaTeX LaTeX)
                val cleanText = message.displayContent
                if (cleanText.isNotBlank()) {
                    MarkdownLatexContent(
                        markdown = cleanText,
                        baseFontSize = 13.sp,
                        textColor = LernspaceTextPrimary,
                        accentColor = Color(0xFF8B5CF6),
                        isDarkSurface = isDarkTheme,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (message.isStreaming) {
                    Text(
                        text = "Menganalisis halaman dan menyiapkan jawaban...",
                        fontSize = 12.5.sp,
                        color = LernspaceTextSecondary
                    )
                }

                // Action Bar (Copy, Insert to Note, Save Flashcards)
                if (!message.isStreaming && (cleanText.isNotBlank() || !message.flashcards.isNullOrEmpty()) && !message.hasError) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.5f))
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Copy Button
                        OutlinedButton(
                            onClick = onCopy,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LernspaceBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = LernspaceTextSecondary)
                            Spacer(Modifier.width(4.dp))
                            Text("Salin", fontSize = 11.sp, color = LernspaceTextSecondary)
                        }

                        // 2. Insert as Note Text Box
                        OutlinedButton(
                            onClick = onInsertToNote,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LernspaceBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Filled.PostAdd, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF8B5CF6))
                            Spacer(Modifier.width(4.dp))
                            Text("Tempel ke Catatan", fontSize = 11.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.SemiBold)
                        }

                        // 3. Save Flashcards Button (if flashcards detected)
                        val cards = message.flashcards
                        if (!cards.isNullOrEmpty()) {
                            Button(
                                onClick = { onSaveFlashcards(cards) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Filled.Style, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("Simpan ${cards.size} Flashcard", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
