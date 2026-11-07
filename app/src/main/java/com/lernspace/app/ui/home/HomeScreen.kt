package com.lernspace.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lernspace.app.data.local.entity.TaskEntity
import com.lernspace.app.ui.theme.*

@Composable
fun HomeScreen(
    tasksDueToday: List<TaskEntity> = emptyList(),
    overdueTasks: List<TaskEntity> = emptyList(),
    onCompleteTask: (String) -> Unit = {},
    onQuickAddTask: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left 2 Columns (Hero Action + Today's Agenda)
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Greeting & Horizon header
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentTeal)
                    )
                    Text(
                        text = "ACADEMIC HORIZON · DAILY SPRINT",
                        style = MaterialTheme.typography.labelSmall,
                        color = LernspaceTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Today's Command Center",
                    style = MaterialTheme.typography.headlineLarge,
                    color = LernspaceDark
                )
            }

            // Hero Card: Imminent Focus Session
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp)),
                color = LernspaceSurfaceCard,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left color accent bar
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(AccentTeal)
                    )

                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AccentBlueContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = AccentBlue
                                    )
                                    Text(
                                        text = "10:30 AM · UPCOMING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentBlue
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AccentTealContainer
                            ) {
                                Text(
                                    text = "NEUROBIOLOGY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Synaptic Plasticity & LTP Review",
                            style = MaterialTheme.typography.titleLarge,
                            color = LernspaceDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Review preparation for upcoming seminar focusing on receptor cascades and dendritic spine structural remodelling.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LernspaceTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { /* Start */ },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                            ) {
                                Text("Launch Study Session")
                            }
                            OutlinedButton(
                                onClick = { /* Notes */ },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Open Linked Notes")
                            }
                        }
                    }
                }
            }

            // Tasks Due Today Section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tasks Due Today",
                        style = MaterialTheme.typography.headlineMedium,
                        color = LernspaceDark
                    )
                    IconButton(onClick = onQuickAddTask) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Add task",
                            tint = AccentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (tasksDueToday.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, LernspaceBorder, RoundedCornerShape(8.dp)),
                        color = LernspaceSurfaceCard
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Semua tugas hari ini selesai. Siap untuk sesi belajar!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LernspaceTextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasksDueToday) { task ->
                            TaskRowItem(task = task, onComplete = { onCompleteTask(task.id) })
                        }
                    }
                }
            }
        }

        // Right Column (Overdue Alert & Quick Stats / Horizon)
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Overdue Warnings Card (if any)
            if (overdueTasks.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, AccentRedContainer, RoundedCornerShape(12.dp)),
                    color = AccentRedContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Warning",
                                tint = AccentRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Overdue Actions (${overdueTasks.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        overdueTasks.take(3).forEach { task ->
                            Text(
                                text = "• ${task.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AccentRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Quick Stats / Velocity Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp)),
                color = LernspaceSurfaceCard
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Daily Sprint Snapshot",
                        style = MaterialTheme.typography.titleLarge,
                        color = LernspaceDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Planned Study", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                            Text("3.5 hrs", style = MaterialTheme.typography.headlineMedium, color = LernspaceDark)
                        }
                        Column {
                            Text("Mastery Check", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                            Text("3 Topics", style = MaterialTheme.typography.headlineMedium, color = AccentTeal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRowItem(
    task: TaskEntity,
    onComplete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(8.dp)),
        color = LernspaceSurfaceCard
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircleOutline,
                        contentDescription = "Complete",
                        tint = LernspaceTextSecondary
                    )
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LernspaceTextPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (task.priority == "HIGH") AccentRedContainer else LernspaceSurfaceLow
            ) {
                Text(
                    text = task.priority,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.priority == "HIGH") AccentRed else LernspaceTextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

