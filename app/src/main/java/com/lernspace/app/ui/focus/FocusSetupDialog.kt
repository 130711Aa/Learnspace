package com.lernspace.app.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.data.repository.FocusTimerPreset
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSetupDialog(
    onDismiss: () -> Unit,
    onStart: (SubjectUi?, TopicUi?, FocusTimerPreset) -> Unit
) {
    val subjects = AcademicWorkspaceState.subjects
    val topics = AcademicWorkspaceState.topics

    var selectedSubject by remember { mutableStateOf<SubjectUi?>(subjects.firstOrNull()) }
    var selectedTopic by remember {
        mutableStateOf<TopicUi?>(
            topics.firstOrNull { it.subjectId == subjects.firstOrNull()?.id }
        )
    }
    var selectedPreset by remember { mutableStateOf(FocusTimerPreset.POMODORO_25) }
    var selectedAmbience by remember { mutableStateOf("Hujan Lembut") }

    val ambienceOptions = listOf("Hening", "Hujan Lembut", "Perpustakaan", "White Noise", "Lofi Beats")

    val availableTopics = remember(selectedSubject?.id) {
        if (selectedSubject != null) {
            topics.filter { it.subjectId == selectedSubject!!.id }
        } else {
            emptyList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(720.dp)
                .wrapContentHeight()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, LernspaceBorder, RoundedCornerShape(20.dp)),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LernspaceDark,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.HourglassBottom,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Mulai Focus Session",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Text(
                                text = "Blokir distraksi, pilih materi silabus, dan fokus belajar mendalam.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LernspaceTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = LernspaceTextSecondary)
                    }
                }

                HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.6f))

                // Section 1: Target Mata Kuliah (Subject)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Pilih Mata Kuliah",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(subjects) { subj ->
                            val isSelected = selectedSubject?.id == subj.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) subj.color.copy(alpha = 0.15f) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) subj.color else LernspaceBorder
                                ),
                                modifier = Modifier.clickable {
                                    selectedSubject = subj
                                    selectedTopic = topics.firstOrNull { it.subjectId == subj.id }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(subj.color)
                                    )
                                    Text(
                                        text = subj.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) subj.color else LernspaceDark
                                    )
                                }
                            }
                        }

                        // Freeform study option
                        item {
                            val isSelected = selectedSubject == null
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) AccentBlue.copy(alpha = 0.15f) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AccentBlue else LernspaceBorder
                                ),
                                modifier = Modifier.clickable {
                                    selectedSubject = null
                                    selectedTopic = null
                                }
                            ) {
                                Text(
                                    text = "Belajar Umum / Bebas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) AccentBlue else LernspaceDark,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Section 2: Target Sub-bab Silabus (Topic)
                if (availableTopics.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "2. Pilih Sub-bab Silabus yang Dipelajari",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(availableTopics) { top ->
                                val isSelected = selectedTopic?.id == top.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) LernspaceDark else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { selectedTopic = top }
                                ) {
                                    Text(
                                        text = top.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else LernspaceDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Timer Preset Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "3. Durasi Timer Sesi",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FocusTimerPreset.values().forEach { preset ->
                            val isSelected = selectedPreset == preset
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedPreset = preset },
                                color = if (isSelected) LernspaceDark else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LernspaceDark else LernspaceBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = preset.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else LernspaceDark
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color(0xFF94A3B8) else LernspaceTextSecondary,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Ambient Audio Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Headphones, contentDescription = null, tint = LernspaceTextSecondary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Suara Latar (Ambience):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = LernspaceTextSecondary
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ambienceOptions) { amb ->
                            val isSelected = selectedAmbience == amb
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) AccentBlue.copy(alpha = 0.15f) else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) AccentBlue else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.clickable { selectedAmbience = amb }
                            ) {
                                Text(
                                    text = amb,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AccentBlue else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = LernspaceTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onStart(selectedSubject, selectedTopic, selectedPreset)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mulai Sesi Fokus",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

