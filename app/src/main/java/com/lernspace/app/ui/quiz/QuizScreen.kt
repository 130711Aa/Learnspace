package com.lernspace.app.ui.quiz

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
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
import com.lernspace.app.core.supabase.QuizSubmission
import com.lernspace.app.core.supabase.QuizSubmissionAnswer
import com.lernspace.app.core.supabase.SupabaseClient
import com.lernspace.app.core.supabase.SupabaseQuestion
import com.lernspace.app.data.local.NoteStorageManager
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.components.MarkdownLatexContent
import kotlinx.coroutines.launch

@Composable
fun QuizScreen() {
    val context = LocalContext.current
    val subjects = AcademicWorkspaceState.subjects
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
    var noteExtractedText by remember { mutableStateOf("") }
    var isExtractingNoteText by remember { mutableStateOf(false) }

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
    var generationStatusText by remember { mutableStateOf("Membuat Kuis AI...") }

    // Active quiz session state
    var quizQuestions by remember { mutableStateOf<List<GeneratedQuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var activeQuizTitle by remember { mutableStateOf("") }

    // Supabase Cloud Bank Soal state
    var cloudQuestions by remember { mutableStateOf<List<SupabaseQuestion>>(emptyList()) }
    var isLoadingCloudQuestions by remember { mutableStateOf(false) }
    var cloudSubjectFilter by remember { mutableStateOf("Semua") }
    var cloudDifficultyFilter by remember { mutableStateOf("Semua") }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var refreshCloudTrigger by remember { mutableIntStateOf(0) }
    var isSubmittingToCloud by remember { mutableStateOf(false) }
    var cloudSubmissionStatus by remember { mutableStateOf<String?>(null) }
    var activeCloudQuestionIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeRecordedAnswers by remember { mutableStateOf<MutableList<QuizSubmissionAnswer>>(mutableListOf()) }
    var quizStartTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sourceMode, cloudSubjectFilter, cloudDifficultyFilter, refreshCloudTrigger) {
        if (sourceMode == 3) {
            isLoadingCloudQuestions = true
            errorMsg = null
            val res = SupabaseClient.fetchQuestions(
                subjectName = if (cloudSubjectFilter == "Semua") null else cloudSubjectFilter,
                difficulty = if (cloudDifficultyFilter == "Semua") null else cloudDifficultyFilter
            )
            res.onSuccess { list ->
                cloudQuestions = list
                isLoadingCloudQuestions = false
            }.onFailure { err ->
                errorMsg = "Gagal memuat dari Supabase: ${err.message}"
                isLoadingCloudQuestions = false
            }
        }
    }

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

    // Auto-extract note text if not already cached in NoteStorageManager
    LaunchedEffect(selectedNote?.id, selectedNote?.pdfUri) {
        val note = selectedNote ?: run {
            noteExtractedText = ""
            return@LaunchedEffect
        }
        val tbText = note.pages
            .flatMap { it.textBoxes }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
        if (tbText.isNotBlank()) {
            noteExtractedText = tbText
            return@LaunchedEffect
        }

        val cached = NoteStorageManager.getExtractedText(context, note.id)
        if (!cached.isNullOrBlank()) {
            noteExtractedText = cached
            return@LaunchedEffect
        }

        val pdfUriStr = note.pdfUri
        if (!pdfUriStr.isNullOrBlank()) {
            isExtractingNoteText = true
            docExtractionStatus = "Mengekstrak teks dokumen catatan..."
            try {
                val uri = Uri.parse(pdfUriStr)
                val res = DocumentTextExtractor.extractTextFromUri(
                    context = context,
                    uri = uri,
                    onProgress = { cur, tot ->
                        docExtractionStatus = "Membaca catatan (hal $cur/$tot)..."
                    }
                )
                res.onSuccess { doc ->
                    if (doc.text.isNotBlank()) {
                        noteExtractedText = doc.text
                        NoteStorageManager.saveExtractedText(context, note.id, doc.text)
                    }
                }
            } catch (_: Exception) {} finally {
                isExtractingNoteText = false
            }
        } else {
            noteExtractedText = ""
        }
    }

    // 0-Token Offline Preview: Hanya membaca offline lokal (ML Kit / Cache) untuk cek kesiapan bahan,
    // TIDAK memanggil Gemini Vision di latar belakang agar hemat token dan bebas rate limit.
    LaunchedEffect(sourceMode, selectedNote?.id, selectedTopic?.id) {
        recognizedHandwritingText = ""
        errorMsg = null
        if (sourceMode != 2 && activeStrokeCount > 0) {
            isRecognizingInk = true
            val cacheId = if (sourceMode == 0) selectedNote?.id ?: "note" else "topic_${selectedTopic?.id ?: ""}"
            try {
                val offlineRes = HandwrittenNoteRecognizer.recognizePagesOffline(cacheId, activeSourcePages)
                recognizedHandwritingText = offlineRes.text
            } catch (_: Exception) {
                // Abaikan jika model lokal belum selesai diunduh
            } finally {
                isRecognizingInk = false
            }
        } else {
            isRecognizingInk = false
        }
    }

    val effectiveSourceText = when (sourceMode) {
        0 -> {
            val base = noteExtractedText.ifBlank {
                selectedNote?.let { NoteStorageManager.getExtractedText(context, it.id).orEmpty() } ?: ""
            }
            listOf(base, recognizedHandwritingText).filter { it.isNotBlank() }.joinToString("\n\n")
        }
        1 -> {
            val topicNotes = notes.filter { it.topicId == selectedTopic?.id }
            val base = topicNotes.joinToString("\n\n") { n ->
                val tbText = n.pages.flatMap { it.textBoxes }.map { it.text.trim() }.filter { it.isNotBlank() }.joinToString("\n")
                val text = if (tbText.isNotBlank()) tbText else NoteStorageManager.getExtractedText(context, n.id).orEmpty()
                if (text.isNotBlank()) "${n.title}\n$text" else ""
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            0 to "Catatan",
                            1 to "Sub-bab",
                            2 to "Dokumen",
                            3 to "Bank Soal Cloud"
                        ).forEach { (idx, label) ->
                            FilterChip(
                                selected = sourceMode == idx,
                                onClick = {
                                    sourceMode = idx
                                    errorMsg = null
                                },
                                label = { Text(label, fontSize = 11.sp) },
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
                                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                    "application/msword",
                                                    "application/vnd.ms-powerpoint",
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
                                            text = "Pilih Dokumen (PDF, Word, PPTX, Teks, Gambar)",
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
                        3 -> {
                            // Bank Soal Cloud Supabase Config View
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Filter Bank Soal:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LernspaceTextSecondary
                                    )
                                    FilledTonalButton(
                                        onClick = { showAddQuestionDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tambah Soal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                val subjects = AcademicWorkspaceState.subjects
                                val allSubjects = remember(subjects, cloudQuestions) {
                                    val list = mutableListOf("Semua")
                                    list.addAll(cloudQuestions.map { it.subjectName }.distinct())
                                    subjects.forEach { s -> if (!list.contains(s.name)) list.add(s.name) }
                                    list
                                }
                                var subjDropdownOpen by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { subjDropdownOpen = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, LernspaceBorder)
                                    ) {
                                        Text(
                                            text = "Mata Kuliah: $cloudSubjectFilter",
                                            modifier = Modifier.weight(1f),
                                            color = LernspaceTextPrimary,
                                            fontSize = 12.sp
                                        )
                                        Icon(Icons.Filled.ArrowDropDown, null, tint = LernspaceTextSecondary)
                                    }
                                    DropdownMenu(expanded = subjDropdownOpen, onDismissRequest = { subjDropdownOpen = false }) {
                                        allSubjects.forEach { sName ->
                                            DropdownMenuItem(
                                                text = { Text(sName, fontSize = 13.sp) },
                                                onClick = {
                                                    cloudSubjectFilter = sName
                                                    subjDropdownOpen = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Semua", "EASY", "MEDIUM", "HARD").forEach { diff ->
                                        FilterChip(
                                            selected = cloudDifficultyFilter == diff,
                                            onClick = { cloudDifficultyFilter = diff },
                                            label = {
                                                Text(
                                                    when (diff) {
                                                        "EASY" -> "Mudah"
                                                        "MEDIUM" -> "Sedang"
                                                        "HARD" -> "Sulit"
                                                        else -> "Semua"
                                                    },
                                                    fontSize = 11.sp
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                if (isLoadingCloudQuestions) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = LernspaceDark)
                                    }
                                } else if (cloudQuestions.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LernspaceSurfaceLow,
                                        border = BorderStroke(1.dp, LernspaceBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Belum ada soal dengan filter ini di Cloud.", fontSize = 12.sp, color = LernspaceTextSecondary)
                                            TextButton(onClick = { showAddQuestionDialog = true }) {
                                                Text("+ Buat Soal Pertama", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "${cloudQuestions.size} soal siap diujikan dari Cloud Supabase",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LernspaceDark
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        cloudQuestions.take(4).forEachIndexed { idx, q ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = LernspaceSurfaceLow,
                                                border = BorderStroke(1.dp, LernspaceBorder),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(LernspaceDark),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = q.topicTitle,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = LernspaceDark,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = q.questionText,
                                                            fontSize = 11.sp,
                                                            color = LernspaceTextSecondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = when (q.difficulty) {
                                                            "EASY" -> Color(0xFFDCFCE7)
                                                            "HARD" -> Color(0xFFFEE2E2)
                                                            else -> Color(0xFFFEF3C7)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = q.difficulty,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when (q.difficulty) {
                                                                "EASY" -> Color(0xFF166534)
                                                                "HARD" -> Color(0xFF991B1B)
                                                                else -> Color(0xFF92400E)
                                                            },
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (cloudQuestions.size > 4) {
                                            Text(
                                                text = "+ ${cloudQuestions.size - 4} soal lainnya",
                                                fontSize = 11.sp,
                                                color = LernspaceTextSecondary,
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val isAnyExtracting = isExtractingDoc || isExtractingNoteText
                    val hasEnoughBaseContent = when (sourceMode) {
                        0, 1 -> activeStrokeCount > 0 || effectiveSourceText.trim().length >= 15
                        3 -> cloudQuestions.isNotEmpty()
                        else -> !isAnyExtracting && effectiveSourceText.trim().length >= 15
                    }
                    val isReady = if (sourceMode == 3) {
                        !isLoadingCloudQuestions && cloudQuestions.isNotEmpty()
                    } else {
                        !isRecognizingInk && !isAnyExtracting && hasEnoughBaseContent
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
                            if (isRecognizingInk || isAnyExtracting || (sourceMode == 3 && isLoadingCloudQuestions)) {
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
                                    sourceMode == 3 && isLoadingCloudQuestions -> "Memuat bank soal dari Supabase..."
                                    sourceMode == 3 && isReady -> "${cloudQuestions.size} soal siap diujikan dari Cloud Supabase"
                                    sourceMode == 3 -> "Belum ada soal dengan filter ini di Cloud"
                                    isRecognizingInk -> "Memeriksa coretan catatan (offline)..."
                                    isAnyExtracting -> docExtractionStatus
                                    sourceMode == 2 && attachedDocument != null -> "Dokumen \"${attachedDocument!!.fileName}\" siap dijadikan kuis"
                                    sourceMode == 2 && isReady -> "Teks materi siap dijadikan kuis"
                                    sourceMode == 2 -> "Ketik materi atau pilih dokumen untuk dijadikan kuis"
                                    activeStrokeCount > 0 && effectiveSourceText.trim().length >= 15 -> "Catatan teks & coretan siap dianalisis AI"
                                    activeStrokeCount > 0 -> "Coretan tulisan tangan siap dianalisis AI"
                                    isReady -> "Catatan teks siap dijadikan kuis"
                                    else -> "Catatan belum memiliki teks atau coretan"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isReady) FontWeight.Medium else FontWeight.Normal,
                                color = if (isReady) LernspaceTextPrimary else LernspaceTextSecondary
                            )
                        }
                    }

                    if (sourceMode != 3) {
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
                    }

                    if (errorMsg != null) {
                        Text(errorMsg!!, fontSize = 12.sp, color = AccentRed)
                    }

                    Spacer(modifier = Modifier.weight(1f, fill = false))

                    Button(
                        enabled = if (sourceMode == 3) {
                            !isLoadingCloudQuestions && cloudQuestions.isNotEmpty()
                        } else {
                            !isGenerating && !isRecognizingInk && !isExtractingDoc && hasEnoughBaseContent
                        },
                        onClick = {
                            if (sourceMode == 3) {
                                val questionsToRun = cloudQuestions.map { q ->
                                    val opts = q.options.map { it.optionText }
                                    val correctIdx = q.options.indexOfFirst { it.isCorrect }.let { if (it >= 0) it else 0 }
                                    GeneratedQuizQuestion(
                                        question = q.questionText,
                                        options = opts,
                                        correctIndex = correctIdx,
                                        explanation = q.explanation ?: ""
                                    )
                                }
                                quizQuestions = questionsToRun
                                activeCloudQuestionIds = cloudQuestions.map { it.id }
                                activeRecordedAnswers = mutableListOf()
                                quizStartTime = System.currentTimeMillis()
                                activeQuizTitle = "Bank Soal: " + (if (cloudSubjectFilter != "Semua") cloudSubjectFilter else "Semua Materi")
                                currentIndex = 0
                                selectedOptionIdx = null
                                correctCount = 0
                                cloudSubmissionStatus = null
                                return@Button
                            }

                            if (!GeminiApiClient.isConfigured()) {
                                errorMsg = "Gemini API Key belum diatur di Pengaturan."
                                return@Button
                            }
                            errorMsg = null
                            isGenerating = true
                            generationStatusText = "Mempersiapkan materi..."
                            coroutineScope.launch {
                                try {
                                    var finalText = effectiveSourceText.trim()

                                    // Jika catatan memiliki coretan tulisan tangan dan teks hasil pengenalan belum ada,
                                    // panggil Gemini Vision secara on-demand HANYA saat tombol ini diklik!
                                    if (sourceMode != 2 && activeStrokeCount > 0 && recognizedHandwritingText.isBlank()) {
                                        generationStatusText = "AI membaca coretan catatan..."
                                        val cacheId = if (sourceMode == 0) selectedNote?.id ?: "note" else "topic_${selectedTopic?.id ?: ""}"
                                        val titleForVision = if (sourceMode == 0) selectedNote?.title ?: "Catatan" else selectedTopic?.title ?: "Sub-bab"
                                        val visionRes = HandwrittenNoteRecognizer.recognizePagesWithGeminiVision(
                                            cacheKeyId = cacheId,
                                            pages = activeSourcePages,
                                            topicTitle = titleForVision
                                        )
                                        visionRes.onSuccess { vr ->
                                            recognizedHandwritingText = vr.text
                                            finalText = listOf(finalText, vr.text).filter { it.isNotBlank() }.joinToString("\n\n")
                                        }.onFailure {
                                            // Fallback ke offline text jika ada
                                            val off = HandwrittenNoteRecognizer.recognizePagesOffline(cacheId, activeSourcePages)
                                            if (off.text.isNotBlank()) {
                                                recognizedHandwritingText = off.text
                                                finalText = listOf(finalText, off.text).filter { it.isNotBlank() }.joinToString("\n\n")
                                            }
                                        }
                                    }

                                    if (finalText.length < 15) {
                                        errorMsg = "Materi tidak cukup untuk dijadikan kuis. Tambahkan teks atau coretan yang lebih jelas."
                                        isGenerating = false
                                        return@launch
                                    }

                                    generationStatusText = "Merangkai soal kuis AI..."
                                    val res = GeminiApiClient.generateQuizQuestions(
                                        sourceText = finalText,
                                        topicTitle = effectiveTopicTitle,
                                        maxQuestions = questionCount
                                    )
                                    res.onSuccess { questions ->
                                        quizQuestions = questions
                                        activeCloudQuestionIds = emptyList()
                                        activeRecordedAnswers = mutableListOf()
                                        quizStartTime = System.currentTimeMillis()
                                        activeQuizTitle = effectiveTopicTitle
                                        currentIndex = 0
                                        selectedOptionIdx = null
                                        correctCount = 0
                                        cloudSubmissionStatus = null
                                    }.onFailure { err ->
                                        errorMsg = err.message ?: "Gagal membuat kuis."
                                    }
                                } finally {
                                    isGenerating = false
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
                            Text(generationStatusText, fontWeight = FontWeight.SemiBold)
                        } else if (sourceMode == 3) {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mulai Latihan dari Bank Soal", fontWeight = FontWeight.SemiBold)
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
                                            val isCorrect = (idx == q.correctIndex)
                                            if (isCorrect) correctCount++
                                            if (activeCloudQuestionIds.isNotEmpty() && currentIndex in activeCloudQuestionIds.indices) {
                                                val qId = activeCloudQuestionIds[currentIndex]
                                                val matchingCloudQ = cloudQuestions.find { it.id == qId }
                                                val optId = matchingCloudQ?.options?.getOrNull(idx)?.id
                                                activeRecordedAnswers.add(
                                                    QuizSubmissionAnswer(
                                                        questionId = qId,
                                                        selectedOptionId = optId,
                                                        isCorrect = isCorrect,
                                                        timeSpentSeconds = 0
                                                    )
                                                )
                                            }
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
                                            if (currentIndex == quizQuestions.lastIndex) {
                                                if (activeCloudQuestionIds.isNotEmpty()) {
                                                    isSubmittingToCloud = true
                                                    val totalSec = ((System.currentTimeMillis() - quizStartTime) / 1000).toInt().coerceAtLeast(1)
                                                    val scoreVal = if (quizQuestions.isNotEmpty()) (correctCount.toDouble() / quizQuestions.size.toDouble()) * 100.0 else 0.0
                                                    coroutineScope.launch {
                                                        val res = SupabaseClient.submitQuizSession(
                                                            QuizSubmission(
                                                                title = activeQuizTitle,
                                                                subjectName = if (cloudSubjectFilter != "Semua") cloudSubjectFilter else "Umum",
                                                                totalQuestions = quizQuestions.size,
                                                                correctAnswers = correctCount,
                                                                score = scoreVal,
                                                                durationSeconds = totalSec,
                                                                answers = activeRecordedAnswers
                                                            )
                                                        )
                                                        isSubmittingToCloud = false
                                                        res.onSuccess {
                                                            cloudSubmissionStatus = "✅ Hasil kuis tersimpan di Supabase Cloud!"
                                                        }.onFailure { err ->
                                                            cloudSubmissionStatus = "⚠️ Selesai lokal (Gagal sync ke Cloud: ${err.message})"
                                                        }
                                                    }
                                                }
                                            }
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
                                if (cloudSubmissionStatus != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LernspaceSurfaceLow,
                                        border = BorderStroke(1.dp, LernspaceBorder)
                                    ) {
                                        Text(
                                            text = cloudSubmissionStatus!!,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LernspaceDark,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                if (isSubmittingToCloud) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LernspaceDark)
                                        Text("Menyimpan hasil ke Supabase Cloud...", fontSize = 12.sp, color = LernspaceTextSecondary)
                                    }
                                }

                                // Tombol upload soal AI ke Supabase jika kuis berasal dari AI
                                if (activeCloudQuestionIds.isEmpty() && quizQuestions.isNotEmpty()) {
                                    var isUploadingQuestions by remember { mutableStateOf(false) }
                                    var uploadSuccess by remember { mutableStateOf(false) }
                                    Button(
                                        onClick = {
                                            isUploadingQuestions = true
                                            coroutineScope.launch {
                                                var count = 0
                                                val subj = selectedTopic?.let { t -> subjects.find { it.id == t.subjectId }?.name }
                                                    ?: selectedNote?.subject ?: "Materi AI"
                                                for (q in quizQuestions) {
                                                    val opts = q.options.mapIndexed { oIdx, oText ->
                                                        oText to (oIdx == q.correctIndex)
                                                    }
                                                    val r = SupabaseClient.insertQuestion(
                                                        subjectName = subj,
                                                        topicTitle = activeQuizTitle,
                                                        questionText = q.question,
                                                        difficulty = "MEDIUM",
                                                        explanation = q.explanation,
                                                        options = opts
                                                    )
                                                    if (r.isSuccess) count++
                                                }
                                                isUploadingQuestions = false
                                                uploadSuccess = true
                                                refreshCloudTrigger++
                                                Toast.makeText(context, "$count soal berhasil disimpan ke Bank Soal Supabase!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !isUploadingQuestions && !uploadSuccess,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                                    ) {
                                        if (isUploadingQuestions) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = LernspaceSurfaceCard, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Mengunggah ke Cloud...", fontSize = 12.sp)
                                        } else {
                                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (uploadSuccess) "✅ Tersimpan di Bank Soal Supabase" else "Simpan Soal ke Bank Soal Cloud", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        currentIndex = 0
                                        selectedOptionIdx = null
                                        correctCount = 0
                                        cloudSubmissionStatus = null
                                        activeRecordedAnswers = mutableListOf()
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

    if (showAddQuestionDialog) {
        AddQuestionDialog(
            initialSubject = if (cloudSubjectFilter != "Semua") cloudSubjectFilter else null,
            onDismiss = { showAddQuestionDialog = false },
            onQuestionAdded = {
                refreshCloudTrigger++
            }
        )
    }
}
