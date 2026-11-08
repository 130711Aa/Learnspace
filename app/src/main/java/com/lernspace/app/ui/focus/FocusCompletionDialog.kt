package com.lernspace.app.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.theme.LernspaceBorder
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.theme.LernspaceTextSecondary

@Composable
fun FocusCompletionDialog(
    onDismiss: () -> Unit
) {
    val topic = AcademicWorkspaceState.focusSelectedTopic
    val subject = AcademicWorkspaceState.focusSelectedSubject
    val elapsedMins = (AcademicWorkspaceState.focusTimeElapsedSeconds / 60).coerceAtLeast(1)

    var updatedStatus by remember { mutableStateOf<TopicStatus?>(topic?.status) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(520.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, LernspaceBorder, RoundedCornerShape(20.dp)),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Success Trophy Icon
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFD1FAE5),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Title & stats
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Sesi Fokus Tuntas!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Text(
                        text = "Hebat! Kamu telah menyelesaikan $elapsedMins menit sesi belajar intensif.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LernspaceTextSecondary
                    )
                }

                // Insights logged banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "+$elapsedMins Menit telah ditambahkan ke grafik jam studi mingguan di Insights.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Syllabus mastery promotion
                if (topic != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Apakah pemahamanmu di materi ini meningkat?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        Text(
                            text = topic.title,
                            fontSize = 12.sp,
                            color = subject?.color ?: Color(0xFF2563EB),
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Upgrade to Comfortable
                            OutlinedButton(
                                onClick = {
                                    updatedStatus = TopicStatus.COMFORTABLE
                                    AcademicWorkspaceState.updateTopicStatus(topic.id, TopicStatus.COMFORTABLE)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (updatedStatus == TopicStatus.COMFORTABLE) Color(0xFFD1FAE5) else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Filled.Check, null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Comfortable", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                            }

                            // Upgrade to Mastered
                            OutlinedButton(
                                onClick = {
                                    updatedStatus = TopicStatus.MASTERED
                                    AcademicWorkspaceState.updateTopicStatus(topic.id, TopicStatus.MASTERED)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (updatedStatus == TopicStatus.MASTERED) Color(0xFFEDE9FE) else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Filled.Star, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mastered", fontSize = 11.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Tutup & Lanjutkan Aktivitas", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

