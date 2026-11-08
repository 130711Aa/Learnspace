package com.lernspace.app.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.data.repository.FocusSessionPhase
import com.lernspace.app.data.repository.FocusTimerPreset
import com.lernspace.app.ui.theme.*

@Composable
fun FocusFullscreenImmersion(
    onMinimize: () -> Unit,
    onFinishEarly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = AcademicWorkspaceState.focusPhase
    val isRunning = AcademicWorkspaceState.isFocusTimerRunning
    val remainingSeconds = AcademicWorkspaceState.focusTimeRemainingSeconds
    val elapsedSeconds = AcademicWorkspaceState.focusTimeElapsedSeconds
    val preset = AcademicWorkspaceState.focusPreset
    val subject = AcademicWorkspaceState.focusSelectedSubject
    val topic = AcademicWorkspaceState.focusSelectedTopic

    val isBreak = phase == FocusSessionPhase.BREAK

    // Calculate progress ratio
    val totalSeconds = if (isBreak) {
        preset.breakMinutes * 60
    } else {
        preset.focusMinutes * 60
    }

    val progress = if (preset == FocusTimerPreset.STOPWATCH || totalSeconds == 0) {
        1f
    } else {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "FocusProgress"
    )

    // Format clock: MM:SS
    val displayMinutes = if (preset == FocusTimerPreset.STOPWATCH) elapsedSeconds / 60 else remainingSeconds / 60
    val displaySeconds = if (preset == FocusTimerPreset.STOPWATCH) elapsedSeconds % 60 else remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", displayMinutes, displaySeconds)

    val ringColor = if (isBreak) Color(0xFF10B981) else (subject?.color ?: AccentBlue)
    val bgColor = if (isBreak) Color(0xFF064E3B) else Color(0xFF090D16)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ── Top Navigation Bar ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phase indicator badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isBreak) Color(0x3310B981) else Color(0x333B82F6),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isBreak) Color(0xFF10B981) else Color(0xFF3B82F6)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isBreak) Color(0xFF10B981) else Color(0xFF3B82F6))
                    )
                    Text(
                        text = if (isBreak) "☕ WAKTU ISTIRAHAT (BREAK)" else "🎯 SESI FOKUS INTENSIF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Top-right actions: Minimize to pill & Finish session
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Minimize to Floating Pill button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x22FFFFFF),
                    modifier = Modifier.clickable(onClick = onMinimize)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = "Minimize",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Buka Bersama Catatan (Minimize)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                // Stop / Finish Session button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33EF4444),
                    modifier = Modifier.clickable(onClick = onFinishEarly)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Akhiri",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Selesai Sesi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                    }
                }
            }
        }

        // ── Center Content: Timer Ring, Clock & Target Topic ──────────────────
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Subject & Topic Badge
            if (subject != null || topic != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x1AFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (subject != null) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(subject.color)
                            )
                            Text(
                                text = subject.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = subject.color
                            )
                        }
                        if (topic != null) {
                            Text(
                                text = "•",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = topic.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Big Circular Countdown Progress Ring
            Box(
                modifier = Modifier.size(320.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 16.dp.toPx()
                    val diameter = size.minDimension - strokeW
                    val topLeft = Offset(strokeW / 2f, strokeW / 2f)
                    val arcSize = Size(diameter, diameter)

                    // Track Background
                    drawArc(
                        color = Color(0x1FFFFFFF),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Active Glowing Progress Ring
                    val sweep = if (preset == FocusTimerPreset.STOPWATCH) 360f else (animatedProgress * 360f)
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = if (isBreak) "Tarik napas & regangkan badan" else preset.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Motivational quote / advice
            Text(
                text = if (isBreak) {
                    "Istirahatkan mata dari layar sejenak. Minum air dan persiapkan pikiran untuk sesi berikutnya."
                } else {
                    "Satu konsep dalam satu waktu. Otakmu sedang mengonsolidasikan memori jangka panjang."
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }

        // ── Bottom Control Deck ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause Toggle Button
            Button(
                onClick = {
                    if (isRunning) {
                        AcademicWorkspaceState.pauseFocusTimer()
                    } else {
                        AcademicWorkspaceState.resumeFocusTimer()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFF334155) else ringColor
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isRunning) "Jeda" else "Lanjut",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Jeda Timer" else "Lanjutkan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Skip to Break / Skip Break Button
            OutlinedButton(
                onClick = {
                    if (!isBreak) {
                        AcademicWorkspaceState.focusPhase = FocusSessionPhase.BREAK
                        AcademicWorkspaceState.focusTimeRemainingSeconds = preset.breakMinutes * 60
                    } else {
                        AcademicWorkspaceState.focusPhase = FocusSessionPhase.FOCUS
                        AcademicWorkspaceState.focusTimeRemainingSeconds = preset.focusMinutes * 60
                    }
                },
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBreak) "Lewati Istirahat" else "Mulai Istirahat",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

