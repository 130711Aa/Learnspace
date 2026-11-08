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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lernspace.app.navigation.LernspaceTabHost
import com.lernspace.app.navigation.Screen
import com.lernspace.app.ui.components.TabletAppShell
import com.lernspace.app.ui.home.HomeViewModel
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.theme.LernspaceTheme

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

                // Native Android tablet back handling
                BackHandler(enabled = backStack.size > 1) {
                    backStack.removeLast()
                    currentRoute = backStack.last()
                }

                var showQuickAddDialog by remember { mutableStateOf(false) }

                TabletAppShell(
                    currentRoute = currentRoute,
                    onNavigate = { route -> navigateTo(route) },
                    onQuickAdd = { showQuickAddDialog = true },
                    onStartFocusSession = { /* Start timer/focus mode */ }
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

