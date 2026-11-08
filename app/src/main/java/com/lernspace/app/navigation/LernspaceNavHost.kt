package com.lernspace.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
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

    fun navigateToTab(route: String) {
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
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
        popExitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToScreen = { route -> navigateToTab(route) },
                onQuickAddTask = onQuickAdd
            )
        }
        composable(Screen.Planner.route) {
            PlannerScreen(
                onNavigateToSyllabus = { subjectId ->
                    navigateToTab(Screen.Learning.route)
                }
            )
        }
        composable(Screen.Learning.route) {
            LearningScreen(
                onNavigateToWorkspace = { subjectId ->
                    navigateToTab(Screen.Workspace.route)
                }
            )
        }
        composable(Screen.Workspace.route) {
            WorkspaceScreen(
                onNavigateToSyllabus = { subjectId ->
                    navigateToTab(Screen.Learning.route)
                }
            )
        }
        composable(Screen.Insights.route) {
            InsightsScreen()
        }
    }
}


