package com.lernspace.app.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.model.NoteUiModel

@Composable
fun WorkspaceScreen() {
    var activeNote by remember { mutableStateOf<NoteUiModel?>(null) }

    val initialNotes = remember {
        mutableStateListOf(
            NoteUiModel(
                title = "LTP Induction & NMDA Receptors",
                subject = "Neurobiology",
                date = "Today, 10:15 AM",
                snippet = "Long-term potentiation requires high-frequency stimulation of Schaffer collaterals to relieve magnesium block..."
            ),
            NoteUiModel(
                title = "Bayesian Prior Distributions Summary",
                subject = "Advanced Statistics",
                date = "Yesterday",
                snippet = "Conjugate priors simplify posterior density calculation when sampling from exponential family distributions..."
            ),
            NoteUiModel(
                title = "Working Memory & Executive Control",
                subject = "Cognitive Psychology",
                date = "Oct 19",
                snippet = "Baddeley's multicomponent model includes the phonological loop, visuospatial sketchpad, and central executive..."
            )
        )
    }

    if (activeNote != null) {
        // Full screen Interactive Note Editor
        NoteEditorScreen(
            note = activeNote!!,
            onBack = { activeNote = null }
        )
    } else {
        // Workspace Notes Overview
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                        text = "Your connected learning environment with stylus inking, text boxes, and images",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LernspaceTextSecondary
                    )
                }

                Button(
                    onClick = {
                        val newNote = NoteUiModel(
                            title = "Untitled Note",
                            subject = "General Study",
                            date = "Just now",
                            snippet = "Start handwriting or typing notes..."
                        )
                        initialNotes.add(0, newNote)
                        activeNote = newNote
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Note")
                }
            }

            // Note Cards Landscape Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                initialNotes.forEach { note ->
                    NoteCard(
                        note = note,
                        onOpen = { activeNote = note },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: NoteUiModel,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen),
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
                            text = note.subject,
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(text = note.date, style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = LernspaceDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = note.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LernspaceTextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Interactive Note",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentTeal
                    )
                }

                FilledTonalButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Open", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
