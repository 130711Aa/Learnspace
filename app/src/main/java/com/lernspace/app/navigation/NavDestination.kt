package com.lernspace.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Dashboard)
    data object Planner : Screen("planner", "Planner", Icons.Filled.CalendarMonth)
    data object Learning : Screen("learning", "Learning", Icons.Filled.MenuBook)
    data object Workspace : Screen("workspace", "Workspace", Icons.Filled.EditNote)
    data object Insights : Screen("insights", "Insights", Icons.Filled.Analytics)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val items get() = listOf(Home, Planner, Learning, Workspace, Insights, Settings)
    }
}

