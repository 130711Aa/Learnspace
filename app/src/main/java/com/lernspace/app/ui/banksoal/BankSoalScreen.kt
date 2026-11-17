package com.lernspace.app.ui.banksoal

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.supabase.BankQuestion
import com.lernspace.app.core.supabase.BankSoalAttemptManager
import com.lernspace.app.core.supabase.SupabaseBankSoalClient
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.components.MarkdownLatexContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BankSoalMainTab {
    CATALOG,    // Bank Soal Utama (Katalog eksplorasi - fokus pemahaman konsep)
    COLLECTION  // Collection Bucket (Soal yang pernah dikerjakan + Pembahasan Lengkap)
}

enum class BankSoalScreenState {
    BROWSE,
    PRACTICE,
    RESULT
}

val CrimsonActionColor = Color(0xFFE11D48) // Refined, balanced Crimson matching modern dark/light UI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSoalScreen(
    onNavigateToQuiz: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    var screenState by remember { mutableStateOf(BankSoalScreenState.BROWSE) }
    var activeMainTab by remember { mutableStateOf(BankSoalMainTab.CATALOG) }

    var allQuestions by remember { mutableStateOf<List<BankQuestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Modern Academic Taxonomy Filters (Subjek -> Topik -> Konsep)
    var selectedSubject by remember { mutableStateOf("Semua") }
    var selectedTopic by remember { mutableStateOf("Semua") }
    var selectedSubtopic by remember { mutableStateOf("Semua") }
    var selectedDifficulty by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }

    // Practice session state
    var practiceQuestions by remember { mutableStateOf<List<BankQuestion>>(emptyList()) }
    var currentPracticeIndex by remember { mutableIntStateOf(0) }
    var userAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) } // index to optionIndex
    var flaggedQuestions by remember { mutableStateOf<Set<Int>>(emptySet()) } // set of indices marked as ragu-ragu
    var isRightSidebarExpanded by remember { mutableStateOf(true) }
    var practiceTimeSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Custom Problem Set / Package Dialog State
    var showPackageDialog by remember { mutableStateOf(false) }
    var packageSourceQuestions by remember { mutableStateOf<List<BankQuestion>>(emptyList()) }
    var packageScopeLabel by remember { mutableStateOf("") }

    fun openPackageDialog(questions: List<BankQuestion>, scope: String) {
        packageSourceQuestions = questions
        packageScopeLabel = scope
        showPackageDialog = true
    }

    fun loadQuestions() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = SupabaseBankSoalClient.fetchQuestions()
            res.onSuccess { data ->
                allQuestions = data
                isLoading = false
            }.onFailure { err ->
                errorMessage = err.message ?: "Gagal terhubung ke Supabase."
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadQuestions()
    }

    // Timer for practice session
    LaunchedEffect(screenState, isTimerRunning) {
        if (screenState == BankSoalScreenState.PRACTICE && isTimerRunning) {
            while (isTimerRunning && screenState == BankSoalScreenState.PRACTICE) {
                delay(1000L)
                practiceTimeSeconds++
            }
        }
    }

    // Dynamic Filter Options
    val availableSubjects = remember(allQuestions) {
        val list = mutableListOf("Semua")
        list.addAll(allQuestions.map { it.subjectName }.filter { it.isNotBlank() }.distinct().sorted())
        list
    }

    val availableTopics = remember(allQuestions, selectedSubject) {
        val list = mutableListOf("Semua")
        val relevant = if (selectedSubject == "Semua") allQuestions else allQuestions.filter { it.subjectName == selectedSubject }
        list.addAll(relevant.map { it.topicTitle }.filter { it.isNotBlank() }.distinct().sorted())
        list
    }

    val availableSubtopics = remember(allQuestions, selectedSubject, selectedTopic) {
        val list = mutableListOf("Semua")
        val relevant = allQuestions.filter { q ->
            (selectedSubject == "Semua" || q.subjectName == selectedSubject) &&
            (selectedTopic == "Semua" || q.topicTitle == selectedTopic)
        }
        list.addAll(relevant.mapNotNull { it.subtopicTitle }.filter { it.isNotBlank() }.distinct().sorted())
        list
    }

    LaunchedEffect(availableTopics) {
        if (!availableTopics.contains(selectedTopic)) selectedTopic = "Semua"
    }
    LaunchedEffect(availableSubtopics) {
        if (!availableSubtopics.contains(selectedSubtopic)) selectedSubtopic = "Semua"
    }

    // Filtered questions based on search & taxonomy
    val filteredQuestions = remember(allQuestions, selectedSubject, selectedTopic, selectedSubtopic, selectedDifficulty, searchQuery) {
        allQuestions.filter { q ->
            val matchSubject = selectedSubject == "Semua" || q.subjectName == selectedSubject
            val matchTopic = selectedTopic == "Semua" || q.topicTitle == selectedTopic
            val matchSubtopic = selectedSubtopic == "Semua" || q.subtopicTitle == selectedSubtopic
            val matchDiff = selectedDifficulty == "Semua" || q.difficulty.equals(selectedDifficulty, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    q.questionText.contains(searchQuery, ignoreCase = true) ||
                    q.subjectName.contains(searchQuery, ignoreCase = true) ||
                    q.topicTitle.contains(searchQuery, ignoreCase = true) ||
                    (q.subtopicTitle?.contains(searchQuery, ignoreCase = true) == true)
            matchSubject && matchTopic && matchSubtopic && matchDiff && matchSearch
        }
    }

    // Collection Bucket: Only questions the user has already completed/attempted!
    val attemptedQuestions = remember(allQuestions, BankSoalAttemptManager.attempts.size) {
        allQuestions.filter { BankSoalAttemptManager.isAttempted(it.id) }
    }

    fun startPractice(questionsToTest: List<BankQuestion>) {
        if (questionsToTest.isEmpty()) return
        practiceQuestions = questionsToTest
        currentPracticeIndex = 0
        userAnswers = emptyMap()
        flaggedQuestions = emptySet()
        practiceTimeSeconds = 0
        isTimerRunning = true
        screenState = BankSoalScreenState.PRACTICE
    }

    fun finishPractice() {
        isTimerRunning = false

        // Record attempts in BankSoalAttemptManager so they unlock in Collection Bucket!
        practiceQuestions.forEachIndexed { idx, q ->
            val chosenOptIdx = userAnswers[idx]
            if (chosenOptIdx != null && chosenOptIdx in q.options.indices) {
                val isCorrect = q.options[chosenOptIdx].isCorrect
                BankSoalAttemptManager.recordAttempt(q.id, chosenOptIdx, isCorrect)
            }
        }

        screenState = BankSoalScreenState.RESULT

        // Async save to Supabase
        coroutineScope.launch {
            val total = practiceQuestions.size
            val correct = practiceQuestions.indices.count { idx ->
                val chosenOptIdx = userAnswers[idx]
                val q = practiceQuestions[idx]
                chosenOptIdx != null && chosenOptIdx in q.options.indices && q.options[chosenOptIdx].isCorrect
            }
            val score = if (total > 0) (correct.toDouble() / total * 100.0) else 0.0
            val titleScope = when {
                selectedSubtopic != "Semua" -> selectedSubtopic
                selectedTopic != "Semua" -> selectedTopic
                selectedSubject != "Semua" -> selectedSubject
                else -> "Latihan Terbuka"
            }
            SupabaseBankSoalClient.saveQuizSession(
                title = titleScope,
                subjectName = selectedSubject.takeIf { it != "Semua" },
                totalQuestions = total,
                correctAnswers = correct,
                score = score,
                durationSeconds = practiceTimeSeconds
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LernspaceSurface)
    ) {
        when (screenState) {
            BankSoalScreenState.BROWSE -> {
                BankSoalCompactBrowseView(
                    activeTab = activeMainTab,
                    onTabChange = { activeMainTab = it },
                    allQuestions = allQuestions,
                    filteredQuestions = filteredQuestions,
                    attemptedQuestions = attemptedQuestions,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    selectedSubject = selectedSubject,
                    selectedTopic = selectedTopic,
                    selectedSubtopic = selectedSubtopic,
                    selectedDifficulty = selectedDifficulty,
                    searchQuery = searchQuery,
                    availableSubjects = availableSubjects,
                    availableTopics = availableTopics,
                    availableSubtopics = availableSubtopics,
                    onSelectSubject = { selectedSubject = it },
                    onSelectTopic = { selectedTopic = it },
                    onSelectSubtopic = { selectedSubtopic = it },
                    onSelectDifficulty = { selectedDifficulty = it },
                    onSearchQueryChange = { searchQuery = it },
                    onRefresh = { loadQuestions() },
                    onOpenPackageDialog = { list, scope -> openPackageDialog(list, scope) },
                    onPracticeConcept = { concept ->
                        val conceptQuestions = allQuestions.filter { it.subtopicTitle == concept }
                        openPackageDialog(conceptQuestions, "Konsep: $concept")
                    },
                    onPracticeSingle = { q -> startPractice(listOf(q)) },
                    onNavigateToQuiz = onNavigateToQuiz
                )

                if (showPackageDialog) {
                    CreatePackageDialog(
                        scopeTitle = packageScopeLabel,
                        availableQuestions = packageSourceQuestions,
                        onDismiss = { showPackageDialog = false },
                        onConfirmPackage = { selectedQuestions ->
                            showPackageDialog = false
                            startPractice(selectedQuestions)
                        }
                    )
                }
            }

            BankSoalScreenState.PRACTICE -> {
                BankSoalSplitPracticeView(
                    questions = practiceQuestions,
                    currentIndex = currentPracticeIndex,
                    userAnswers = userAnswers,
                    flaggedQuestions = flaggedQuestions,
                    elapsedSeconds = practiceTimeSeconds,
                    isSidebarExpanded = isRightSidebarExpanded,
                    onToggleSidebar = { isRightSidebarExpanded = !isRightSidebarExpanded },
                    onSelectOption = { qIdx, optIdx ->
                        userAnswers = userAnswers + (qIdx to optIdx)
                    },
                    onToggleFlag = { qIdx ->
                        flaggedQuestions = if (flaggedQuestions.contains(qIdx)) {
                            flaggedQuestions - qIdx
                        } else {
                            flaggedQuestions + qIdx
                        }
                    },
                    onNavigateIndex = { newIdx ->
                        currentPracticeIndex = newIdx.coerceIn(0, practiceQuestions.lastIndex)
                    },
                    onFinish = { finishPractice() },
                    onCancel = {
                        isTimerRunning = false
                        screenState = BankSoalScreenState.BROWSE
                    }
                )
            }

            BankSoalScreenState.RESULT -> {
                BankSoalResultSummaryView(
                    questions = practiceQuestions,
                    userAnswers = userAnswers,
                    durationSeconds = practiceTimeSeconds,
                    onRetry = { startPractice(practiceQuestions) },
                    onGoToCollection = {
                        activeMainTab = BankSoalMainTab.COLLECTION
                        screenState = BankSoalScreenState.BROWSE
                    },
                    onBackToBrowse = { screenState = BankSoalScreenState.BROWSE }
                )
            }
        }
    }
}

/**
 * Modern Compact Browse View:
 * - Tab Bar: "Bank Soal (Katalog)" vs "Koleksi & Pembahasan 🔓"
 * - Compact single-line filter (Subjek, Topik, Konsep, Kesulitan)
 * - Questions do NOT show answers/explanations until worked on!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankSoalCompactBrowseView(
    activeTab: BankSoalMainTab,
    onTabChange: (BankSoalMainTab) -> Unit,
    allQuestions: List<BankQuestion>,
    filteredQuestions: List<BankQuestion>,
    attemptedQuestions: List<BankQuestion>,
    isLoading: Boolean,
    errorMessage: String?,
    selectedSubject: String,
    selectedTopic: String,
    selectedSubtopic: String,
    selectedDifficulty: String,
    searchQuery: String,
    availableSubjects: List<String>,
    availableTopics: List<String>,
    availableSubtopics: List<String>,
    onSelectSubject: (String) -> Unit,
    onSelectTopic: (String) -> Unit,
    onSelectSubtopic: (String) -> Unit,
    onSelectDifficulty: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenPackageDialog: (List<BankQuestion>, String) -> Unit,
    onPracticeConcept: (String) -> Unit,
    onPracticeSingle: (BankQuestion) -> Unit,
    onNavigateToQuiz: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Bar: Title & Mode Switcher (Katalog vs Koleksi)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(
                        text = "Bank Soal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Text(
                        text = "Katalog soal terstandarisasi untuk menguji pemahaman konsep.",
                        fontSize = 12.sp,
                        color = LernspaceTextSecondary
                    )
                }

                // Modern Pill Tab Switcher
                Surface(
                    color = LernspaceSurfaceCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        val isCatalog = activeTab == BankSoalMainTab.CATALOG
                        Surface(
                            modifier = Modifier.clickable { onTabChange(BankSoalMainTab.CATALOG) },
                            shape = RoundedCornerShape(7.dp),
                            color = if (isCatalog) LernspaceDark else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.FolderOpen,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isCatalog) Color.White else LernspaceDark
                                )
                                Text(
                                    "Katalog Soal",
                                    fontSize = 12.sp,
                                    fontWeight = if (isCatalog) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCatalog) Color.White else LernspaceDark
                                )
                            }
                        }

                        val isCollection = activeTab == BankSoalMainTab.COLLECTION
                        Surface(
                            modifier = Modifier.clickable { onTabChange(BankSoalMainTab.COLLECTION) },
                            shape = RoundedCornerShape(7.dp),
                            color = if (isCollection) LernspaceDark else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.LockOpen,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isCollection) Color.White else LernspaceDark
                                )
                                Text(
                                    "Koleksi & Pembahasan (${attemptedQuestions.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (isCollection) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCollection) Color.White else LernspaceDark
                                )
                            }
                        }
                    }
                }
            }

            // Right header actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = LernspaceDark, modifier = Modifier.size(20.dp))
                    }
                }

                Button(
                    onClick = onNavigateToQuiz,
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceSurfaceCard, contentColor = LernspaceDark),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LernspaceBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(14.dp), tint = AccentPurple)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Kuis AI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (activeTab == BankSoalMainTab.CATALOG && filteredQuestions.isNotEmpty()) {
                    Button(
                        onClick = {
                            val scope = buildString {
                                if (selectedSubject != "Semua") append(selectedSubject) else append("Semua Subjek")
                                if (selectedTopic != "Semua") append(" › $selectedTopic")
                                if (selectedSubtopic != "Semua") append(" › $selectedSubtopic")
                            }
                            onOpenPackageDialog(filteredQuestions, scope)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.AutoStories, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buat Paket Soal (${filteredQuestions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (activeTab == BankSoalMainTab.COLLECTION && attemptedQuestions.isNotEmpty()) {
                    Button(
                        onClick = {
                            onOpenPackageDialog(attemptedQuestions, "Koleksi & Pembahasan Tersimpan")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Replay, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Latihan Ulang Koleksi (${attemptedQuestions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // COMPACT FILTER BAR (Single unified horizontal bar - hemat tempat)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dropdown 1: Subjek
                CompactFilterDropdown(
                    label = "Subjek",
                    selectedValue = selectedSubject,
                    options = availableSubjects,
                    onSelect = onSelectSubject
                )

                // Dropdown 2: Topik
                CompactFilterDropdown(
                    label = "Topik",
                    selectedValue = selectedTopic,
                    options = availableTopics,
                    onSelect = onSelectTopic
                )

                // Dropdown 3: Konsep (Sub-topik)
                if (availableSubtopics.size > 1) {
                    CompactFilterDropdown(
                        label = "Konsep",
                        selectedValue = selectedSubtopic,
                        options = availableSubtopics,
                        onSelect = onSelectSubtopic
                    )
                }

                // Dropdown 4: Tingkat Kesulitan
                CompactFilterDropdown(
                    label = "Tingkat",
                    selectedValue = when (selectedDifficulty) {
                        "EASY" -> "Mudah"
                        "MEDIUM" -> "Sedang"
                        "HARD" -> "Sulit"
                        else -> "Semua"
                    },
                    options = listOf("Semua", "Mudah", "Sedang", "Sulit"),
                    onSelect = {
                        val key = when (it) {
                            "Mudah" -> "EASY"
                            "Sedang" -> "MEDIUM"
                            "Sulit" -> "HARD"
                            else -> "Semua"
                        }
                        onSelectDifficulty(key)
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Compact Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Cari soal...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(14.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(220.dp).height(38.dp)
                )
            }
        }

        val displayList = if (activeTab == BankSoalMainTab.CATALOG) filteredQuestions else attemptedQuestions.filter { q ->
            (selectedSubject == "Semua" || q.subjectName == selectedSubject) &&
            (selectedTopic == "Semua" || q.topicTitle == selectedTopic) &&
            (selectedSubtopic == "Semua" || q.subtopicTitle == selectedSubtopic)
        }

        // Question List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LernspaceDark)
            }
        } else if (displayList.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = LernspaceSurfaceCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LernspaceBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (activeTab == BankSoalMainTab.COLLECTION) Icons.Filled.Lock else Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = LernspaceTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (activeTab == BankSoalMainTab.COLLECTION) "Belum Ada Soal di Koleksi" else "Tidak Ada Soal yang Sesuai",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = LernspaceDark
                    )
                    Text(
                        text = if (activeTab == BankSoalMainTab.COLLECTION) "Kerjakan latihan soal di tab Katalog terlebih dahulu. Pembahasan soal akan otomatis terbuka di sini!" else "Coba ubah filter Subjek atau Topik di atas.",
                        fontSize = 12.sp,
                        color = LernspaceTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(displayList, key = { _, item -> item.id }) { index, q ->
                    val isAttempted = BankSoalAttemptManager.isAttempted(q.id)
                    val attempt = BankSoalAttemptManager.getAttempt(q.id)

                    CompactQuestionCard(
                        question = q,
                        index = index,
                        isCollectionMode = activeTab == BankSoalMainTab.COLLECTION,
                        isAttempted = isAttempted,
                        attemptRecord = attempt,
                        onPractice = { onPracticeSingle(q) },
                        onPracticeConcept = q.subtopicTitle?.let { concept ->
                            { onPracticeConcept(concept) }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Compact Dropdown Selector with Pop-up Menu
 */
@Composable
private fun CompactFilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(7.dp),
            color = if (selectedValue != "Semua") LernspaceDark.copy(alpha = 0.08f) else LernspaceSurface,
            border = BorderStroke(1.dp, if (selectedValue != "Semua") LernspaceDark.copy(alpha = 0.3f) else LernspaceBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$label: $selectedValue",
                    fontSize = 11.sp,
                    fontWeight = if (selectedValue != "Semua") FontWeight.Bold else FontWeight.Medium,
                    color = LernspaceDark
                )
                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = LernspaceTextSecondary)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opt,
                            fontSize = 12.sp,
                            fontWeight = if (opt == selectedValue) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Card for an individual question:
 * - In Catalog mode: NO SPOILER! Only question statement, taxonomy, difficulty, and button to practice.
 * - In Collection mode: Shows full explanation, user answer, and correct key!
 */
@Composable
private fun CompactQuestionCard(
    question: BankQuestion,
    index: Int,
    isCollectionMode: Boolean,
    isAttempted: Boolean,
    attemptRecord: com.lernspace.app.core.supabase.QuestionAttempt?,
    onPractice: () -> Unit,
    onPracticeConcept: (() -> Unit)? = null
) {
    var expandedExplanation by remember { mutableStateOf(isCollectionMode) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceCard,
        border = BorderStroke(1.dp, LernspaceBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Taxonomy & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Index Badge
                    Surface(
                        color = LernspaceDark,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Subjek Badge
                    Surface(
                        color = AccentBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = question.subjectName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Topik Badge
                    Surface(
                        color = AccentPurple.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = question.topicTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentPurple,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Konsep Badge
                    if (!question.subtopicTitle.isNullOrBlank()) {
                        Surface(
                            color = AccentGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = question.subtopicTitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Attempted status pill
                if (isAttempted) {
                    val wasCorrect = attemptRecord?.isCorrect == true
                    Surface(
                        color = if (wasCorrect) AccentGreen.copy(alpha = 0.12f) else AccentRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (wasCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                null,
                                modifier = Modifier.size(11.dp),
                                tint = if (wasCorrect) AccentGreen else AccentRed
                            )
                            Text(
                                text = if (wasCorrect) "Sudah Dikerjakan • Benar" else "Sudah Dikerjakan • Perlu Review",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (wasCorrect) AccentGreen else AccentRed
                            )
                        }
                    }
                } else {
                    Surface(
                        color = LernspaceSurfaceLow,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                null,
                                modifier = Modifier.size(10.dp),
                                tint = LernspaceTextSecondary
                            )
                            Text(
                                text = "Belum Dikerjakan",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = LernspaceTextSecondary
                            )
                        }
                    }
                }
            }

            // Question Statement with LaTeX
            MarkdownLatexContent(
                markdown = question.questionText,
                baseFontSize = 14.sp,
                textColor = LernspaceDark
            )

            // IF IN COLLECTION MODE OR EXPANDED (ONLY FOR ATTEMPTED QUESTIONS!)
            if (isAttempted && expandedExplanation) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Pilihan Jawaban & Kunci Resmi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)

                    val userPickIdx = attemptRecord?.selectedOptionIndex

                    question.options.forEachIndexed { optIdx, opt ->
                        val isCorrectKey = opt.isCorrect
                        val isUserPick = userPickIdx == optIdx

                        val bg = when {
                            isCorrectKey -> AccentGreen.copy(alpha = 0.12f)
                            isUserPick -> AccentRed.copy(alpha = 0.12f)
                            else -> LernspaceSurface
                        }
                        val border = when {
                            isCorrectKey -> AccentGreen
                            isUserPick -> AccentRed
                            else -> LernspaceBorder
                        }

                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = bg,
                            border = BorderStroke(1.dp, border)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${opt.optionKey}.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrectKey) AccentGreen else if (isUserPick) AccentRed else LernspaceDark
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    MarkdownLatexContent(
                                        markdown = opt.optionText,
                                        baseFontSize = 12.sp,
                                        textColor = LernspaceDark
                                    )
                                }
                                if (isCorrectKey) {
                                    Text("Kunci Jawaban", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                }
                                if (isUserPick && !isCorrectKey) {
                                    Text("Jawaban Anda", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                                }
                            }
                        }
                    }

                    if (!question.explanation.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = AccentPurple.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Lightbulb, null, modifier = Modifier.size(13.dp), tint = AccentPurple)
                                    Text("Pembahasan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                                }
                                MarkdownLatexContent(
                                    markdown = question.explanation,
                                    baseFontSize = 12.sp,
                                    textColor = LernspaceDark
                                )
                            }
                        }
                    }
                }
            }

            // Bottom action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAttempted) {
                    TextButton(
                        onClick = { expandedExplanation = !expandedExplanation },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (expandedExplanation) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AccentBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expandedExplanation) "Sembunyikan Pembahasan" else "Lihat Pembahasan Lengkap",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentBlue
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Lock, null, modifier = Modifier.size(13.dp), tint = LernspaceTextSecondary)
                        Text(
                            text = "Kerjakan soal ini untuk membuka pembahasan",
                            fontSize = 11.sp,
                            color = LernspaceTextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!question.subtopicTitle.isNullOrBlank() && onPracticeConcept != null) {
                        OutlinedButton(
                            onClick = onPracticeConcept,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.AutoStories, null, modifier = Modifier.size(12.dp), tint = AccentPurple)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paket Konsep Ini", fontSize = 11.sp, color = AccentPurple, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = onPractice,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, LernspaceBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(13.dp), tint = LernspaceDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAttempted) "Kerjakan Ulang" else "Kerjakan Soal Ini",
                            fontSize = 11.sp,
                            color = LernspaceDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * SPLIT PRACTICE VIEWPORT (MATCHING USER SCREENSHOT EXACTLY!)
 * Left: Question Body + Large Rounded Options + Timer & Navigation Bar
 * Right: "Semua Soal" Header + Number Grid (5 cols, answered/flagged) + Pink Finish Button
 */
@Composable
private fun BankSoalSplitPracticeView(
    questions: List<BankQuestion>,
    currentIndex: Int,
    userAnswers: Map<Int, Int>,
    flaggedQuestions: Set<Int>,
    elapsedSeconds: Int,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    onSelectOption: (qIndex: Int, optIndex: Int) -> Unit,
    onToggleFlag: (qIndex: Int) -> Unit,
    onNavigateIndex: (Int) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    val q = questions.getOrNull(currentIndex) ?: return
    val selectedOptionIdx = userAnswers[currentIndex]
    val isFlagged = flaggedQuestions.contains(currentIndex)
    var showCancelDialog by remember { mutableStateOf(false) }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Batal Latihan?") },
            text = { Text("Jawaban yang sudah Anda pilih akan hilang jika keluar sekarang.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    onCancel()
                }) {
                    Text("Ya, Keluar", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Lanjutkan")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── LEFT COLUMN: QUESTION VIEWPORT & CONTROLS ─────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: [ ✕ ] [ Thin Progress Bar ] [ 🚩 ] [ ☀️ ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = { showCancelDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = LernspaceDark)
                }

                // Thin sleek progress bar
                val progress = (currentIndex + 1).toFloat() / questions.size.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.Black,
                    trackColor = Color(0xFFE5E7EB)
                )

                // Flag / Bookmark (Ragu-ragu)
                IconButton(
                    onClick = { onToggleFlag(currentIndex) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFlagged) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Ragu-ragu",
                        tint = if (isFlagged) Color(0xFFF59E0B) else Color.Gray
                    )
                }

                // Theme / Sun indicator
                IconButton(onClick = { /* toggle theme */ }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.WbSunny, contentDescription = "Tema", tint = Color.Gray)
                }
            }

            // Center Content: Question Statement + Options
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // LaTeX Question
                MarkdownLatexContent(
                    markdown = q.questionText,
                    baseFontSize = 17.sp,
                    textColor = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Spacious rounded rectangular options (matching screenshot)
                q.options.forEachIndexed { optIdx, opt ->
                    val isSelected = selectedOptionIdx == optIdx

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectOption(currentIndex, optIdx) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFE5E7EB) else Color.White,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF374151) else Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            MarkdownLatexContent(
                                markdown = opt.optionText,
                                baseFontSize = 15.sp,
                                textColor = Color(0xFF111827)
                            )
                        }
                    }
                }
            }

            // Bottom Bar: [ 🟡 Timer ] [ ← 1/15 → ] [ ⊞ Grid Toggle ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Timer Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFF59E0B)
                    )
                    Text(
                        text = timeFormatted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }

                // Center: Navigation Pill (← 1/15 →)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { onNavigateIndex(currentIndex - 1) },
                            enabled = currentIndex > 0,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sebelumnya", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "${currentIndex + 1}/${questions.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )

                        IconButton(
                            onClick = { onNavigateIndex(currentIndex + 1) },
                            enabled = currentIndex < questions.lastIndex,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Berikutnya", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Right: Grid Sidebar Toggle
                IconButton(onClick = onToggleSidebar, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.GridView,
                        contentDescription = "Toggle Grid",
                        tint = if (isSidebarExpanded) Color.Black else Color.Gray
                    )
                }
            }
        }

        // ── RIGHT COLUMN: SIDEBAR ("Semua Soal" + Number Grid + Finish Button) ──
        AnimatedVisibility(
            visible = isSidebarExpanded,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                color = Color(0xFFFAFAFA),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Title & Metadata
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onToggleSidebar, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tutup Sidebar", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "Semua Soal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = q.subjectName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.FormatListNumbered, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Text("${questions.size} soal", fontSize = 11.sp, color = Color.Gray)
                                Text("•", fontSize = 11.sp, color = Color.Gray)
                                Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Text("Tanpa batas", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        // Number Grid (5 Columns like in screenshot)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(questions) { idx, _ ->
                                val isAnswered = userAnswers.containsKey(idx)
                                val isCurrent = idx == currentIndex
                                val isThisFlagged = flaggedQuestions.contains(idx)

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isAnswered -> Color(0xFF27272A) // Dark filled like in image
                                                else -> Color.White
                                            }
                                        )
                                        .clickable { onNavigateIndex(idx) }
                                        .then(
                                            if (isCurrent) Modifier.background(
                                                color = if (isAnswered) Color(0xFF27272A) else Color.White,
                                                shape = RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Base outline if unanswered
                                    if (!isAnswered) {
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color.Transparent,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) Color(0xFF111827) else Color(0xFFE5E7EB))
                                        ) {}
                                    }

                                    // Number
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAnswered) Color.White else Color(0xFF111827)
                                    )

                                    // Yellow bookmark flag on top-right if flagged
                                    if (isThisFlagged) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp),
                                            contentAlignment = Alignment.TopEnd
                                        ) {
                                            Surface(
                                                color = Color(0xFFF59E0B),
                                                shape = RoundedCornerShape(2.dp),
                                                modifier = Modifier.size(6.dp)
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom: Action Button ("Selesai Mengerjakan") & Mode Pills
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onFinish,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonActionColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Selesai Mengerjakan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        // Subtle clean mode badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = LernspaceSurfaceCard,
                                border = BorderStroke(1.dp, LernspaceBorder),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.AutoStories, null, modifier = Modifier.size(12.dp), tint = LernspaceDark)
                                    Text("Latihan Mandiri", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                                }
                            }

                            Surface(
                                color = LernspaceSurfaceCard,
                                border = BorderStroke(1.dp, LernspaceBorder),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.LockOpen, null, modifier = Modifier.size(12.dp), tint = LernspaceDark)
                                    Text("Buka Koleksi", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Result Summary Screen with prompt to open the Collection Bucket!
 */
@Composable
private fun BankSoalResultSummaryView(
    questions: List<BankQuestion>,
    userAnswers: Map<Int, Int>,
    durationSeconds: Int,
    onRetry: () -> Unit,
    onGoToCollection: () -> Unit,
    onBackToBrowse: () -> Unit
) {
    val total = questions.size
    val correct = questions.indices.count { idx ->
        val chosenOptIdx = userAnswers[idx]
        val q = questions[idx]
        chosenOptIdx != null && chosenOptIdx in q.options.indices && q.options[chosenOptIdx].isCorrect
    }
    val score = if (total > 0) (correct.toDouble() / total * 100.0) else 0.0

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.width(480.dp),
            shape = RoundedCornerShape(20.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (score >= 70) AccentGreen.copy(alpha = 0.15f) else CrimsonActionColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${score.toInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (score >= 70) AccentGreen else CrimsonActionColor
                        )
                    }
                }

                Text(
                    text = "Latihan Selesai!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LernspaceDark
                )

                Text(
                    text = "Seluruh ${total} soal yang Anda kerjakan telah tersimpan di Koleksi. Pembahasan dan kunci jawaban kini dapat ditinjau kapan saja.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = LernspaceTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Benar", fontSize = 11.sp, color = LernspaceTextSecondary)
                        Text("$correct", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Salah", fontSize = 11.sp, color = LernspaceTextSecondary)
                        Text("${total - correct}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Waktu", fontSize = 11.sp, color = LernspaceTextSecondary)
                        Text(timeFormatted, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onGoToCollection,
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Filled.LockOpen, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Koleksi & Pembahasan Lengkap", fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(8.dp)) {
                        Text("Ulangi Latihan")
                    }
                    OutlinedButton(onClick = onBackToBrowse, shape = RoundedCornerShape(8.dp)) {
                        Text("Kembali ke Katalog")
                    }
                }
            }
        }
    }
}

/**
 * Dialog to configure and launch a customized Question Package (Paket Latihan):
 * - Target Scope (Subjek / Topik / Konsep)
 * - Number of questions (5, 10, 15, 20, or All)
 * - Prioritize unattempted questions
 * - Shuffle option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePackageDialog(
    scopeTitle: String,
    availableQuestions: List<BankQuestion>,
    onDismiss: () -> Unit,
    onConfirmPackage: (List<BankQuestion>) -> Unit
) {
    val totalAvailable = availableQuestions.size
    val unattemptedCount = availableQuestions.count { !BankSoalAttemptManager.isAttempted(it.id) }
    var selectedCount by remember { mutableIntStateOf(minOf(5, totalAvailable)) }
    var prioritizeUnattempted by remember { mutableStateOf(unattemptedCount > 0) }
    var shuffleQuestions by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = LernspaceDark.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoStories,
                        null,
                        tint = LernspaceDark,
                        modifier = Modifier.padding(6.dp).size(20.dp)
                    )
                }
                Column {
                    Text("Buat Paket Soal Latihan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    Text("Latihan terfokus pada konsep materi pilihan", fontSize = 11.sp, color = LernspaceTextSecondary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Scope Pill
                Surface(
                    color = LernspaceSurfaceLow,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LernspaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Cakupan Materi:", fontSize = 10.sp, color = LernspaceTextSecondary, fontWeight = FontWeight.Medium)
                        Text(scopeTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    }
                }

                // Question Quantity Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Jumlah Soal:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                        Text("$selectedCount dari $totalAvailable soal", fontSize = 11.sp, color = LernspaceTextSecondary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val options = listOf(5, 10, 15, 20).filter { it <= totalAvailable } + listOf(totalAvailable)
                        options.distinct().forEach { count ->
                            val label = if (count == totalAvailable) "Semua ($count)" else "$count Soal"
                            FilterChip(
                                selected = selectedCount == count,
                                onClick = { selectedCount = count },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }
                }

                // Option: Prioritize Unattempted
                if (unattemptedCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fokus Soal Baru", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                            Text("Dahulukan $unattemptedCount soal yang belum pernah dikerjakan", fontSize = 10.sp, color = LernspaceTextSecondary)
                        }
                        Switch(
                            checked = prioritizeUnattempted,
                            onCheckedChange = { prioritizeUnattempted = it }
                        )
                    }
                }

                // Option: Shuffle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Acak Urutan Soal", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                        Text("Variasi soal agar tidak monoton", fontSize = 10.sp, color = LernspaceTextSecondary)
                    }
                    Switch(
                        checked = shuffleQuestions,
                        onCheckedChange = { shuffleQuestions = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var list = availableQuestions
                    if (prioritizeUnattempted) {
                        val unattempted = list.filter { !BankSoalAttemptManager.isAttempted(it.id) }
                        val attempted = list.filter { BankSoalAttemptManager.isAttempted(it.id) }
                        list = unattempted + attempted
                    }
                    if (shuffleQuestions) {
                        list = list.shuffled()
                    }
                    val finalPackage = list.take(selectedCount)
                    onConfirmPackage(finalPackage)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mulai Kerjakan ($selectedCount Soal)", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
