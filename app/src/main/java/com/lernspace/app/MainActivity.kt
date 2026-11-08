package com.lernspace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lernspace.app.navigation.LernspaceNavHost
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
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

                var showQuickAddDialog by remember { mutableStateOf(false) }

                TabletAppShell(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == currentRoute) return@TabletAppShell
                        if (route == Screen.Home.route) {
                            val popped = navController.popBackStack(Screen.Home.route, inclusive = false)
                            if (!popped) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onQuickAdd = { showQuickAddDialog = true },
                    onStartFocusSession = { /* Start timer/focus mode */ }
                ) {
                    LernspaceNavHost(
                        navController = navController,
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

