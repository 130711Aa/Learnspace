package com.lernspace.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lernspace.app.ui.home.HomeScreen
import com.lernspace.app.ui.insights.InsightsScreen
import com.lernspace.app.ui.learning.LearningScreen
import com.lernspace.app.ui.planner.PlannerScreen
import com.lernspace.app.ui.workspace.WorkspaceScreen

@Composable
fun LernspaceNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onQuickAdd: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(onQuickAddTask = onQuickAdd)
        }
        composable(Screen.Planner.route) {
            PlannerScreen()
        }
        composable(Screen.Learning.route) {
            LearningScreen()
        }
        composable(Screen.Workspace.route) {
            WorkspaceScreen()
        }
        composable(Screen.Insights.route) {
            InsightsScreen()
        }
    }
}
