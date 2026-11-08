package com.lernspace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        val app = application as LernspaceApplication
        HomeViewModel.provideFactory(app.taskRepository, app.subjectRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LernspaceTheme {
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

                // ── IMMERSIVE FULL-SCREEN NOTE CANVAS (Zero Rail, 100% Display) ──
                if (activeEditingNote != null) {
                    BackHandler {
                        AcademicWorkspaceState.closeNoteEditor()
                    }
                    NoteEditorScreen(
                        note = activeEditingNote,
                        onBack = { AcademicWorkspaceState.closeNoteEditor() }
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

