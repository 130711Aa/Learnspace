package com.lernspace.app.ui.workspace.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.audio.NoteAudioController
import com.lernspace.app.ui.theme.*

/**
 * Compact, elegant Audio Lecture Recording & Player Bar.
 * Appears below the topbar when recording or playing audio lectures.
 */
@Composable
fun AudioLectureBar(
    audioController: NoteAudioController,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recState = audioController.recordingState
    val playState = audioController.playbackState
    val hasAudio = audioController.hasExistingRecording

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LernspaceSurfaceCard.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, LernspaceBorder),
        shadowElevation = 4.dp,
        modifier = modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                // ── RECORDING IN PROGRESS ──
                recState.isRecording -> {
                    // Pulsing red indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (recState.isPaused) AccentAmber else AccentRed)
                    )

                    // Audio Amplitude wave simulation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.width(36.dp).height(16.dp)
                    ) {
                        val normAmp = (recState.amplitude / 32767f).coerceIn(0.15f, 1f)
                        val barHeights = listOf(
                            (normAmp * 0.5f).coerceIn(0.2f, 1f),
                            (normAmp * 1.0f).coerceIn(0.3f, 1f),
                            (normAmp * 0.75f).coerceIn(0.25f, 1f),
                            (normAmp * 0.4f).coerceIn(0.2f, 1f)
                        )
                        barHeights.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(if (recState.isPaused) 0.2f else h)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (recState.isPaused) AccentAmber else AccentRed)
                            )
                        }
                    }

                    // Elapsed Time
                    Text(
                        text = NoteAudioController.formatDuration(recState.elapsedMs),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )

                    // Pause / Resume
                    Surface(
                        shape = CircleShape,
                        color = LernspaceSurfaceLow,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                if (recState.isPaused) {
                                    audioController.resumeRecording()
                                } else {
                                    audioController.pauseRecording()
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (recState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (recState.isPaused) "Lanjutkan" else "Jeda",
                                tint = LernspaceDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Stop & Save Recording
                    Surface(
                        shape = CircleShape,
                        color = AccentRed.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { audioController.stopRecording() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Simpan Rekaman",
                                tint = AccentRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // ── PLAYBACK READY / PLAYING ──
                hasAudio -> {
                    // Play / Pause Button
                    Surface(
                        shape = CircleShape,
                        color = AccentBlue.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { audioController.togglePlayback() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playState.isPlaying) "Jeda" else "Putar Rekaman",
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Playback Progress & Durations
                    Column(
                        modifier = Modifier.widthIn(min = 120.dp, max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Slider(
                            value = playState.currentPositionMs.toFloat(),
                            onValueChange = { audioController.seekTo(it.toInt()) },
                            valueRange = 0f..(playState.totalDurationMs.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(
                                thumbColor = AccentBlue,
                                activeTrackColor = AccentBlue,
                                inactiveTrackColor = LernspaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth().height(18.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = NoteAudioController.formatDuration(playState.currentPositionMs.toLong()),
                                fontSize = 10.sp,
                                color = LernspaceTextSecondary
                            )
                            Text(
                                text = NoteAudioController.formatDuration(playState.totalDurationMs.toLong()),
                                fontSize = 10.sp,
                                color = LernspaceTextSecondary
                            )
                        }
                    }

                    // Re-record (Delete & start fresh)
                    Surface(
                        shape = CircleShape,
                        color = LernspaceSurfaceLow,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                audioController.deleteRecording()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Hapus Rekaman",
                                tint = AccentRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Close Bar
                    Surface(
                        shape = CircleShape,
                        color = LernspaceSurfaceLow,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                audioController.stopPlayback()
                                onClose()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Tutup Player",
                                tint = LernspaceTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // ── READY TO RECORD (Fresh) ──
                else -> {
                    Text(
                        text = "Rekam Suara Pelajaran",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LernspaceDark
                    )

                    Surface(
                        shape = CircleShape,
                        color = AccentRed,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                audioController.startRecording()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.FiberManualRecord,
                                contentDescription = "Mulai Merekam",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = LernspaceSurfaceLow,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onClose)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Batal",
                                tint = LernspaceTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
