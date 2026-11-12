package com.lernspace.app.ui.quiz

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.ai.GeminiApiClient
import com.lernspace.app.core.ai.GeneratedQuizQuestion
import com.lernspace.app.core.ai.HandwrittenNoteRecognizer
import com.lernspace.app.core.document.DocumentTextExtractor
import com.lernspace.app.core.document.ExtractedDocumentResult
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.components.MarkdownLatexContent
import kotlinx.coroutines.launch

@Composable
fun QuizScreen() {
    val context = LocalContext.current
    val topics = AcademicWorkspaceState.topics
    val notes = AcademicWorkspaceState.notes
    val coroutineScope = rememberCoroutineScope()

    var sourceMode by remember { mutableIntStateOf(if (notes.isNotEmpty()) 0 else 2) } // 0=Note, 1=Topic, 2=Manual/Doc
    var selectedNote by remember(notes) { mutableStateOf(notes.firstOrNull()) }
    var selectedTopic by remember(topics) { mutableStateOf(topics.firstOrNull()) }
    var manualMaterialText by remember { mutableStateOf("") }
    var questionCount by remember { mutableIntStateOf(5) }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var attachedDocument by remember { mutableStateOf<ExtractedDocumentResult?>(null) }
    var isExtractingDoc by remember { mutableStateOf(false) }
    var docExtractionStatus by remember { mutableStateOf("") }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        uri?.let { pickedUri ->
            isExtractingDoc = true
            docExtractionStatus = "Membuka dokumen..."
            errorMsg = null
            coroutineScope.launch {
                val res = DocumentTextExtractor.extractTextFromUri(
                    context = context,
                    uri = pickedUri,
                    onProgress = { cur, tot ->
                        docExtractionStatus = "Membaca halaman $cur dari $tot..."
                    }
                )
                res.onSuccess { doc ->
                    attachedDocument = doc
                    manualMaterialText = doc.text
                    isExtractingDoc = false
                }.onFailure { err ->
                    isExtractingDoc = false
                    errorMsg = "Gagal membaca dokumen: ${err.message}"
                }
            }
        }
    }

    var isRecognizingInk by remember { mutableStateOf(false) }
    var recognizedHandwritingText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // Active quiz session state
    var quizQuestions by remember { mutableStateOf<List<GeneratedQuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var activeQuizTitle by remember { mutableStateOf("") }

    val activeSourcePages = remember(sourceMode, selectedNote, selectedTopic, notes) {
        when (sourceMode) {
            0 -> selectedNote?.pages ?: emptyList()
            1 -> notes.filter { it.topicId == selectedTopic?.id }.flatMap { it.pages }
            else -> emptyList()
        }
    }
    val activeStrokeCount = remember(activeSourcePages) {
        HandwrittenNoteRecognizer.countHandwrittenStrokes(activeSourcePages)
    }

    LaunchedEffect(sourceMode, selectedNote?.id, selectedTopic?.id) {
        recognizedHandwritingText = ""
        errorMsg = null
        if (sourceMode != 2 && activeStrokeCount > 0) {
            isRecognizingInk = true
            val cacheId = if (sourceMode == 0) selectedNote?.id ?: "note" else "topic_${selectedTopic?.id ?: ""}"
            val titleForVision = if (sourceMode == 0) selectedNote?.title ?: "Catatan" else selectedTopic?.title ?: "Sub-bab"
            try {
                val offlineRes = HandwrittenNoteRecognizer.recognizePagesOffline(cacheId, activeSourcePages)
                if (GeminiApiClient.isConfigured()) {
                    val visionRes = HandwrittenNoteRecognizer.recognizePagesWithGeminiVision(
                        cacheKeyId = cacheId,
                        pages = activeSourcePages,
                        topicTitle = titleForVision
                    )
                    visionRes.onSuccess { vr ->
                        recognizedHandwritingText = vr.text
                    }.onFailure {
                        recognizedHandwritingText = offlineRes.text
                    }
                } else {
                    recognizedHandwritingText = offlineRes.text
                }
            } finally {
                isRecognizingInk = false
            }
        } else {
            isRecognizingInk = false
        }
    }

    val effectiveSourceText = when (sourceMode) {
        0 -> {
            val base = selectedNote?.pages
                ?.flatMap { it.textBoxes }
                ?.map { it.text.trim() }
                ?.filter { it.isNotBlank() }
                ?.joinToString("\n")
                .orEmpty()
            listOf(base, recognizedHandwritingText).filter { it.isNotBlank() }.joinToString("\n\n")
        }
        1 -> {
            val topicNotes = notes.filter { it.topicId == selectedTopic?.id }
            val base = topicNotes.joinToString("\n") { n ->
                val tbText = n.pages.flatMap { it.textBoxes }.map { it.text.trim() }.filter { it.isNotBlank() }.joinToString("\n")
                if (tbText.isNotBlank()) "${n.title}\n$tbText" else ""
            }.trim()
            listOf(base, recognizedHandwritingText).filter { it.isNotBlank() }.joinToString("\n\n")
        }
        else -> {
            val docText = attachedDocument?.text.orEmpty()
            val manual = manualMaterialText.trim()
            when {
                docText.isNotBlank() && manual.isNotBlank() && docText != manual ->
                    "$docText\n\nCatatan Tambahan:\n$manual"
                manual.isNotBlank() -> manual
                else -> docText
            }
        }
    }

    val effectiveTopicTitle = when (sourceMode) {
        0 -> selectedNote?.title ?: "Catatan"
        1 -> selectedTopic?.title ?: "Sub-bab"
        else -> attachedDocument?.fileName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Materi Kuis"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LernspaceSurface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Kuis AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = LernspaceDark
                )
                Text(
                    text = "Latihan pilihan ganda otomatis dari catatan, sub-bab, atau materi teks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LernspaceTextSecondary
                )
            }
            if (quizQuestions.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        quizQuestions = emptyList()
                        currentIndex = 0
                        selectedOptionIdx = null
                        correctCount = 0
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Icon(Icons.Filled.Refresh, null, tint = LernspaceDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Kuis Baru", color = LernspaceDark, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 2-Column Tablet Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Source & Generator Config
            Surface(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = LernspaceSurfaceCard,
                border = BorderStroke(1.dp, LernspaceBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sumber Materi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Catatan", 1 to "Sub-bab", 2 to "Manual / Dokumen").forEach { (idx, label) ->
                            FilterChip(
                                selected = sourceMode == idx,
                                onClick = { sourceMode = idx },
                                label = { Text(label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    when (sourceMode) {
                        0 -> {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, LernspaceBorder)
                                ) {
                                    Text(
                                        text = selectedNote?.title ?: "Pilih catatan...",
                                        modifier = Modifier.weight(1f),
                                        color = LernspaceTextPrimary
                                    )
                                    Icon(Icons.Filled.ArrowDropDown, null, tint = LernspaceTextSecondary)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    notes.forEach { n ->
                                        DropdownMenuItem(
                                            text = { Text("${n.title} (${n.subject})") },
                                            onClick = { selectedNote = n; expanded = false }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, LernspaceBorder)
                                ) {
                                    Text(
                                        text = selectedTopic?.title ?: "Pilih sub-bab...",
                                        modifier = Modifier.weight(1f),
                                        color = LernspaceTextPrimary
                                    )
                                    Icon(Icons.Filled.ArrowDropDown, null, tint = LernspaceTextSecondary)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    topics.forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t.title) },
                                            onClick = { selectedTopic = t; expanded = false }
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (attachedDocument == null) {
                                    OutlinedButton(
                                        onClick = {
                                            documentPickerLauncher.launch(
                                                arrayOf(
                                                    "application/pdf",
                                                    "text/plain",
                                                    "text/markdown",
                                                    "image/*",
                                                    "*/*"
                                                )
                                            )
                                        },
                                        enabled = !isExtractingDoc,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, LernspaceBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.UploadFile, null, tint = LernspaceDark, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pilih Dokumen (PDF, Teks, Gambar)",
                                            color = LernspaceDark,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LernspaceSurfaceLow,
                                        border = BorderStroke(1.dp, LernspaceBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val icon = when {
                                                attachedDocument!!.fileName.endsWith(".pdf", ignoreCase = true) -> Icons.Filled.PictureAsPdf
                                                attachedDocument!!.mimeType.startsWith("image/") -> Icons.Filled.Image
                                                else -> Icons.Filled.Description
                                            }
                                            Icon(icon, null, tint = LernspaceDark, modifier = Modifier.size(24.dp))
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = attachedDocument!!.fileName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LernspaceDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (attachedDocument!!.fileName.endsWith(".pdf", ignoreCase = true)) {
                                                        "${attachedDocument!!.pageCount} halaman • ${attachedDocument!!.wordCount} kata dimuat"
                                                    } else {
                                                        "${attachedDocument!!.wordCount} kata dimuat"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = LernspaceTextSecondary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    attachedDocument = null
                                                    manualMaterialText = ""
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.Close, "Hapus dokumen", tint = LernspaceTextSecondary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                if (isExtractingDoc) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = LernspaceDark)
                                        Text(docExtractionStatus, fontSize = 12.sp, color = LernspaceTextSecondary)
                                    }
                                }

                                OutlinedTextField(
                                    value = manualMaterialText,
                                    onValueChange = { manualMaterialText = it },
                                    label = {
                                        Text(
                                            if (attachedDocument != null) "Teks Materi (dari dokumen, bisa diedit)"
                                            else "Teks Materi Kuis"
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            if (attachedDocument != null) "Teks dari dokumen berhasil dimuat. Bisa tambah atau edit di sini..."
                                            else "Tempel materi kuliah di sini atau pilih dokumen (PDF/Teks/Gambar) di atas..."
                                        )
                                    },
                                    minLines = if (attachedDocument != null) 4 else 5,
                                    maxLines = 8,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Clean status indicator (tanpa preview teks hasil bacaan AI)
                    val isReady = when (sourceMode) {
                        0, 1 -> !isRecognizingInk && effectiveSourceText.trim().length >= 15
                        else -> !isExtractingDoc && effectiveSourceText.trim().length >= 15
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LernspaceSurfaceLow,
                        border = BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isRecognizingInk || isExtractingDoc) {
                                CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = LernspaceDark)
                            } else {
                                Icon(
                                    imageVector = if (isReady) Icons.Filled.CheckCircle else Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = if (isReady) LernspaceDark else LernspaceTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = when {
                                    isRecognizingInk -> "AI sedang membaca catatan..."
                                    isExtractingDoc -> docExtractionStatus
                                    sourceMode == 2 && attachedDocument != null -> "Dokumen \"${attachedDocument!!.fileName}\" siap dijadikan kuis"
                                    sourceMode == 2 && isReady -> "Teks materi siap dijadikan kuis"
                                    sourceMode == 2 -> "Ketik materi atau pilih dokumen untuk dijadikan kuis"
                                    recognizedHandwritingText.isNotBlank() -> "AI sudah selesai membaca catatan"
                                    isReady -> "Catatan teks siap dijadikan kuis"
                                    else -> "Catatan belum memiliki cukup teks atau coretan"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isReady) FontWeight.Medium else FontWeight.Normal,
                                color = if (isReady) LernspaceTextPrimary else LernspaceTextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Jumlah Soal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 8, 10).forEach { count ->
                                FilterChip(
                                    selected = questionCount == count,
                                    onClick = { questionCount = count },
                                    label = { Text("$count", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    if (errorMsg != null) {
                        Text(errorMsg!!, fontSize = 12.sp, color = AccentRed)
                    }

                    Spacer(modifier = Modifier.weight(1f, fill = false))

                    Button(
                        enabled = !isGenerating && !isRecognizingInk && !isExtractingDoc && effectiveSourceText.trim().length >= 15,
                        onClick = {
                            if (!GeminiApiClient.isConfigured()) {
                                errorMsg = "Gemini API Key belum diatur di Pengaturan."
                                return@Button
                            }
                            errorMsg = null
                            isGenerating = true
                            coroutineScope.launch {
                                val res = GeminiApiClient.generateQuizQuestions(
                                    sourceText = effectiveSourceText,
                                    topicTitle = effectiveTopicTitle,
                                    maxQuestions = questionCount
                                )
                                isGenerating = false
                                res.onSuccess { questions ->
                                    quizQuestions = questions
                                    activeQuizTitle = effectiveTopicTitle
                                    currentIndex = 0
                                    selectedOptionIdx = null
                                    correctCount = 0
                                }.onFailure { err ->
                                    errorMsg = err.message ?: "Gagal membuat kuis."
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LernspaceDark,
                            contentColor = LernspaceSurfaceCard
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LernspaceSurfaceCard)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Membuat Kuis AI...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mulai Kuis AI", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Right Column: Interactive Quiz Viewport
            Surface(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = LernspaceSurfaceCard,
                border = BorderStroke(1.dp, LernspaceBorder),
                shadowElevation = 1.dp
            ) {
                when {
                    quizQuestions.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Quiz, null, tint = LernspaceTextSecondary, modifier = Modifier.size(44.dp))
                                Text(
                                    text = "Belum Ada Kuis Aktif",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark
                                )
                                Text(
                                    text = "Pilih catatan atau sub-bab di panel kiri, lalu tekan \"Mulai Kuis AI\" untuk berlatih.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LernspaceTextSecondary
                                )
                            }
                        }
                    }
                    currentIndex < quizQuestions.size -> {
                        val q = quizQuestions[currentIndex]
                        val isAnswered = selectedOptionIdx != null
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeQuizTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LernspaceTextSecondary
                                )
                                Text(
                                    text = "Soal ${currentIndex + 1} / ${quizQuestions.size}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (currentIndex + 1).toFloat() / quizQuestions.size.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = LernspaceDark,
                                trackColor = LernspaceSurfaceLow
                            )

                            MarkdownLatexContent(
                                markdown = q.question,
                                baseFontSize = 16.sp,
                                textColor = LernspaceTextPrimary,
                                accentColor = LernspaceDark,
                                isDarkSurface = LocalIsDarkTheme.current,
                                modifier = Modifier.fillMaxWidth()
                            )

                            val letters = listOf("A", "B", "C", "D")
                            q.options.forEachIndexed { idx, optText ->
                                val isChosen = selectedOptionIdx == idx
                                val isCorrectOpt = idx == q.correctIndex
                                val bgColor = when {
                                    !isAnswered -> LernspaceSurfaceLow
                                    isCorrectOpt -> LernspaceDark
                                    isChosen -> LernspaceSurfaceCard
                                    else -> LernspaceSurfaceLow
                                }
                                val fgColor = when {
                                    !isAnswered -> LernspaceTextPrimary
                                    isCorrectOpt -> LernspaceSurfaceCard
                                    else -> LernspaceTextSecondary
                                }
                                val borderCol = when {
                                    !isAnswered -> LernspaceBorder
                                    isCorrectOpt || isChosen -> LernspaceDark
                                    else -> LernspaceBorder
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = bgColor,
                                    border = BorderStroke(if (isChosen || (isAnswered && isCorrectOpt)) 1.5.dp else 1.dp, borderCol),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isAnswered) {
                                            selectedOptionIdx = idx
                                            if (idx == q.correctIndex) correctCount++
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "${letters.getOrElse(idx) { "${idx + 1}" }}.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = fgColor
                                        )
                                        Box(modifier = Modifier.weight(1f)) {
                                            MarkdownLatexContent(
                                                markdown = optText,
                                                baseFontSize = 14.sp,
                                                textColor = fgColor,
                                                accentColor = fgColor,
                                                isDarkSurface = if (isAnswered && isCorrectOpt) !LocalIsDarkTheme.current else LocalIsDarkTheme.current,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        if (isAnswered && isCorrectOpt) {
                                            Icon(Icons.Filled.CheckCircle, null, tint = fgColor, modifier = Modifier.size(18.dp))
                                        } else if (isAnswered && isChosen) {
                                            Icon(Icons.Filled.Cancel, null, tint = LernspaceTextPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            if (isAnswered && q.explanation.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LernspaceSurfaceLow,
                                    border = BorderStroke(1.dp, LernspaceBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (selectedOptionIdx == q.correctIndex) "Benar" else "Kurang Tepat",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        MarkdownLatexContent(
                                            markdown = q.explanation,
                                            baseFontSize = 13.sp,
                                            textColor = LernspaceTextSecondary,
                                            accentColor = LernspaceDark,
                                            isDarkSurface = LocalIsDarkTheme.current,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            if (isAnswered) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button(
                                        onClick = {
                                            selectedOptionIdx = null
                                            currentIndex++
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LernspaceDark,
                                            contentColor = LernspaceSurfaceCard
                                        )
                                    ) {
                                        Text(
                                            text = if (currentIndex == quizQuestions.lastIndex) "Lihat Skor" else "Soal Berikutnya",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val pct = (correctCount * 100) / quizQuestions.size.coerceAtLeast(1)
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, tint = LernspaceDark, modifier = Modifier.size(48.dp))
                                Text("Kuis Selesai", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                Text(
                                    text = "$correctCount / ${quizQuestions.size} Benar ($pct%)",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LernspaceDark
                                )
                                Text(
                                    text = "Topik: $activeQuizTitle",
                                    fontSize = 13.sp,
                                    color = LernspaceTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        currentIndex = 0
                                        selectedOptionIdx = null
                                        correctCount = 0
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, LernspaceBorder)
                                ) {
                                    Text("Ulangi Kuis", color = LernspaceDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
