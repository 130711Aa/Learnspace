package com.lernspace.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ViewInAr
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
    data object MimoLab : Screen("mimo_lab", "Mimo Lab", Icons.Filled.Functions)
    data object Math3DLab : Screen("math3d_lab", "Math 3D", Icons.Filled.ViewInAr)
    data object Flashcards : Screen("flashcards", "Cards", Icons.Filled.Style)
    data object Quiz : Screen("quiz", "Quiz", Icons.Filled.Quiz)
    data object BankSoal : Screen("bank_soal", "Bank Soal", Icons.Filled.FolderCopy)
    data object Insights : Screen("insights", "Insights", Icons.Filled.Analytics)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val items get() = listOf(Home, Planner, Learning, Workspace, MimoLab, Math3DLab, Flashcards, Quiz, BankSoal, Insights, Settings)
    }
}

