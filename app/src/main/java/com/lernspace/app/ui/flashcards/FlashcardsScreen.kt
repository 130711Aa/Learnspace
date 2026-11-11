package com.lernspace.app.ui.flashcards

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lernspace.app.core.ai.GeminiApiClient
import com.lernspace.app.core.ai.GeneratedFlashcard
import com.lernspace.app.core.ai.HybridStudyAiRouter
import com.lernspace.app.core.ai.LocalSlmManager
import com.lernspace.app.core.ai.SlmModelSpec
import com.lernspace.app.data.repository.*
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.model.NoteUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen() {
    val decks = AcademicWorkspaceState.flashcardDecks
    val allCards = AcademicWorkspaceState.flashcards
    val subjects = AcademicWorkspaceState.subjects
    val topics = AcademicWorkspaceState.topics
    val notes = AcademicWorkspaceState.notes

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "DUE", or subjectId
    var searchQuery by remember { mutableStateOf("") }
    var selectedDeckId by remember { mutableStateOf<String?>(null) }

    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<FlashcardUi?>(null) }
    var showSlmGenerateDialog by remember { mutableStateOf(false) }

    // Active study session state
    var studyDeck by remember { mutableStateOf<FlashcardDeckUi?>(null) }
    var studyCards by remember { mutableStateOf<List<FlashcardUi>>(emptyList()) }

    val nowMs = System.currentTimeMillis()
    val totalDueCount = allCards.count { it.nextReviewAt <= nowMs }

    val filteredDecks = decks.filter { deck ->
        val deckCards = allCards.filter { it.deckId == deck.id }
        val dueInDeck = deckCards.count { it.nextReviewAt <= nowMs }

        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "DUE" -> dueInDeck > 0
            else -> deck.subjectId == selectedFilter
        }
        val matchesSearch = searchQuery.isBlank() ||
                deck.title.contains(searchQuery, ignoreCase = true) ||
                deck.subjectName.contains(searchQuery, ignoreCase = true) ||
                (deck.topicName?.contains(searchQuery, ignoreCase = true) == true) ||
                deckCards.any {
                    it.front.contains(searchQuery, ignoreCase = true) ||
                            it.back.contains(searchQuery, ignoreCase = true)
                }

        matchesFilter && matchesSearch
    }

    // Keep selectedDeckId valid
    LaunchedEffect(filteredDecks.size, decks.size) {
        if (selectedDeckId == null || decks.none { it.id == selectedDeckId }) {
            selectedDeckId = filteredDecks.firstOrNull()?.id ?: decks.firstOrNull()?.id
        }
    }

    val activeDeck = decks.find { it.id == selectedDeckId } ?: filteredDecks.firstOrNull()
    val activeDeckCards = if (activeDeck != null) {
        allCards.filter { it.deckId == activeDeck.id }
    } else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Header Bar ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Flashcards",
                    style = MaterialTheme.typography.headlineLarge,
                    color = LernspaceDark,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LernspaceSurfaceLow,
                    border = BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Text(
                        text = "${decks.size} Deck",
                        style = MaterialTheme.typography.labelSmall,
                        color = LernspaceTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (totalDueCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "$totalDueCount Perlu Review",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showSlmGenerateDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = AccentBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate dari Catatan", fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                }

                Button(
                    onClick = { showCreateDeckDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Deck", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Search & Filter Row ───────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari deck, mata kuliah, atau kartu...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, null, tint = LernspaceTextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LernspaceDark,
                    unfocusedBorderColor = LernspaceBorder,
                    focusedContainerColor = LernspaceSurfaceCard,
                    unfocusedContainerColor = LernspaceSurfaceLow
                ),
                modifier = Modifier
                    .width(320.dp)
                    .height(48.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Semua (${decks.size})", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "DUE",
                        onClick = { selectedFilter = "DUE" },
                        leadingIcon = {
                            Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(13.dp))
                        },
                        label = { Text("Perlu Review ($totalDueCount)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                items(subjects, key = { it.id }) { subject ->
                    val count = decks.count { it.subjectId == subject.id }
                    FilterChip(
                        selected = selectedFilter == subject.id,
                        onClick = { selectedFilter = subject.id },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(subject.color)
                            )
                        },
                        label = { Text("${subject.name} ($count)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // ── Main 2-Column Tablet Viewport ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Left Column: Deck List ────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .weight(0.78f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = LernspaceSurfaceCard,
                border = BorderStroke(1.dp, LernspaceBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Decks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        Text(
                            text = "${filteredDecks.size} decks",
                            style = MaterialTheme.typography.labelMedium,
                            color = LernspaceTextSecondary
                        )
                    }

                    if (filteredDecks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Style,
                                    contentDescription = null,
                                    tint = LernspaceTextSecondary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = if (decks.isEmpty()) "Belum ada deck flashcard" else "Tidak ada deck yang cocok",
                                    fontWeight = FontWeight.SemiBold,
                                    color = LernspaceDark,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Buat deck baru untuk mata kuliah atau sub-bab, lalu tambahkan kartu belajar.",
                                    color = LernspaceTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredDecks, key = { it.id }) { deck ->
                                val deckCards = allCards.filter { it.deckId == deck.id }
                                val dueCount = deckCards.count { it.nextReviewAt <= nowMs }
                                val masteredCount = deckCards.count { it.repetitions >= 2 }
                                val subjectColor = subjects.find { it.id == deck.subjectId }?.color ?: AccentBlue

                                DeckListItem(
                                    deck = deck,
                                    subjectColor = subjectColor,
                                    totalCards = deckCards.size,
                                    dueCards = dueCount,
                                    masteredCards = masteredCount,
                                    isSelected = activeDeck?.id == deck.id,
                                    onClick = { selectedDeckId = deck.id },
                                    onDelete = {
                                        AcademicWorkspaceState.deleteFlashcardDeck(deck.id)
                                        if (selectedDeckId == deck.id) {
                                            selectedDeckId = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── Right Column: Selected Deck Detail & Cards ────────────────────
            Surface(
                modifier = Modifier
                    .weight(1.22f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = LernspaceSurfaceCard,
                border = BorderStroke(1.dp, LernspaceBorder),
                shadowElevation = 1.dp
            ) {
                if (activeDeck == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Style,
                                contentDescription = null,
                                tint = LernspaceTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Pilih atau Buat Deck Flashcard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Text(
                                text = "Gunakan Spaced Repetition (SM-2) untuk mengingat materi catatan & sub-bab lebih lama.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LernspaceTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { showCreateDeckDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buat Deck Pertama")
                            }
                        }
                    }
                } else {
                    val subjectColor = subjects.find { it.id == activeDeck.subjectId }?.color ?: AccentBlue
                    val dueCards = activeDeckCards.filter { it.nextReviewAt <= nowMs }
                    val masteredCards = activeDeckCards.count { it.repetitions >= 2 }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Deck Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = subjectColor.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(subjectColor)
                                            )
                                            Text(
                                                text = activeDeck.subjectName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = subjectColor
                                            )
                                        }
                                    }
                                    if (!activeDeck.topicName.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = LernspaceSurfaceLow
                                        ) {
                                            Text(
                                                text = activeDeck.topicName,
                                                fontSize = 11.sp,
                                                color = LernspaceTextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = activeDeck.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark
                                )
                                if (activeDeck.description.isNotBlank()) {
                                    Text(
                                        text = activeDeck.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LernspaceTextSecondary
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showAddCardDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah Kartu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    enabled = activeDeckCards.isNotEmpty(),
                                    onClick = {
                                        studyDeck = activeDeck
                                        studyCards = if (dueCards.isNotEmpty()) dueCards else activeDeckCards
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (dueCards.isNotEmpty()) AccentBlue else LernspaceDark
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (dueCards.isNotEmpty()) "Review (${dueCards.size})" else "Latihan Semua",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Deck Summary Metrics Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DeckMetricChip(
                                label = "Total Kartu",
                                value = "${activeDeckCards.size}",
                                accent = LernspaceDark,
                                modifier = Modifier.weight(1f)
                            )
                            DeckMetricChip(
                                label = "Perlu Review",
                                value = "${dueCards.size}",
                                accent = if (dueCards.isNotEmpty()) AccentAmber else LernspaceTextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            DeckMetricChip(
                                label = "Dikuasai",
                                value = "$masteredCards",
                                accent = AccentTeal,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = LernspaceBorder)

                        // Cards List
                        if (activeDeckCards.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Quiz,
                                        contentDescription = null,
                                        tint = LernspaceTextSecondary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Text(
                                        text = "Deck ini masih kosong",
                                        fontWeight = FontWeight.SemiBold,
                                        color = LernspaceDark
                                    )
                                    Text(
                                        text = "Tambahkan kartu secara manual atau siapkan ekstraksi dari catatan.",
                                        fontSize = 12.sp,
                                        color = LernspaceTextSecondary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { showAddCardDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                                        ) {
                                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Tambah Kartu")
                                        }
                                        OutlinedButton(
                                            onClick = { showSlmGenerateDialog = true },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = AccentBlue)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Dari Catatan")
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(activeDeckCards, key = { it.id }) { card ->
                                    FlashcardRowItem(
                                        card = card,
                                        isDue = card.nextReviewAt <= nowMs,
                                        onEdit = { cardToEdit = card },
                                        onDelete = { AcademicWorkspaceState.deleteFlashcard(card.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Fullscreen Flip-Card Review Session ───────────────────────────────────
    if (studyDeck != null && studyCards.isNotEmpty()) {
        FlashcardStudyDialog(
            deck = studyDeck!!,
            initialCards = studyCards,
            onReviewCard = { cardId, rating ->
                AcademicWorkspaceState.reviewFlashcard(cardId, rating)
            },
            onDismiss = {
                studyDeck = null
                studyCards = emptyList()
            }
        )
    }

    // ── Create Deck Dialog ────────────────────────────────────────────────────
    if (showCreateDeckDialog) {
        CreateFlashcardDeckDialog(
            subjects = subjects,
            topics = topics,
            notes = notes,
            onDismiss = { showCreateDeckDialog = false },
            onConfirm = { title, desc, subject, topic, note ->
                val newDeck = FlashcardDeckUi(
                    title = title,
                    description = desc,
                    subjectId = subject?.id,
                    subjectName = subject?.name ?: "General Study",
                    topicId = topic?.id,
                    topicName = topic?.title,
                    sourceNoteId = note?.id
                )
                AcademicWorkspaceState.addFlashcardDeck(newDeck)
                selectedDeckId = newDeck.id
                showCreateDeckDialog = false
            }
        )
    }

    // ── Add / Edit Card Dialog ────────────────────────────────────────────────
    if (showAddCardDialog && activeDeck != null) {
        AddEditFlashcardDialog(
            existingCard = null,
            onDismiss = { showAddCardDialog = false },
            onConfirm = { front, back, hint ->
                AcademicWorkspaceState.addFlashcard(
                    FlashcardUi(
                        deckId = activeDeck.id,
                        front = front,
                        back = back,
                        hint = hint
                    )
                )
                showAddCardDialog = false
            }
        )
    }

    if (cardToEdit != null) {
        AddEditFlashcardDialog(
            existingCard = cardToEdit,
            onDismiss = { cardToEdit = null },
            onConfirm = { front, back, hint ->
                AcademicWorkspaceState.updateFlashcard(
                    cardToEdit!!.copy(front = front, back = back, hint = hint)
                )
                cardToEdit = null
            }
        )
    }

    // ── SLM Generate From Notes / Topic / Text Dialog ─────────────────────────
    if (showSlmGenerateDialog) {
        GenerateFlashcardsFromNoteDialog(
            subjects = subjects,
            topics = topics,
            notes = notes,
            targetDeck = activeDeck,
            onDismiss = { showSlmGenerateDialog = false },
            onImportGenerated = { deckTitle, subject, topic, note, cards ->
                val target = activeDeck ?: FlashcardDeckUi(
                    title = deckTitle,
                    description = "Dibuat otomatis dari ${note?.title ?: topic?.title ?: "Materi Belajar"}",
                    subjectId = subject?.id,
                    subjectName = subject?.name ?: "General Study",
                    topicId = topic?.id,
                    topicName = topic?.title,
                    sourceNoteId = note?.id
                ).also {
                    AcademicWorkspaceState.addFlashcardDeck(it)
                    selectedDeckId = it.id
                }

                cards.forEach { card ->
                    AcademicWorkspaceState.addFlashcard(
                        FlashcardUi(
                            deckId = target.id,
                            front = card.front,
                            back = card.back,
                            hint = card.hint
                        )
                    )
                }
                showSlmGenerateDialog = false
            }
        )
    }
}

@Composable
private fun DeckListItem(
    deck: FlashcardDeckUi,
    subjectColor: Color,
    totalCards: Int,
    dueCards: Int,
    masteredCards: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val masteryFraction = if (totalCards > 0) masteredCards.toFloat() / totalCards else 0f

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) LernspaceSurfaceLow else LernspaceSurfaceCard,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) LernspaceDark else LernspaceBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(subjectColor)
                    )
                    Text(
                        text = deck.subjectName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subjectColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dueCards > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "$dueCards due",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Filled.MoreVert, null, tint = LernspaceTextSecondary, modifier = Modifier.size(15.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Hapus Deck", color = AccentRed) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = AccentRed) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = deck.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalCards kartu • $masteredCards dikuasai",
                    fontSize = 11.sp,
                    color = LernspaceTextSecondary
                )
                Text(
                    text = "${(masteryFraction * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
            }

            LinearProgressIndicator(
                progress = { masteryFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = subjectColor,
                trackColor = LernspaceBorder
            )
        }
    }
}

@Composable
private fun DeckMetricChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LernspaceSurfaceLow,
        border = BorderStroke(1.dp, LernspaceBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, fontSize = 11.sp, color = LernspaceTextSecondary)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun FlashcardRowItem(
    card: FlashcardUi,
    isDue: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusLabel = when {
        card.repetitions >= 2 -> "Dikuasai"
        card.repetitions == 1 -> "Belajar"
        else -> "Baru"
    }
    val statusBg = when {
        card.repetitions >= 2 -> Color(0xFFD1FAE5)
        card.repetitions == 1 -> Color(0xFFDBEAFE)
        else -> Color(0xFFF1F5F9)
    }
    val statusFg = when {
        card.repetitions >= 2 -> Color(0xFF059669)
        card.repetitions == 1 -> Color(0xFF1D4ED8)
        else -> Color(0xFF475569)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = BorderStroke(1.dp, LernspaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Front
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PERTANYAAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                Text(
                    text = card.front,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                if (card.hint.isNotBlank()) {
                    Text(
                        text = "Hint: ${card.hint}",
                        fontSize = 11.sp,
                        color = LernspaceTextSecondary
                    )
                }
            }

            VerticalDivider(modifier = Modifier.height(48.dp), color = LernspaceBorder)

            // Back
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("JAWABAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                Text(
                    text = card.back,
                    fontSize = 13.sp,
                    color = LernspaceTextPrimary
                )
            }

            // Status & Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isDue) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                            Text(
                                text = "Due",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, null, tint = LernspaceTextSecondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, null, tint = AccentRed, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardStudyDialog(
    deck: FlashcardDeckUi,
    initialCards: List<FlashcardUi>,
    onReviewCard: (String, FlashcardRating) -> Unit,
    onDismiss: () -> Unit
) {
    val queue = remember { mutableStateListOf<FlashcardUi>().apply { addAll(initialCards) } }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var goodCount by remember { mutableIntStateOf(0) }
    var againCount by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current.density
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "flashcard_flip"
    )

    val isFinished = currentIndex >= queue.size
    val currentCard = if (!isFinished) queue[currentIndex] else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .fillMaxHeight(0.84f),
            shape = RoundedCornerShape(24.dp),
            color = LernspaceSurface,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header & Progress
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = deck.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Text(
                                text = if (isFinished) "Sesi Selesai" else "Kartu ${currentIndex + 1} dari ${queue.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = LernspaceTextSecondary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = LernspaceDark)
                        }
                    }

                    LinearProgressIndicator(
                        progress = {
                            if (queue.isEmpty()) 1f
                            else (currentIndex.toFloat() / queue.size.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentBlue,
                        trackColor = LernspaceBorder
                    )
                }

                // Center Card or Completion View
                if (isFinished) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentTeal.copy(alpha = 0.14f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CheckCircle, null, tint = AccentTeal, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Review Sesi Selesai!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$goodCount kartu diingat dengan baik • $againCount perlu diulang",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LernspaceTextSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    queue.clear()
                                    queue.addAll(initialCards)
                                    currentIndex = 0
                                    isFlipped = false
                                    showHint = false
                                    goodCount = 0
                                    againCount = 0
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ulangi Sesi")
                            }
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                            ) {
                                Text("Kembali ke Deck")
                            }
                        }
                    }
                } else if (currentCard != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = LernspaceSurfaceCard,
                            border = BorderStroke(
                                1.5.dp,
                                if (rotation > 90f) AccentBlue.copy(alpha = 0.45f) else LernspaceBorder
                            ),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 14f * density
                                }
                                .clickable { isFlipped = !isFlipped }
                        ) {
                            if (rotation <= 90f) {
                                // Front Side
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LernspaceSurfaceLow
                                    ) {
                                        Text(
                                            text = "PERTANYAAN",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceTextSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = currentCard.front,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LernspaceDark,
                                            textAlign = TextAlign.Center
                                        )
                                        if (currentCard.hint.isNotBlank()) {
                                            if (showHint) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFFEF3C7)
                                                ) {
                                                    Text(
                                                        text = "Hint: ${currentCard.hint}",
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF92400E),
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                    )
                                                }
                                            } else {
                                                TextButton(onClick = { showHint = true }) {
                                                    Icon(Icons.Filled.Lightbulb, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Tampilkan Hint")
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Ketuk kartu untuk melihat jawaban",
                                        fontSize = 12.sp,
                                        color = LernspaceTextSecondary
                                    )
                                }
                            } else {
                                // Back Side (counter-mirrored so text reads normally)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AccentBlue.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "JAWABAN",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Text(
                                        text = currentCard.back,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = LernspaceDark,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "Seberapa mudah kamu mengingatnya?",
                                        fontSize = 12.sp,
                                        color = LernspaceTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Action Controls
                    if (!isFlipped) {
                        Button(
                            onClick = { isFlipped = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Lihat Jawaban", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val ratings = listOf(
                                Triple(FlashcardRating.AGAIN, Color(0xFFFEE2E2), Color(0xFFDC2626)),
                                Triple(FlashcardRating.HARD, Color(0xFFFEF3C7), Color(0xFFD97706)),
                                Triple(FlashcardRating.GOOD, Color(0xFFDBEAFE), Color(0xFF2563EB)),
                                Triple(FlashcardRating.EASY, Color(0xFFD1FAE5), Color(0xFF059669))
                            )
                            ratings.forEach { (rating, bg, fg) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bg,
                                    border = BorderStroke(1.dp, fg.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onReviewCard(currentCard.id, rating)
                                            if (rating == FlashcardRating.AGAIN) {
                                                againCount++
                                            } else {
                                                goodCount++
                                            }
                                            isFlipped = false
                                            showHint = false
                                            currentIndex++
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(rating.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
                                        Text(
                                            text = previewIntervalLabel(currentCard, rating),
                                            fontSize = 11.sp,
                                            color = fg.copy(alpha = 0.85f)
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
private fun CreateFlashcardDeckDialog(
    subjects: List<SubjectUi>,
    topics: List<TopicUi>,
    notes: List<NoteUiModel>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, SubjectUi?, TopicUi?, NoteUiModel?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var selectedTopic by remember { mutableStateOf<TopicUi?>(null) }
    var selectedNote by remember { mutableStateOf<NoteUiModel?>(null) }

    val subjectTopics = remember(selectedSubject) {
        if (selectedSubject != null) topics.filter { it.subjectId == selectedSubject!!.id } else emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.width(500.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Buat Deck Flashcard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Deck") },
                    placeholder = { Text("e.g. Anatomi Sistem Saraf") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi (Opsional)") },
                    placeholder = { Text("Ringkasan materi atau fokus ujian") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (subjects.isNotEmpty()) {
                    Text("Mata Kuliah:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = {
                                    selectedSubject = subj
                                    selectedTopic = null
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(subj.color)
                                    )
                                },
                                label = { Text(subj.name, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                if (subjectTopics.isNotEmpty()) {
                    Text("Sub-bab / Topik (Opsional):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjectTopics) { top ->
                            FilterChip(
                                selected = selectedTopic == top,
                                onClick = {
                                    selectedTopic = if (selectedTopic == top) null else top
                                    if (title.isBlank() && selectedTopic != null) title = top.title
                                },
                                label = { Text(top.title, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                if (notes.isNotEmpty()) {
                    Text("Tautkan ke Catatan (Opsional):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(notes.take(10)) { note ->
                            FilterChip(
                                selected = selectedNote == note,
                                onClick = {
                                    selectedNote = if (selectedNote == note) null else note
                                    if (title.isBlank() && selectedNote != null) title = note.title
                                },
                                label = { Text(note.title, fontSize = 11.sp, maxLines = 1) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = title.isNotBlank(),
                        onClick = {
                            onConfirm(title.trim(), description.trim(), selectedSubject, selectedTopic, selectedNote)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                    ) {
                        Text("Simpan Deck")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditFlashcardDialog(
    existingCard: FlashcardUi?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var front by remember { mutableStateOf(existingCard?.front ?: "") }
    var back by remember { mutableStateOf(existingCard?.back ?: "") }
    var hint by remember { mutableStateOf(existingCard?.hint ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.width(480.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingCard == null) "Tambah Kartu Baru" else "Edit Kartu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Depan (Pertanyaan / Istilah)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Belakang (Jawaban / Penjelasan)") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Hint / Petunjuk (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = front.isNotBlank() && back.isNotBlank(),
                        onClick = { onConfirm(front.trim(), back.trim(), hint.trim()) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                    ) {
                        Text(if (existingCard == null) "Tambah Kartu" else "Simpan")
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerateFlashcardsFromNoteDialog(
    subjects: List<SubjectUi>,
    topics: List<TopicUi>,
    notes: List<NoteUiModel>,
    targetDeck: FlashcardDeckUi?,
    onDismiss: () -> Unit,
    onImportGenerated: (String, SubjectUi?, TopicUi?, NoteUiModel?, List<GeneratedFlashcard>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sourceMode by remember { mutableIntStateOf(if (notes.isNotEmpty()) 0 else 2) } // 0 = Catatan, 1 = Sub-bab, 2 = Teks Manual
    var selectedNote by remember { mutableStateOf(notes.firstOrNull()) }
    var selectedTopic by remember { mutableStateOf(topics.firstOrNull()) }
    var manualMaterialText by remember { mutableStateOf("") }
    var cardCount by remember { mutableIntStateOf(6) }

    var isGenerating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val generatedCards = remember { mutableStateListOf<GeneratedFlashcard>() }
    val selectedFlags = remember { mutableStateListOf<Boolean>() }

    val hasSlmReady = LocalSlmManager.hasReadyModel(context)
    val hasGeminiReady = GeminiApiClient.isConfigured() && GeminiApiClient.preferGeminiWhenOnline
    val hasAnyAi = hasSlmReady || hasGeminiReady
    val isDownloading = LocalSlmManager.isDownloading
    var activeProviderResult by remember { mutableStateOf<String?>(null) }
    var activeGenJob by remember { mutableStateOf<Job?>(null) }

    val notePlainText = remember(selectedNote) {
        selectedNote?.pages
            ?.flatMap { it.textBoxes }
            ?.map { it.text.trim() }
            ?.filter { it.isNotBlank() }
            ?.joinToString("\n")
            ?: ""
    }

    val topicMaterialText = remember(selectedTopic, notes) {
        val t = selectedTopic ?: return@remember ""
        val linkedNotesText = notes.filter { it.topicId == t.id }
            .flatMap { it.pages }
            .flatMap { it.textBoxes }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
        buildString {
            appendLine("Sub-bab: ${t.title}")
            if (t.description.isNotBlank()) appendLine("Deskripsi: ${t.description}")
            if (linkedNotesText.isNotBlank()) {
                appendLine("Catatan terkait:")
                appendLine(linkedNotesText)
            }
        }.trim()
    }

    val effectiveSourceText = when (sourceMode) {
        0 -> if (manualMaterialText.isNotBlank()) manualMaterialText else notePlainText
        1 -> if (manualMaterialText.isNotBlank()) manualMaterialText else topicMaterialText
        else -> manualMaterialText
    }

    val effectiveTopicTitle = when (sourceMode) {
        0 -> selectedNote?.title ?: "Catatan Belajar"
        1 -> selectedTopic?.title ?: "Topik Belajar"
        else -> targetDeck?.title ?: "Materi Belajar"
    }

    // Rule-based fallback extractor (`Istilah : Penjelasan`) when SLM isn't downloaded
    val ruleBasedPairs = remember(effectiveSourceText) {
        effectiveSourceText.lines()
            .mapNotNull { line ->
                val sep = when {
                    line.contains(":") -> ":"
                    line.contains("->") -> "->"
                    line.contains(" = ") -> " = "
                    else -> null
                }
                if (sep != null) {
                    val parts = line.split(sep, limit = 2)
                    val q = parts[0].trim().removePrefix("-").removePrefix("•").trim()
                    val a = parts.getOrNull(1)?.trim() ?: ""
                    if (q.length >= 2 && a.length >= 2) GeneratedFlashcard(q, a) else null
                } else null
            }
    }

    Dialog(onDismissRequest = { if (!isGenerating) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = AccentBlue)
                        Column {
                            Text(
                                text = "Generate Flashcard (AI)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Text(
                                text = when {
                                    activeProviderResult != null -> "Hasil terakhir: $activeProviderResult"
                                    hasGeminiReady && hasSlmReady -> "Engine: ${GeminiApiClient.selectedModelId} (fallback SLM: ${LocalSlmManager.activeModelFileName})"
                                    hasGeminiReady -> "Engine aktif: ${GeminiApiClient.selectedModelId} (BYOK)"
                                    hasSlmReady -> "Engine offline: ${LocalSlmManager.activeModelFileName}"
                                    else -> "Aktifkan Gemini API di Settings atau unduh model SLM offline"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasAnyAi) AccentTeal else LernspaceTextSecondary
                            )
                        }
                    }
                    IconButton(
                        enabled = !isGenerating,
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                if (!hasAnyAi) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentBlue.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Unduh model SLM ringan untuk membuat flashcard otomatis 100% offline, atau gunakan pola \"Istilah : Penjelasan\" secara manual.",
                                fontSize = 12.sp,
                                color = LernspaceDark
                            )
                            if (isDownloading) {
                                LinearProgressIndicator(
                                    progress = { LocalSlmManager.downloadProgress },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                    color = AccentBlue
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(LocalSlmManager.downloadStatusText, fontSize = 11.sp, color = LernspaceTextSecondary)
                                    TextButton(onClick = { LocalSlmManager.cancelDownload() }) {
                                        Text("Batal", fontSize = 11.sp, color = AccentRed)
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { LocalSlmManager.startModelDownload(context, SlmModelSpec.QWEN_2_5_0_5B) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Unduh Qwen 0.5B (~469 MB)", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { LocalSlmManager.startModelDownload(context, SlmModelSpec.QWEN_2_5_1_5B) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Qwen 1.5B (~1.04 GB)", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Source Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = sourceMode == 0,
                        onClick = { sourceMode = 0; manualMaterialText = "" },
                        label = { Text("Dari Catatan", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = sourceMode == 1,
                        onClick = { sourceMode = 1; manualMaterialText = "" },
                        label = { Text("Dari Sub-bab", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = sourceMode == 2,
                        onClick = { sourceMode = 2 },
                        label = { Text("Tempel Materi", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                when (sourceMode) {
                    0 -> {
                        if (notes.isEmpty()) {
                            Text("Belum ada catatan di Workspace.", fontSize = 12.sp, color = LernspaceTextSecondary)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(notes) { note ->
                                    FilterChip(
                                        selected = selectedNote == note,
                                        onClick = { selectedNote = note; manualMaterialText = "" },
                                        label = { Text(note.title, fontSize = 11.sp, maxLines = 1) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                            if (notePlainText.isBlank()) {
                                Text(
                                    text = "Catatan ini belum memiliki teks ketikan (TextBox). Tempel ringkasan materinya di bawah:",
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary
                                )
                            }
                        }
                    }
                    1 -> {
                        if (topics.isEmpty()) {
                            Text("Belum ada sub-bab di Syllabus.", fontSize = 12.sp, color = LernspaceTextSecondary)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(topics) { top ->
                                    FilterChip(
                                        selected = selectedTopic == top,
                                        onClick = { selectedTopic = top; manualMaterialText = "" },
                                        label = { Text(top.title, fontSize = 11.sp, maxLines = 1) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = if (sourceMode == 2 || effectiveSourceText.isBlank()) manualMaterialText else effectiveSourceText,
                    onValueChange = { manualMaterialText = it },
                    label = { Text("Teks Materi Sumber") },
                    placeholder = { Text("Tempel ringkasan materi atau poin kuliah di sini...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Card count selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Jumlah Kartu:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(4, 6, 8, 10).forEach { count ->
                            FilterChip(
                                selected = cardCount == count,
                                onClick = { cardCount = count },
                                label = { Text("$count", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, fontSize = 12.sp, color = AccentRed)
                }

                // Generated Preview List
                if (generatedCards.isNotEmpty()) {
                    Text(
                        text = "Hasil Kartu (${selectedFlags.count { it }} dipilih):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(generatedCards.size) { idx ->
                            val card = generatedCards[idx]
                            val checked = selectedFlags.getOrElse(idx) { true }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LernspaceSurfaceLow,
                                border = BorderStroke(1.dp, LernspaceBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (idx < selectedFlags.size) selectedFlags[idx] = !checked
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { if (idx < selectedFlags.size) selectedFlags[idx] = it },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(card.front, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                        Text(card.back, fontSize = 11.sp, color = LernspaceTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
                } // end scrollable middle body

                // Bottom Actions
                val liveStatus = when {
                    !isGenerating -> ""
                    HybridStudyAiRouter.isUsingLocalFallback || !hasGeminiReady ->
                        LocalSlmManager.statusMessage.ifBlank { HybridStudyAiRouter.statusMessage }
                    else -> HybridStudyAiRouter.statusMessage.ifBlank { "Menghubungi ${GeminiApiClient.selectedModelId}..." }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = liveStatus,
                        fontSize = 11.sp,
                        color = AccentBlue,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isGenerating) {
                            TextButton(onClick = {
                                activeGenJob?.cancel()
                                LocalSlmManager.cancelGeneration()
                                isGenerating = false
                            }) {
                                Text("Hentikan", color = AccentRed)
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("Tutup")
                            }
                        }

                        if (!hasSlmReady && ruleBasedPairs.isNotEmpty() && generatedCards.isEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    generatedCards.clear()
                                    selectedFlags.clear()
                                    generatedCards.addAll(ruleBasedPairs)
                                    repeat(ruleBasedPairs.size) { selectedFlags.add(true) }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Ekstrak Pola (${ruleBasedPairs.size})", fontSize = 12.sp)
                            }
                        }

                        if (hasAnyAi) {
                            OutlinedButton(
                                enabled = !isGenerating && effectiveSourceText.trim().length >= 15,
                                onClick = {
                                    isGenerating = true
                                    errorMsg = null
                                    activeGenJob = coroutineScope.launch {
                                        try {
                                            val res = HybridStudyAiRouter.generateFlashcards(
                                                context = context,
                                                sourceText = effectiveSourceText,
                                                topicTitle = effectiveTopicTitle,
                                                maxCards = cardCount
                                            )
                                            res.onSuccess { result ->
                                                activeProviderResult = result.providerLabel
                                                generatedCards.clear()
                                                selectedFlags.clear()
                                                generatedCards.addAll(result.cards)
                                                repeat(result.cards.size) { selectedFlags.add(true) }
                                            }.onFailure { err ->
                                                errorMsg = err.message ?: "Gagal membuat flashcard."
                                            }
                                        } catch (_: CancellationException) {
                                            errorMsg = "Pembuatan kartu dihentikan."
                                        } finally {
                                            isGenerating = false
                                            activeGenJob = null
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(15.dp), tint = AccentBlue)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = when {
                                        generatedCards.isNotEmpty() -> "Ulangi"
                                        hasGeminiReady -> "Generate Gemini"
                                        else -> "Generate SLM"
                                    },
                                    fontSize = 12.sp
                                )
                            }
                        }

                        val chosenCards = generatedCards.filterIndexed { i, _ -> selectedFlags.getOrElse(i) { false } }
                        if (chosenCards.isNotEmpty()) {
                            Button(
                                enabled = !isGenerating,
                                onClick = {
                                    val note = if (sourceMode == 0) selectedNote else null
                                    val top = when (sourceMode) {
                                        1 -> selectedTopic
                                        0 -> topics.find { it.id == note?.topicId }
                                        else -> null
                                    }
                                    val subj = subjects.find { it.id == (note?.subjectId ?: top?.subjectId) }
                                    val title = targetDeck?.title ?: "$effectiveTopicTitle — Flashcards"
                                    onImportGenerated(title, subj, top, note, chosenCards)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                            ) {
                                Text("Simpan ${chosenCards.size} Kartu", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
