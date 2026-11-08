package com.lernspace.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lernspace.app.ui.home.HomeScreen
import com.lernspace.app.ui.home.HomeViewModel
import com.lernspace.app.ui.insights.InsightsScreen
import com.lernspace.app.ui.learning.LearningScreen
import com.lernspace.app.ui.planner.PlannerScreen
import com.lernspace.app.ui.workspace.WorkspaceScreen

@Composable
fun LernspaceNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onQuickAdd: () -> Unit = {}
) {
    val homeUiState by homeViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToScreen = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onQuickAddTask = onQuickAdd
            )
        }
        composable(Screen.Planner.route) {
            PlannerScreen(
                onNavigateToSyllabus = { subjectId ->
                    navController.navigate(Screen.Learning.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Learning.route) {
            LearningScreen(
                onNavigateToWorkspace = { subjectId ->
                    navController.navigate(Screen.Workspace.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Workspace.route) {
            WorkspaceScreen(
                onNavigateToSyllabus = { subjectId ->
                    navController.navigate(Screen.Learning.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Insights.route) {
            InsightsScreen()
        }
    }
}


