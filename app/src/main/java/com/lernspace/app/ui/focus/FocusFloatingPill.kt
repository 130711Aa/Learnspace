package com.lernspace.app.ui.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.data.repository.FocusSessionPhase
import com.lernspace.app.data.repository.FocusTimerPreset
import com.lernspace.app.ui.theme.AccentBlue
import com.lernspace.app.ui.theme.LernspaceDark
import kotlin.math.roundToInt

@Composable
fun FocusFloatingPill(
    onMaximize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = AcademicWorkspaceState.isFocusTimerRunning
    val phase = AcademicWorkspaceState.focusPhase
    val remainingSeconds = AcademicWorkspaceState.focusTimeRemainingSeconds
    val elapsedSeconds = AcademicWorkspaceState.focusTimeElapsedSeconds
    val preset = AcademicWorkspaceState.focusPreset
    val subject = AcademicWorkspaceState.focusSelectedSubject

    val isBreak = phase == FocusSessionPhase.BREAK

    val displayMinutes = if (preset == FocusTimerPreset.STOPWATCH) elapsedSeconds / 60 else remainingSeconds / 60
    val displaySeconds = if (preset == FocusTimerPreset.STOPWATCH) elapsedSeconds % 60 else remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", displayMinutes, displaySeconds)

    val badgeColor = if (isBreak) Color(0xFF10B981) else (subject?.color ?: AccentBlue)

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var pillWidthPx by remember { mutableStateOf(0f) }
    var pillHeightPx by remember { mutableStateOf(0f) }

    // Initialize position safely away from Android status bar (top = 56dp, right side of screen)
    LaunchedEffect(screenWidthPx) {
        if (AcademicWorkspaceState.focusPillOffsetX < 0f) {
            AcademicWorkspaceState.focusPillOffsetX = (screenWidthPx - with(density) { 360.dp.toPx() }).coerceAtLeast(16f)
        }
        if (AcademicWorkspaceState.focusPillOffsetY < 0f) {
            AcademicWorkspaceState.focusPillOffsetY = with(density) { 56.dp.toPx() }
        }
    }

    val currentX = if (AcademicWorkspaceState.focusPillOffsetX < 0f) {
        (screenWidthPx - with(density) { 360.dp.toPx() }).coerceAtLeast(16f)
    } else {
        AcademicWorkspaceState.focusPillOffsetX
    }

    val currentY = if (AcademicWorkspaceState.focusPillOffsetY < 0f) {
        with(density) { 56.dp.toPx() }
    } else {
        AcademicWorkspaceState.focusPillOffsetY
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                .onSizeChanged { size ->
                    pillWidthPx = size.width.toFloat()
                    pillHeightPx = size.height.toFloat()
                },
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(vertical = 5.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── DRAGGABLE SECTION (Handle, Status Dot, Timer, Subject Badge) ──
                Row(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val minX = with(density) { 16.dp.toPx() }
                                val minY = with(density) { 36.dp.toPx() } // safe from system status bar
                                val maxX = (screenWidthPx - pillWidthPx - with(density) { 16.dp.toPx() }).coerceAtLeast(minX)
                                val maxY = (screenHeightPx - pillHeightPx - with(density) { 24.dp.toPx() }).coerceAtLeast(minY)

                                val newX = (AcademicWorkspaceState.focusPillOffsetX + dragAmount.x).coerceIn(minX, maxX)
                                val newY = (AcademicWorkspaceState.focusPillOffsetY + dragAmount.y).coerceIn(minY, maxY)
                                AcademicWorkspaceState.focusPillOffsetX = newX
                                AcademicWorkspaceState.focusPillOffsetY = newY
                            }
                        }
                        .padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Drag grip affordance
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = "Geser pil",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )

                    // Pulsing status dot with halo
                    Box(
                        modifier = Modifier.size(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.2f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                    }

                    // Monospace formatted timer
                    Text(
                        text = timeFormatted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = LernspaceDark
                    )

                    // Phase or Subject Chip
                    if (isBreak) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "ISTIRAHAT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    } else if (subject != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = subject.code.ifBlank { subject.name },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Spacer(modifier = Modifier.width(6.dp))

                // ── COMPACT ACTION BUTTONS (Pause/Play & Fullscreen) ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    // Play / Pause compact button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) Color(0xFFF1F5F9) else Color(0xFFFEF3C7)
                            )
                            .border(
                                1.dp,
                                if (isRunning) Color(0xFFE2E8F0) else Color(0xFFFDE68A),
                                CircleShape
                            )
                            .clickable {
                                if (isRunning) {
                                    AcademicWorkspaceState.pauseFocusTimer()
                                } else {
                                    AcademicWorkspaceState.resumeFocusTimer()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Jeda Fokus" else "Lanjut Fokus",
                            tint = if (isRunning) LernspaceDark else Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Fullscreen Maximize compact button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .clickable(onClick = onMaximize),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Layar Penuh Zen",
                            tint = LernspaceDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
