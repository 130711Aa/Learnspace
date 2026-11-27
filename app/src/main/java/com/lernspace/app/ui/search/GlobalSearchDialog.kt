package com.lernspace.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lernspace.app.core.mimolab.MimoLabActiveSession
import com.lernspace.app.core.mimolab.MimoSimulation
import com.lernspace.app.core.supabase.BankQuestion
import com.lernspace.app.core.supabase.SupabaseBankSoalClient
import com.lernspace.app.core.supabase.SupabaseMimoLabClient
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.data.repository.FlashcardDeckUi
import com.lernspace.app.data.repository.FlashcardUi
import com.lernspace.app.data.repository.PlannerItemUi
import com.lernspace.app.data.repository.TaskUiModel
import com.lernspace.app.data.repository.WeeklyClassUi
import com.lernspace.app.navigation.Screen
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.practice.PracticeHubState
import com.lernspace.app.ui.practice.PracticeTab
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.model.NoteUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Top bar Search Pill strictly following the Stitch design system
 * (lernspace_home_dashboard_landscape), optimized for touch/tablet.
 */
@Composable
fun GlobalSearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = LernspaceTextSecondary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Cari catatan, silabus, tugas, jadwal, flashcard, simulasi lab, bank soal...",
                color = LernspaceTextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

enum class SearchCategory(val label: String, val icon: ImageVector) {
    ALL("Semua", Icons.Filled.Search),
    NOTES("Catatan", Icons.Filled.EditNote),
    TOPICS("Silabus", Icons.AutoMirrored.Filled.MenuBook),
    TASKS("Tugas", Icons.Filled.CheckCircleOutline),
    SUBJECTS("Mata Kuliah", Icons.Filled.School),
    SCHEDULE("Jadwal & KRS", Icons.Filled.AccessTime),
    FLASHCARDS("Flashcard", Icons.Filled.Style),
    SIMULATIONS("Lab Sains & 3D", Icons.Filled.Functions),
    PRACTICE("Bank Soal", Icons.Filled.FolderCopy)
}

data class Math3DPresetItem(
    val title: String,
    val subtitle: String,
    val formulaOrCategory: String
)

sealed class SearchResultItem {
    data class NoteResult(val note: NoteUiModel) : SearchResultItem()
    data class TopicResult(val topic: TopicUi, val subject: SubjectUi?) : SearchResultItem()
    data class TaskResult(val task: TaskUiModel) : SearchResultItem()
    data class SubjectResult(val subject: SubjectUi) : SearchResultItem()
    data class ScheduleResult(val item: PlannerItemUi) : SearchResultItem()
    data class WeeklyClassResult(val weeklyClass: WeeklyClassUi) : SearchResultItem()
    data class FlashcardDeckResult(val deck: FlashcardDeckUi, val cardCount: Int) : SearchResultItem()
    data class FlashcardResult(val card: FlashcardUi, val deckTitle: String?) : SearchResultItem()
    data class SimulationResult(val sim: MimoSimulation) : SearchResultItem()
    data class Math3DResult(val item: Math3DPresetItem) : SearchResultItem()
    data class BankQuestionResult(val question: BankQuestion) : SearchResultItem()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchDialog(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    // Core In-Memory Academic State
    val allNotes = AcademicWorkspaceState.notes
    val allTopics = AcademicWorkspaceState.topics
    val allTasks = AcademicWorkspaceState.tasks
    val allSubjects = AcademicWorkspaceState.subjects
    val allSchedule = AcademicWorkspaceState.scheduleItems
    val allWeeklyClasses = AcademicWorkspaceState.weeklyClasses
    val allDecks = AcademicWorkspaceState.flashcardDecks
    val allFlashcards = AcademicWorkspaceState.flashcards

    // Simulations (MimoLab STEM Simulations)
    val allSimulations = remember {
        SupabaseMimoLabClient.getBuiltInStarterSimulations()
    }

    // GeoNix Math 3D Presets
    val math3dPresets = remember {
        listOf(
            Math3DPresetItem("Benda Putar & Integral Volume", "Visualisasi integral volume putar sumbu-x y = sqrt(x)", "Kalkulus 3D"),
            Math3DPresetItem("Vektor Ruang 3D (R³)", "Operasi vektor u = (3, 2, 4), dot product & sudut ruang", "Aljabar Vektor"),
            Math3DPresetItem("Permukaan Multivariabel z = f(x, y)", "Kontur 3D paraboloid & sadel hiperbolik dinamis", "Kalkulus Lanjut"),
            Math3DPresetItem("Bangun Ruang Geometri 3D", "Polyhedron, kubus, silinder, kerucut & bola interaktif", "Geometri Ruang"),
            Math3DPresetItem("Jumlah Riemann 2D & 3D", "Aproksimasi partisi integral kotak persegi", "Kalkulus")
        )
    }

    // Bank Soal Questions
    var bankQuestions by remember { mutableStateOf<List<BankQuestion>>(emptyList()) }
    LaunchedEffect(Unit) {
        val cached = SupabaseBankSoalClient.loadQuestionsFromCache()
        if (cached.isNotEmpty()) {
            bankQuestions = cached
        } else {
            coroutineScope.launch {
                SupabaseBankSoalClient.fetchQuestions().onSuccess {
                    bankQuestions = it
                }
            }
        }
    }

    // Search Query Processing
    val trimmedQuery = searchQuery.trim().lowercase()

    val matchedNotes = remember(trimmedQuery, allNotes.size, allNotes.toList()) {
        if (trimmedQuery.isBlank()) allNotes.toList()
        else allNotes.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    it.displaySnippet.lowercase().contains(trimmedQuery) ||
                    it.subject.lowercase().contains(trimmedQuery) ||
                    (it.topicName?.lowercase()?.contains(trimmedQuery) == true)
        }
    }

    val matchedTopics = remember(trimmedQuery, allTopics.size, allTopics.toList()) {
        if (trimmedQuery.isBlank()) allTopics.toList()
        else allTopics.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    it.description.lowercase().contains(trimmedQuery)
        }
    }

    val matchedTasks = remember(trimmedQuery, allTasks.size, allTasks.toList()) {
        if (trimmedQuery.isBlank()) allTasks.toList()
        else allTasks.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    it.notes.lowercase().contains(trimmedQuery) ||
                    (it.subjectName?.lowercase()?.contains(trimmedQuery) == true) ||
                    (it.topicName?.lowercase()?.contains(trimmedQuery) == true) ||
                    it.priority.lowercase().contains(trimmedQuery)
        }
    }

    val matchedSubjects = remember(trimmedQuery, allSubjects.size, allSubjects.toList()) {
        if (trimmedQuery.isBlank()) allSubjects.toList()
        else allSubjects.filter {
            it.name.lowercase().contains(trimmedQuery) ||
                    it.code.lowercase().contains(trimmedQuery) ||
                    it.description.lowercase().contains(trimmedQuery)
        }
    }

    val matchedSchedule = remember(trimmedQuery, allSchedule.size, allSchedule.toList()) {
        if (trimmedQuery.isBlank()) allSchedule.toList()
        else allSchedule.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    (it.subjectName?.lowercase()?.contains(trimmedQuery) == true) ||
                    (it.locationOrRoom?.lowercase()?.contains(trimmedQuery) == true) ||
                    (it.topicName?.lowercase()?.contains(trimmedQuery) == true)
        }
    }

    val matchedWeeklyClasses = remember(trimmedQuery, allWeeklyClasses.size, allWeeklyClasses.toList()) {
        if (trimmedQuery.isBlank()) allWeeklyClasses.toList()
        else allWeeklyClasses.filter {
            it.subjectName.lowercase().contains(trimmedQuery) ||
                    it.subjectCode.lowercase().contains(trimmedQuery) ||
                    (it.room?.lowercase()?.contains(trimmedQuery) == true) ||
                    (it.lecturer?.lowercase()?.contains(trimmedQuery) == true) ||
                    it.type.lowercase().contains(trimmedQuery)
        }
    }

    val matchedDecks = remember(trimmedQuery, allDecks.size, allDecks.toList()) {
        if (trimmedQuery.isBlank()) allDecks.toList()
        else allDecks.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    it.description.lowercase().contains(trimmedQuery) ||
                    it.subjectName.lowercase().contains(trimmedQuery) ||
                    (it.topicName?.lowercase()?.contains(trimmedQuery) == true)
        }
    }

    val matchedFlashcards = remember(trimmedQuery, allFlashcards.size, allFlashcards.toList()) {
        if (trimmedQuery.isBlank()) allFlashcards.take(15)
        else allFlashcards.filter {
            it.front.lowercase().contains(trimmedQuery) ||
                    it.back.lowercase().contains(trimmedQuery) ||
                    it.hint.lowercase().contains(trimmedQuery)
        }
    }

    val matchedSimulations = remember(trimmedQuery, allSimulations) {
        if (trimmedQuery.isBlank()) allSimulations
        else allSimulations.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    it.description.lowercase().contains(trimmedQuery) ||
                    it.conceptTopic.lowercase().contains(trimmedQuery) ||
                    it.subject.displayName.lowercase().contains(trimmedQuery) ||
                    it.subject.chipLabel.lowercase().contains(trimmedQuery)
        }
    }

    val matchedMath3D = remember(trimmedQuery, math3dPresets) {
        if (trimmedQuery.isBlank()) math3dPresets
        else math3dPresets.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    it.subtitle.lowercase().contains(trimmedQuery) ||
                    it.formulaOrCategory.lowercase().contains(trimmedQuery)
        }
    }

    val matchedBankQuestions = remember(trimmedQuery, bankQuestions) {
        if (trimmedQuery.isBlank()) bankQuestions.take(20)
        else bankQuestions.filter {
            it.questionText.lowercase().contains(trimmedQuery) ||
                    it.subjectName.lowercase().contains(trimmedQuery) ||
                    it.topicTitle.lowercase().contains(trimmedQuery) ||
                    (it.subtopicTitle?.lowercase()?.contains(trimmedQuery) == true) ||
                    (it.explanation?.lowercase()?.contains(trimmedQuery) == true) ||
                    it.difficulty.lowercase().contains(trimmedQuery)
        }
    }

    val totalCount = matchedNotes.size + matchedTopics.size + matchedTasks.size +
            matchedSubjects.size + matchedSchedule.size + matchedWeeklyClasses.size +
            matchedDecks.size + matchedFlashcards.size +
            matchedSimulations.size + matchedMath3D.size + matchedBankQuestions.size

    val results = remember(
        selectedCategory,
        matchedNotes, matchedTopics, matchedTasks, matchedSubjects,
        matchedSchedule, matchedWeeklyClasses, matchedDecks, matchedFlashcards,
        matchedSimulations, matchedMath3D, matchedBankQuestions
    ) {
        val list = mutableListOf<SearchResultItem>()
        when (selectedCategory) {
            SearchCategory.ALL -> {
                matchedNotes.forEach { list.add(SearchResultItem.NoteResult(it)) }
                matchedTopics.forEach { topic ->
                    val subj = allSubjects.find { it.id == topic.subjectId }
                    list.add(SearchResultItem.TopicResult(topic, subj))
                }
                matchedTasks.forEach { list.add(SearchResultItem.TaskResult(it)) }
                matchedSubjects.forEach { list.add(SearchResultItem.SubjectResult(it)) }
                matchedSchedule.forEach { list.add(SearchResultItem.ScheduleResult(it)) }
                matchedWeeklyClasses.forEach { list.add(SearchResultItem.WeeklyClassResult(it)) }
                matchedDecks.forEach { deck ->
                    val cardCount = allFlashcards.count { it.deckId == deck.id }
                    list.add(SearchResultItem.FlashcardDeckResult(deck, cardCount))
                }
                matchedSimulations.forEach { list.add(SearchResultItem.SimulationResult(it)) }
                matchedMath3D.forEach { list.add(SearchResultItem.Math3DResult(it)) }
                matchedBankQuestions.forEach { list.add(SearchResultItem.BankQuestionResult(it)) }
                matchedFlashcards.forEach { card ->
                    val deck = allDecks.find { it.id == card.deckId }
                    list.add(SearchResultItem.FlashcardResult(card, deck?.title))
                }
            }
            SearchCategory.NOTES -> matchedNotes.forEach { list.add(SearchResultItem.NoteResult(it)) }
            SearchCategory.TOPICS -> matchedTopics.forEach { topic ->
                val subj = allSubjects.find { it.id == topic.subjectId }
                list.add(SearchResultItem.TopicResult(topic, subj))
            }
            SearchCategory.TASKS -> matchedTasks.forEach { list.add(SearchResultItem.TaskResult(it)) }
            SearchCategory.SUBJECTS -> matchedSubjects.forEach { list.add(SearchResultItem.SubjectResult(it)) }
            SearchCategory.SCHEDULE -> {
                matchedSchedule.forEach { list.add(SearchResultItem.ScheduleResult(it)) }
                matchedWeeklyClasses.forEach { list.add(SearchResultItem.WeeklyClassResult(it)) }
            }
            SearchCategory.FLASHCARDS -> {
                matchedDecks.forEach { deck ->
                    val cardCount = allFlashcards.count { it.deckId == deck.id }
                    list.add(SearchResultItem.FlashcardDeckResult(deck, cardCount))
                }
                matchedFlashcards.forEach { card ->
                    val deck = allDecks.find { it.id == card.deckId }
                    list.add(SearchResultItem.FlashcardResult(card, deck?.title))
                }
            }
            SearchCategory.SIMULATIONS -> {
                matchedSimulations.forEach { list.add(SearchResultItem.SimulationResult(it)) }
                matchedMath3D.forEach { list.add(SearchResultItem.Math3DResult(it)) }
            }
            SearchCategory.PRACTICE -> {
                matchedBankQuestions.forEach { list.add(SearchResultItem.BankQuestionResult(it)) }
            }
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(820.dp)
                .heightIn(min = 440.dp, max = 620.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ── Search Input Row ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(LernspaceSurfaceLow)
                        .border(1.dp, Color(0xFFE2E4E8), CircleShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF45474C),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = LernspaceTextPrimary,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(LernspaceDark),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari di seluruh sistem: catatan, silabus, tugas, jadwal, lab, kuis...",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = Color(0xFF888B92)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Touch-friendly Close Icon Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF45474C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Category Filter Pills ─────────────────────────────────────
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(SearchCategory.values()) { category ->
                        val isSelected = selectedCategory == category
                        val count = when (category) {
                            SearchCategory.ALL -> totalCount
                            SearchCategory.NOTES -> matchedNotes.size
                            SearchCategory.TOPICS -> matchedTopics.size
                            SearchCategory.TASKS -> matchedTasks.size
                            SearchCategory.SUBJECTS -> matchedSubjects.size
                            SearchCategory.SCHEDULE -> matchedSchedule.size + matchedWeeklyClasses.size
                            SearchCategory.FLASHCARDS -> matchedDecks.size + matchedFlashcards.size
                            SearchCategory.SIMULATIONS -> matchedSimulations.size + matchedMath3D.size
                            SearchCategory.PRACTICE -> matchedBankQuestions.size
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedCategory = category }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF45474C)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFE0E3E5)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF1E293B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFF1F5F9)
                )

                // ── Results Area ──────────────────────────────────────────────
                if (selectedCategory == SearchCategory.ALL && searchQuery.isBlank()) {
                    // "Semua" + Empty query: Quick overview & popular recommendations
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val suggestedQueries = remember(allTopics.size, allSubjects.size) {
                            (allTopics.take(4).map { it.title } +
                                    allSubjects.take(2).map { it.name } +
                                    listOf("Benda Putar", "Termodinamika", "Flashcard")).distinct()
                        }

                        if (suggestedQueries.isNotEmpty()) {
                            Text(
                                text = "PENCARIAN CEPAT & TOPIK POPULER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                suggestedQueries.forEach { suggestion ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LernspaceSurfaceLow,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { searchQuery = suggestion }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                contentDescription = null,
                                                tint = AccentTeal,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = suggestion,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = LernspaceDark
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Text(
                            text = "MODUL & KONTEN TERBARU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (allSimulations.isNotEmpty()) {
                                item {
                                    SimulationResultCard(
                                        sim = allSimulations.first(),
                                        onClick = {
                                            MimoLabActiveSession.activeFloatingSimulation = allSimulations.first()
                                            onNavigate(Screen.MimoLab.route)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                            if (allDecks.isNotEmpty()) {
                                item {
                                    val deck = allDecks.first()
                                    val cardCount = allFlashcards.count { it.deckId == deck.id }
                                    FlashcardDeckResultCard(
                                        deck = deck,
                                        cardCount = cardCount,
                                        onClick = {
                                            PracticeHubState.selectedTab = PracticeTab.FLASHCARDS
                                            onNavigate(Screen.Flashcards.route)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                            items(allNotes.take(2)) { note ->
                                NoteResultCard(
                                    note = note,
                                    onClick = {
                                        AcademicWorkspaceState.openNoteEditor(note)
                                        onDismiss()
                                    }
                                )
                            }
                            items(allTasks.take(1)) { task ->
                                TaskResultCard(
                                    task = task,
                                    onClick = {
                                        AcademicWorkspaceState.selectedPlannerTab = 1
                                        onNavigate(Screen.Planner.route)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                } else if (results.isEmpty()) {
                    // No matches state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Tidak ada hasil untuk \"$searchQuery\"" else "Tidak ada item di kategori ${selectedCategory.label}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Coba kata kunci lain atau pilih tab kategori yang berbeda.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    // Direct category listing & filtered search results
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "DAFTAR ${selectedCategory.label.uppercase()} (${results.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(results) { item ->
                                when (item) {
                                    is SearchResultItem.NoteResult -> {
                                        NoteResultCard(
                                            note = item.note,
                                            onClick = {
                                                AcademicWorkspaceState.openNoteEditor(item.note)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.TopicResult -> {
                                        TopicResultCard(
                                            topic = item.topic,
                                            subject = item.subject,
                                            onClick = {
                                                AcademicWorkspaceState.selectedLearningSubjectId = item.topic.subjectId
                                                onNavigate(Screen.Learning.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.TaskResult -> {
                                        TaskResultCard(
                                            task = item.task,
                                            onClick = {
                                                AcademicWorkspaceState.selectedPlannerTab = 1
                                                onNavigate(Screen.Planner.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.SubjectResult -> {
                                        SubjectResultCard(
                                            subject = item.subject,
                                            onClick = {
                                                AcademicWorkspaceState.selectedLearningSubjectId = item.subject.id
                                                onNavigate(Screen.Learning.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.ScheduleResult -> {
                                        ScheduleResultCard(
                                            item = item.item,
                                            onClick = {
                                                AcademicWorkspaceState.selectedPlannerTab = 0
                                                onNavigate(Screen.Planner.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.WeeklyClassResult -> {
                                        WeeklyClassResultCard(
                                            weeklyClass = item.weeklyClass,
                                            onClick = {
                                                AcademicWorkspaceState.selectedPlannerTab = 0
                                                onNavigate(Screen.Planner.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.FlashcardDeckResult -> {
                                        FlashcardDeckResultCard(
                                            deck = item.deck,
                                            cardCount = item.cardCount,
                                            onClick = {
                                                PracticeHubState.selectedTab = PracticeTab.FLASHCARDS
                                                onNavigate(Screen.Flashcards.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.FlashcardResult -> {
                                        FlashcardResultCard(
                                            card = item.card,
                                            deckTitle = item.deckTitle,
                                            onClick = {
                                                PracticeHubState.selectedTab = PracticeTab.FLASHCARDS
                                                onNavigate(Screen.Flashcards.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.SimulationResult -> {
                                        SimulationResultCard(
                                            sim = item.sim,
                                            onClick = {
                                                MimoLabActiveSession.activeFloatingSimulation = item.sim
                                                onNavigate(Screen.MimoLab.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.Math3DResult -> {
                                        Math3DResultCard(
                                            item = item.item,
                                            onClick = {
                                                onNavigate(Screen.Math3DLab.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.BankQuestionResult -> {
                                        BankQuestionResultCard(
                                            question = item.question,
                                            onClick = {
                                                PracticeHubState.selectedTab = PracticeTab.BANK_SOAL
                                                onNavigate(Screen.BankSoal.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteResultCard(
    note: NoteUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EditNote,
                    contentDescription = null,
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = note.displaySnippet,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = "Catatan • ${note.subject}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TopicResultCard(
    topic: TopicUi,
    subject: SubjectUi?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentTealContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = topic.description,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = topic.status.chipBg
            ) {
                Text(
                    text = "${subject?.code ?: "Silabus"} • ${topic.status.label}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = topic.status.chipFg,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TaskResultCard(
    task: TaskUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (task.priority == "HIGH") AccentRedContainer else Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Checklist,
                    contentDescription = null,
                    tint = if (task.priority == "HIGH") AccentRed else Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Deadline: ${task.dueDate} • ${task.subjectName ?: ""}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = if (task.priority == "HIGH") AccentRedContainer else Color(0xFFE2E8F0)
            ) {
                Text(
                    text = "Tugas • ${task.priority}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.priority == "HIGH") AccentRed else Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SubjectResultCard(
    subject: SubjectUi,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(subject.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = subject.color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${subject.code}: ${subject.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subject.description,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = "Mata Kuliah",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ScheduleResultCard(
    item: PlannerItemUi,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.startTime} - ${item.endTime} • ${item.locationOrRoom ?: (item.subjectName ?: "")}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = "Jadwal • ${item.type.name}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun WeeklyClassResultCard(
    weeklyClass: WeeklyClassUi,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${weeklyClass.subjectCode}: ${weeklyClass.subjectName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                val dayLabel = when (weeklyClass.dayOfWeek) {
                    1 -> "Senin"
                    2 -> "Selasa"
                    3 -> "Rabu"
                    4 -> "Kamis"
                    5 -> "Jumat"
                    6 -> "Sabtu"
                    else -> "Minggu"
                }
                Text(
                    text = "$dayLabel • ${weeklyClass.startTime} - ${weeklyClass.endTime} • ${weeklyClass.room ?: "Ruang Kelas"} • ${weeklyClass.lecturer ?: "Dosen"}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFFEF3C7)
            ) {
                Text(
                    text = "KRS • ${weeklyClass.type}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FlashcardDeckResultCard(
    deck: FlashcardDeckUi,
    cardCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Style,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${deck.description.ifBlank { "Dek kartu Active Recall" }} • ${deck.subjectName}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFEDE9FE)
            ) {
                Text(
                    text = "$cardCount Kartu",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4F46E5),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FlashcardResultCard(
    card: FlashcardUi,
    deckTitle: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF3E8FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Style,
                    contentDescription = null,
                    tint = Color(0xFF9333EA),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.front,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Jawaban: ${card.back}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = "Flashcard • ${deckTitle ?: "Umum"}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SimulationResultCard(
    sim: MimoSimulation,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Functions,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sim.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${sim.conceptTopic} • ${sim.description}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFE0F2FE)
            ) {
                Text(
                    text = "MimoLab • ${sim.subject.chipLabel}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0369A1),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun Math3DResultCard(
    item: Math3DPresetItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFCCFBF1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ViewInAr,
                    contentDescription = null,
                    tint = Color(0xFF0D9488),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFCCFBF1)
            ) {
                Text(
                    text = "GeoNix • ${item.formulaOrCategory}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun BankQuestionResultCard(
    question: BankQuestion,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFE4E6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderCopy,
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = question.questionText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${question.subjectName} • ${question.topicTitle} • ${question.options.size} Pilihan",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFFFE4E6)
            ) {
                Text(
                    text = "Bank Soal • ${question.difficulty}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBE123C),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
