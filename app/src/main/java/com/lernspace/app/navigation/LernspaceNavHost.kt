package com.lernspace.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import com.lernspace.app.ui.practice.PracticeHubScreen
import com.lernspace.app.ui.practice.PracticeHubState
import com.lernspace.app.ui.practice.PracticeTab
import com.lernspace.app.ui.home.HomeScreen
import com.lernspace.app.ui.home.HomeViewModel
import com.lernspace.app.ui.insights.InsightsScreen
import com.lernspace.app.ui.learning.LearningScreen
import com.lernspace.app.ui.mimolab.MimoLabScreen
import com.lernspace.app.ui.planner.PlannerScreen
import com.lernspace.app.ui.quiz.QuizScreen
import com.lernspace.app.ui.settings.SettingsScreen
import com.lernspace.app.ui.workspace.WorkspaceScreen

/**
 * High-performance tab visibility modifier that keeps composables alive in memory (tab cache).
 * When invisible:
 * - Does not place the layout node, eliminating GPU draw calls and pointer input dispatch.
 * - Retains full composition state, scroll positions, and user input so switching back is 0ms instant.
 */
fun Modifier.keepAliveTab(visible: Boolean): Modifier = this.then(
    Modifier
        .graphicsLayer {
            alpha = if (visible) 1f else 0f
        }
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                if (visible) {
                    placeable.place(0, 0)
                }
            }
        }
)

/**
 * Tab-cached host for tablet note-taking experience.
 * Replaces heavy NavHost teardown/recreation with instant, keep-alive multi-pane swapping.
 */
@Composable
fun LernspaceTabHost(
    currentRoute: String,
    visitedTabs: List<String>,
    onNavigate: (String) -> Unit,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onQuickAdd: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Tab 1: Home
        if (visitedTabs.contains(Screen.Home.route)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(currentRoute == Screen.Home.route)
            ) {
                HomeScreen(
                    onNavigateToScreen = onNavigate,
                    onQuickAddTask = onQuickAdd
                )
            }
        }

        // Tab 2: Planner
        if (visitedTabs.contains(Screen.Planner.route)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(currentRoute == Screen.Planner.route)
            ) {
                PlannerScreen(
                    onNavigateToSyllabus = { _ ->
                        onNavigate(Screen.Learning.route)
                    }
                )
            }
        }

        // Tab 3: Learning / Syllabus
        if (visitedTabs.contains(Screen.Learning.route)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(currentRoute == Screen.Learning.route)
            ) {
                LearningScreen(
                    onNavigateToWorkspace = { _ ->
                        onNavigate(Screen.Workspace.route)
                    }
                )
            }
        }

        // Tab 4: Workspace
        if (visitedTabs.contains(Screen.Workspace.route)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(currentRoute == Screen.Workspace.route)
            ) {
                WorkspaceScreen(
                    onNavigateToSyllabus = { _ ->
                        onNavigate(Screen.Learning.route)
                    }
                )
            }
        }

        // Tab: Mimo Lab
        if (visitedTabs.contains(Screen.MimoLab.route)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(currentRoute == Screen.MimoLab.route)
            ) {
                MimoLabScreen(
                    onNavigateToWorkspace = {
                        onNavigate(Screen.Workspace.route)
                    }
                )
            }
        }


        // Tab: Practice Hub (Unified Flashcards, Quiz, Bank Soal)
        val isPracticeRoute = currentRoute in listOf(
            Screen.Practice.route,
            Screen.Flashcards.route,
            Screen.Quiz.route,
            Screen.BankSoal.route
        )
        val isPracticeVisited = visitedTabs.any { it in listOf(
            Screen.Practice.route,
            Screen.Flashcards.route,
            Screen.Quiz.route,
            Screen.BankSoal.route
        ) }

        if (isPracticeVisited) {
            when (currentRoute) {
                Screen.Flashcards.route -> PracticeHubState.selectedTab = PracticeTab.FLASHCARDS
                Screen.Quiz.route -> PracticeHubState.selectedTab = PracticeTab.QUIZ
                Screen.BankSoal.route -> PracticeHubState.selectedTab = PracticeTab.BANK_SOAL
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(isPracticeRoute)
            ) {
                PracticeHubScreen()
            }
        }

        // Tab 5: Insights
        if (visitedTabs.contains(Screen.Insights.route)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(currentRoute == Screen.Insights.route)
            ) {
                InsightsScreen(
                    onNavigateToSyllabus = { _ ->
                        onNavigate(Screen.Learning.route)
                    },
                    onNavigateToPlanner = {
                        onNavigate(Screen.Planner.route)
                    },
                    onNavigateToWorkspace = {
                        onNavigate(Screen.Workspace.route)
                    },
                    onNavigateToBankSoal = {
                        onNavigate(Screen.BankSoal.route)
                    },
                    onNavigateToFlashcards = {
                        onNavigate(Screen.Flashcards.route)
                    }
                )
            }
        }

        // Tab 6: Settings
        if (visitedTabs.contains(Screen.Settings.route)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(currentRoute == Screen.Settings.route)
            ) {
                SettingsScreen()
            }
        }
    }
}



