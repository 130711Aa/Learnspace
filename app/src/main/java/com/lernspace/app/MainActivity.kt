package com.lernspace.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.lernspace.app.data.local.AcademicDatabaseManager
import com.lernspace.app.data.local.AppSettingsManager
import com.lernspace.app.data.local.AppThemeMode
import com.lernspace.app.data.local.NoteStorageManager
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.navigation.LernspaceTabHost
import com.lernspace.app.navigation.Screen
import com.lernspace.app.ui.components.TabletAppShell
import com.lernspace.app.ui.focus.FocusCompletionDialog
import com.lernspace.app.ui.focus.FocusFloatingPill
import com.lernspace.app.ui.focus.FocusFullscreenImmersion
import com.lernspace.app.ui.focus.FocusSetupDialog
import com.lernspace.app.ui.home.HomeViewModel
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.theme.LernspaceTheme
import com.lernspace.app.ui.workspace.NoteEditorScreen
import com.lernspace.app.ui.math3d.Math3DLabScreen
import com.lernspace.app.ui.workspace.document.DocxDocumentCodec
import com.lernspace.app.ui.workspace.model.NoteCanvasType
import com.lernspace.app.ui.workspace.model.NotePageData
import com.lernspace.app.ui.workspace.model.NoteUiModel
import com.lernspace.app.ui.workspace.model.PaperStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        val app = application as LernspaceApplication
        HomeViewModel.provideFactory(app.taskRepository, app.subjectRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        handleIncomingDocumentIntent(intent)
        setContent {
            val isDarkTheme = when (AppSettingsManager.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            LernspaceTheme(darkTheme = isDarkTheme) {
                // Instant keep-alive route state
                var currentRoute by remember { mutableStateOf(Screen.Home.route) }
                val visitedTabs = remember { mutableStateListOf(Screen.Home.route) }
                val backStack = remember { mutableStateListOf(Screen.Home.route) }

                fun navigateTo(route: String) {
                    if (route == currentRoute) return
                    if (!visitedTabs.contains(route)) {
                        visitedTabs.add(route)
                    }
                    if (route == Screen.Home.route) {
                        backStack.clear()
                        backStack.add(Screen.Home.route)
                    } else {
                        backStack.removeAll { it == route }
                        backStack.add(route)
                    }
                    currentRoute = route
                }

                // ── 1-Second Focus Session Timer Engine ─────────────────────
                LaunchedEffect(AcademicWorkspaceState.isFocusSessionActive, AcademicWorkspaceState.isFocusTimerRunning) {
                    while (AcademicWorkspaceState.isFocusSessionActive && AcademicWorkspaceState.isFocusTimerRunning) {
                        kotlinx.coroutines.delay(1000L)
                        AcademicWorkspaceState.tickFocusSecond()
                    }
                }

                val activeEditingNote = AcademicWorkspaceState.activeEditingNote
                val isMath3DLab = currentRoute == Screen.Math3DLab.route

                // ── IMMERSIVE FULL-SCREEN NOTE CANVAS & MATH 3D (Zero Rail, 100% Display, No accidental gestures) ──
                val isImmersive = activeEditingNote != null || isMath3DLab
                DisposableEffect(isImmersive) {
                    setImmersiveFullscreen(isImmersive)
                    onDispose {
                        setImmersiveFullscreen(false)
                    }
                }

                if (activeEditingNote != null) {
                    BackHandler {
                        AcademicWorkspaceState.closeNoteEditor()
                    }
                    NoteEditorScreen(
                        note = activeEditingNote,
                        onBack = { AcademicWorkspaceState.closeNoteEditor() }
                    )
                } else if (isMath3DLab) {
                    BackHandler {
                        if (backStack.size > 1) {
                            backStack.removeLast()
                            currentRoute = backStack.last()
                        } else {
                            navigateTo(Screen.Home.route)
                        }
                    }
                    Math3DLabScreen(
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeLast()
                                currentRoute = backStack.last()
                            } else {
                                navigateTo(Screen.Home.route)
                            }
                        }
                    )
                } else {
                    // ── TABLET APP SHELL WITH NAVIGATION RAIL ────────────────────
                    BackHandler(enabled = backStack.size > 1) {
                        backStack.removeLast()
                        currentRoute = backStack.last()
                    }

                    var showQuickAddDialog by remember { mutableStateOf(false) }

                    TabletAppShell(
                        currentRoute = currentRoute,
                        onNavigate = { route -> navigateTo(route) },
                        onQuickAdd = { showQuickAddDialog = true },
                        onStartFocusSession = { AcademicWorkspaceState.showFocusSetupDialog = true }
                    ) {
                        LernspaceTabHost(
                            currentRoute = currentRoute,
                            visitedTabs = visitedTabs,
                            onNavigate = { route -> navigateTo(route) },
                            homeViewModel = homeViewModel,
                            onQuickAdd = { showQuickAddDialog = true }
                        )
                    }

                    if (showQuickAddDialog) {
                        QuickAddTaskDialog(
                            onDismiss = { showQuickAddDialog = false },
                            onConfirm = { taskTitle ->
                                homeViewModel.quickAddTask(taskTitle)
                                showQuickAddDialog = false
                            }
                        )
                    }
                }

                // ── GLOBAL OVERLAYS: Focus Session (Zen Fullscreen vs Floating Pill) ──
                if (AcademicWorkspaceState.isFocusSessionActive) {
                    if (!AcademicWorkspaceState.isFocusSessionMinimized) {
                        // Fullscreen Zen Immersion Overlay
                        BackHandler {
                            AcademicWorkspaceState.isFocusSessionMinimized = true
                        }
                        FocusFullscreenImmersion(
                            onMinimize = { AcademicWorkspaceState.isFocusSessionMinimized = true },
                            onFinishEarly = { AcademicWorkspaceState.finishFocusSessionEarly() }
                        )
                    } else {
                        // Companion Floating Capsule Pill (draggable, visible everywhere)
                        FocusFloatingPill(
                            onMaximize = { AcademicWorkspaceState.isFocusSessionMinimized = false }
                        )
                    }
                }

                // Focus Setup Modal Dialog
                if (AcademicWorkspaceState.showFocusSetupDialog) {
                    FocusSetupDialog(
                        onDismiss = { AcademicWorkspaceState.showFocusSetupDialog = false },
                        onStart = { subj, top, preset ->
                            AcademicWorkspaceState.startFocusSession(subj, top, preset)
                        }
                    )
                }

                // Focus Completion & Mastery Upgrade Dialog
                if (AcademicWorkspaceState.showFocusCompletionDialog) {
                    FocusCompletionDialog(
                        onDismiss = { AcademicWorkspaceState.dismissFocusSession() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingDocumentIntent(intent)
    }

    private fun handleIncomingDocumentIntent(incomingIntent: Intent?) {
        if (incomingIntent?.action != Intent.ACTION_VIEW) return
        val uri = incomingIntent.data ?: return
        val mime = incomingIntent.type ?: contentResolver.getType(uri)
        val displayName = DocxDocumentCodec.getDisplayName(this, uri)
        if (!DocxDocumentCodec.isEditableDocument(displayName, mime)) return

        lifecycleScope.launch {
            val importedRes = withContext(Dispatchers.IO) {
                DocxDocumentCodec.importFromUri(this@MainActivity, uri)
            }
            importedRes.onSuccess { imported ->
                val noteId = UUID.randomUUID().toString()
                withContext(Dispatchers.IO) {
                    if (imported.plainText.isNotBlank()) {
                        NoteStorageManager.saveExtractedText(this@MainActivity, noteId, imported.plainText)
                    }
                }
                val timeStr = "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                val snippetText = imported.plainText.replace("\n", " ").take(90)
                    .ifBlank { "${imported.formatLabel}..." }
                val newNote = NoteUiModel(
                    id = noteId,
                    title = imported.title,
                    subject = imported.formatLabel,
                    date = timeStr,
                    snippet = snippetText,
                    canvasType = NoteCanvasType.DOC_DOCUMENT,
                    paperStyle = PaperStyle.BLANK,
                    documentContent = imported.html,
                    pages = mutableListOf(NotePageData(pageIndex = 0))
                )
                AcademicWorkspaceState.notes.add(0, newNote)
                AcademicDatabaseManager.persistNote(newNote)
                AcademicWorkspaceState.openNoteEditor(newNote)
                Toast.makeText(this@MainActivity, "Membuka ${imported.title}", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(this@MainActivity, "Gagal membuka dokumen: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private var isImmersiveFullscreenActive = false

    private fun setImmersiveFullscreen(enable: Boolean) {
        isImmersiveFullscreenActive = enable
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enable) {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && (AcademicWorkspaceState.activeEditingNote != null || isImmersiveFullscreenActive)) {
            setImmersiveFullscreen(true)
        }
    }
}

@Composable
fun QuickAddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title (e.g. Read Chapter 4)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title) },
                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

