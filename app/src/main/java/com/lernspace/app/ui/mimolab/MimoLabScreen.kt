package com.lernspace.app.ui.mimolab

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lernspace.app.core.mimolab.MimoLabActiveSession
import com.lernspace.app.core.mimolab.MimoSimulation
import com.lernspace.app.core.mimolab.MimoSubject
import com.lernspace.app.core.supabase.SupabaseMimoLabClient
import com.lernspace.app.ui.mimolab.components.MimoSimulationViewer
import com.lernspace.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MimoLabScreen(
    onNavigateToWorkspace: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current

    var selectedSubject by remember { mutableStateOf<MimoSubject?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var simulations by remember { mutableStateOf<List<MimoSimulation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Active Simulation Viewer Dialog
    var activeViewerSim by remember { mutableStateOf<MimoSimulation?>(null) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val res = SupabaseMimoLabClient.fetchSimulations(
                subject = selectedSubject,
                searchQuery = searchQuery
            )
            withContext(Dispatchers.Main) {
                isLoading = false
                res.onSuccess { list ->
                    simulations = list
                }.onFailure {
                    simulations = SupabaseMimoLabClient.getBuiltInStarterSimulations()
                }
            }
        }
    }

    LaunchedEffect(selectedSubject, searchQuery) {
        loadData()
    }

    val displayedSimulations = remember(simulations, selectedSubject, searchQuery) {
        simulations.filter { sim ->
            val matchSubject = selectedSubject == null || sim.subject == selectedSubject
            val matchQuery = searchQuery.isBlank() ||
                    sim.title.contains(searchQuery, ignoreCase = true) ||
                    sim.description.contains(searchQuery, ignoreCase = true) ||
                    sim.conceptTopic.contains(searchQuery, ignoreCase = true)
            matchSubject && matchQuery
        }
    }

    if (activeViewerSim != null) {
        // ── Dedicated Fullscreen Studio Workspace (Option 1) ──
        MimoSimulationViewer(
            simulation = activeViewerSim!!,
            isDarkTheme = isDark,
            isFullscreen = true,
            showHeader = true,
            onInsertSnapshotToNote = { bitmap ->
                MimoLabActiveSession.pendingSnapshotBitmap = bitmap
                Toast.makeText(context, "Snapshot simulasi berhasil disalin ke catatan!", Toast.LENGTH_SHORT).show()
                activeViewerSim = null
                onNavigateToWorkspace?.invoke()
            },
            onClose = { activeViewerSim = null },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            containerColor = LernspaceSurface,
            topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LernspaceSurfaceCard)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Mimo Lab",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LernspaceDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Laboratorium simulasi visual interaktif untuk memahami konsep rumit sains & matematika",
                            fontSize = 13.sp,
                            color = LernspaceTextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { loadData() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = LernspaceDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar + Subject Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        placeholder = { Text("Cari konsep, hukum fisika, rumus...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = LernspaceTextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = LernspaceTextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) Color.White else Color.Black,
                            unfocusedBorderColor = LernspaceBorder,
                            focusedContainerColor = LernspaceSurface,
                            unfocusedContainerColor = LernspaceSurface
                        )
                    )

                    // Subject filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSubject == null,
                                onClick = { selectedSubject = null },
                                label = { Text("Semua", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        items(MimoSubject.entries) { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = { selectedSubject = if (selectedSubject == subj) null else subj },
                                label = { Text(subj.chipLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (isDark) Color.White else Color.Black)
                }
            } else if (displayedSimulations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Functions,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = LernspaceTextSecondary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ada simulasi yang cocok dengan pencarian"
                            else "Belum ada simulasi di katalog",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = LernspaceTextSecondary
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedSimulations, key = { it.id }) { sim ->
                        MimoSimulationCard(
                            simulation = sim,
                            isDark = isDark,
                            onPlay = {
                                activeViewerSim = sim
                            },
                            onPinToNote = {
                                MimoLabActiveSession.activeFloatingSimulation = sim
                                Toast.makeText(context, "'${sim.title}' disematkan melayang di catatan!", Toast.LENGTH_SHORT).show()
                                onNavigateToWorkspace?.invoke()
                            }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun MimoSimulationCard(
    simulation: MimoSimulation,
    isDark: Boolean,
    onPlay: () -> Unit,
    onPinToNote: () -> Unit
) {
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(simulation.subject.badgeColorHex))
    } catch (_: Exception) {
        Color(0xFF0284C7)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LernspaceSurfaceCard),
        border = BorderStroke(1.dp, LernspaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Subject Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = subjectColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = simulation.subject.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = subjectColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Title & Concept
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = simulation.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LernspaceDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = simulation.description,
                    fontSize = 12.sp,
                    color = LernspaceTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(color = LernspaceBorder)

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color.White else Color.Black,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Jalankan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onPinToNote,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.OpenWith, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Melayang", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
