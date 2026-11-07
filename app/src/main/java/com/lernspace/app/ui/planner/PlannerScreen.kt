package com.lernspace.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.theme.*

@Composable
fun PlannerScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Day Agenda", "Week Schedule", "Month Horizon")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Planner Header & View Modes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Planner & Scheduling",
                    style = MaterialTheme.typography.headlineLarge,
                    color = LernspaceDark
                )
                Text(
                    text = "Hybrid calendar with deterministic study block suggestions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LernspaceTextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.width(360.dp),
                    containerColor = LernspaceSurfaceLow,
                    indicator = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedTab == index) LernspaceDark else LernspaceTextSecondary
                                )
                            }
                        )
                    }
                }

                Button(
                    onClick = { /* Add event/session */ },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Plan Session")
                }
            }
        }

        // Planner Layout (Time blocks grid placeholder)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp)),
            color = LernspaceSurfaceCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time gutter
                Column(
                    modifier = Modifier.width(60.dp),
                    verticalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00").forEach { time ->
                        Text(text = time, style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                    }
                }

                // Schedule blocks
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScheduleCard(
                        title = "Neurobiology Lecture: Action Potentials",
                        time = "09:00 - 10:15",
                        category = "LECTURE",
                        color = AccentBlueContainer,
                        accentColor = AccentBlue
                    )
                    ScheduleCard(
                        title = "Suggested Study: Synaptic Review (Hybrid Engine)",
                        time = "10:30 - 12:00",
                        category = "HYBRID SUGGESTION",
                        color = AccentTealContainer,
                        accentColor = AccentTeal
                    )
                    ScheduleCard(
                        title = "Advanced Statistics Lab Problem Set",
                        time = "13:30 - 15:00",
                        category = "ASSIGNMENT",
                        color = LernspaceSurfaceLow,
                        accentColor = LernspaceDark
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(
    title: String,
    time: String,
    category: String,
    color: Color,
    accentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = category, style = MaterialTheme.typography.labelSmall, color = accentColor)
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = LernspaceDark)
            }
            Text(text = time, style = MaterialTheme.typography.labelMedium, color = LernspaceTextSecondary)
        }
    }
}

