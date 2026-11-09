package com.lernspace.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.data.local.AppSettingsManager
import com.lernspace.app.data.local.Handedness
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.data.repository.FocusSessionPhase
import com.lernspace.app.data.repository.FocusTimerPreset
import com.lernspace.app.navigation.Screen
import com.lernspace.app.ui.search.GlobalSearchDialog
import com.lernspace.app.ui.search.GlobalSearchPill
import com.lernspace.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletAppShell(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onQuickAdd: () -> Unit,
    onStartFocusSession: () -> Unit,
    content: @Composable () -> Unit
) {
    val isLeftHanded = AppSettingsManager.handedness == Handedness.LEFT_HANDED

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LernspaceSurface)
    ) {
        if (!isLeftHanded) {
            TabletNavRail(currentRoute = currentRoute, onNavigate = onNavigate)
        }

        TabletMainContent(
            modifier = Modifier.weight(1f),
            onNavigate = onNavigate,
            onStartFocusSession = onStartFocusSession,
            content = content
        )

        if (isLeftHanded) {
            TabletNavRail(currentRoute = currentRoute, onNavigate = onNavigate)
        }
    }

    if (AcademicWorkspaceState.isGlobalSearchOpen) {
        GlobalSearchDialog(
            onDismiss = { AcademicWorkspaceState.isGlobalSearchOpen = false },
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun TabletNavRail(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Surface(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight(),
        color = LernspaceSurfaceCard,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color.White else Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoStories,
                        contentDescription = "Logo",
                        tint = if (isDark) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lernspace",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LernspaceDark
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Nav Items
                Screen.items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { onNavigate(screen.route) },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = if (isDark) Color.Black else Color.White,
                            selectedTextColor = if (isDark) Color.White else Color.Black,
                            indicatorColor = if (isDark) Color.White else Color.Black,
                            unselectedIconColor = LernspaceTextSecondary,
                            unselectedTextColor = LernspaceTextSecondary
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletMainContent(
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    onStartFocusSession: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxHeight()
    ) {
        // Tablet TopBar with Stitch Global Search Pill
        TopAppBar(
            title = {
                GlobalSearchPill(
                    onClick = { AcademicWorkspaceState.isGlobalSearchOpen = true },
                    modifier = Modifier.width(360.dp)
                )
            },
            actions = {
                val isFocusActive = AcademicWorkspaceState.isFocusSessionActive
                val focusPhase = AcademicWorkspaceState.focusPhase
                val remainingSec = AcademicWorkspaceState.focusTimeRemainingSeconds
                val elapsedSec = AcademicWorkspaceState.focusTimeElapsedSeconds
                val preset = AcademicWorkspaceState.focusPreset
                val displayMin = if (preset == FocusTimerPreset.STOPWATCH) elapsedSec / 60 else remainingSec / 60
                val displaySec = if (preset == FocusTimerPreset.STOPWATCH) elapsedSec % 60 else remainingSec % 60
                val timeFormatted = String.format("%02d:%02d", displayMin, displaySec)

                val isDark = LocalIsDarkTheme.current
                Button(
                    onClick = {
                        if (isFocusActive) {
                            AcademicWorkspaceState.isFocusSessionMinimized = false
                        } else {
                            onStartFocusSession()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isFocusActive) {
                            if (isDark) Color.White else Color.Black
                        } else if (focusPhase == FocusSessionPhase.BREAK) {
                            Color(0xFF10B981)
                        } else {
                            Color(0xFF2563EB)
                        },
                        contentColor = if (!isFocusActive) {
                            if (isDark) Color.Black else Color.White
                        } else Color.White
                    ),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = if (!isFocusActive) Icons.Filled.PlayArrow else Icons.Filled.HourglassBottom,
                        contentDescription = "Focus",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!isFocusActive) "Focus Session" else "$timeFormatted • ${if (focusPhase == FocusSessionPhase.BREAK) "Break" else "Focus"}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = LernspaceSurface
            )
        )

        // Screen content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            content()
        }
    }
}

