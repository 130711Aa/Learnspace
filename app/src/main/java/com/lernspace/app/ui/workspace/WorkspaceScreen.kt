package com.lernspace.app.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.theme.*

@Composable
fun WorkspaceScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Workspace & Notes",
                    style = MaterialTheme.typography.headlineLarge,
                    color = LernspaceDark
                )
                Text(
                    text = "Text notes and material references connected to subjects and topics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LernspaceTextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { /* Quick stylus note */ },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stylus Canvas")
                }
                Button(
                    onClick = { /* New text note */ },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Note")
                }
            }
        }

        // Workspace note cards
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NoteCard(
                title = "LTP Induction & NMDA Receptors",
                subject = "Neurobiology",
                snippet = "Long-term potentiation requires high-frequency stimulation of the Schaffer collaterals to relieve magnesium block...",
                date = "Oct 24"
            )
            NoteCard(
                title = "Bayesian Prior Distributions Summary",
                subject = "Advanced Statistics",
                snippet = "Conjugate priors simplify posterior density calculation when sampling from exponential families...",
                date = "Oct 22"
            )
            NoteCard(
                title = "Working Memory & Executive Control",
                subject = "Cognitive Psychology",
                snippet = "Baddeley's multicomponent model includes the phonological loop, visuospatial sketchpad, and central executive...",
                date = "Oct 19"
            )
        }
    }
}

@Composable
fun RowScope.NoteCard(
    title: String,
    subject: String,
    snippet: String,
    date: String
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp)),
        color = LernspaceSurfaceCard
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = LernspaceSurfaceLow) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(text = date, style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = LernspaceDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LernspaceTextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Text Note",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentTeal
                )
            }
        }
    }
}
