package com.lernspace.app.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.theme.*

@Composable
fun LearningScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Learning Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Learning Syllabus & Topics",
                    style = MaterialTheme.typography.headlineLarge,
                    color = LernspaceDark
                )
                Text(
                    text = "Organized by subjects, modules, and self-assessed understanding",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LernspaceTextSecondary
                )
            }

            Button(
                onClick = { /* Add Subject */ },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Subject")
            }
        }

        // Subject & Topics Landscape Columns
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subject List (Left Column)
            Surface(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp)),
                color = LernspaceSurfaceCard
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enrolled Subjects",
                        style = MaterialTheme.typography.titleLarge,
                        color = LernspaceDark
                    )

                    SubjectItem(name = "Neurobiology", code = "NEUR-401", count = "12 Topics", isSelected = true)
                    SubjectItem(name = "Cognitive Psychology", code = "PSYC-320", count = "8 Topics", isSelected = false)
                    SubjectItem(name = "Advanced Statistics", code = "STAT-210", count = "15 Topics", isSelected = false)
                    SubjectItem(name = "Organic Chemistry II", code = "CHEM-222", count = "9 Topics", isSelected = false)
                }
            }

            // Topics & Mastery Status (Right Column)
            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp)),
                color = LernspaceSurfaceCard
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Topics in Neurobiology",
                            style = MaterialTheme.typography.titleLarge,
                            color = LernspaceDark
                        )
                        OutlinedButton(onClick = { /* Add topic */ }, shape = RoundedCornerShape(6.dp)) {
                            Text("Add Topic", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    TopicStatusCard(
                        title = "1. Resting Membrane Potential & Ion Channels",
                        status = "COMFORTABLE",
                        statusColor = AccentTealContainer,
                        textColor = AccentTeal
                    )
                    TopicStatusCard(
                        title = "2. Synaptic Transmission & LTP Plasticity",
                        status = "NEEDS_REVIEW",
                        statusColor = AccentAmber.copy(alpha = 0.15f),
                        textColor = AccentAmber
                    )
                    TopicStatusCard(
                        title = "3. Hippocampal Encoding Circuitry",
                        status = "LEARNING",
                        statusColor = AccentBlueContainer,
                        textColor = AccentBlue
                    )
                    TopicStatusCard(
                        title = "4. Glial Cell Regulation and Myelination",
                        status = "NOT_STARTED",
                        statusColor = LernspaceSurfaceLow,
                        textColor = LernspaceTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectItem(
    name: String,
    code: String,
    count: String,
    isSelected: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = if (isSelected) LernspaceSurfaceLow else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = Icons.Filled.Folder, contentDescription = null, tint = AccentBlue)
                Column {
                    Text(text = name, style = MaterialTheme.typography.bodyLarge, color = LernspaceDark)
                    Text(text = code, style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                }
            }
            Text(text = count, style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
        }
    }
}

@Composable
fun TopicStatusCard(
    title: String,
    status: String,
    statusColor: Color,
    textColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(8.dp)),
        color = LernspaceSurfaceCard
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = LernspaceDark)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor
            ) {
                Text(
                    text = status.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
