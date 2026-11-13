package com.lernspace.app.ui.workspace.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lernspace.app.core.audio.NoteAudioController
import com.lernspace.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.ln

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
    val context = LocalContext.current
    val recState = audioController.recordingState
    val playState = audioController.playbackState
    val hasAudio = audioController.hasExistingRecording

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = audioController.startRecording()
            if (!started) {
                Toast.makeText(context, "Gagal memulai perekaman audio", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin mikrofon diperlukan untuk merekam suara pelajaran", Toast.LENGTH_SHORT).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

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
                    // Breathing red / amber indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer {
                                if (!recState.isPaused) {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                            }
                            .clip(CircleShape)
                            .background(if (recState.isPaused) AccentAmber else AccentRed)
                    )

                    // Dynamic multi-bar scrolling audio waveform
                    LiveWaveformVisualizer(
                        amplitude = recState.amplitude,
                        isPaused = recState.isPaused
                    )

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
                                val hasMicPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasMicPermission) {
                                    val started = audioController.startRecording()
                                    if (!started) {
                                        Toast.makeText(context, "Gagal memulai perekaman audio", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
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

/**
 * Modern, organic audio waveform visualizer.
 * Renders a rolling sequence of soundwave bars driven by logarithmic loudness.
 */
@Composable
private fun LiveWaveformVisualizer(
    amplitude: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 14
    val history = remember {
        mutableStateListOf<Float>().apply {
            repeat(barCount) { add(0.12f) }
        }
    }

    val currentAmplitude by rememberUpdatedState(amplitude)
    val currentIsPaused by rememberUpdatedState(isPaused)

    // Continuous 50ms (20 FPS) animation loop: runs continuously so bars ALWAYS scroll smoothly
    LaunchedEffect(Unit) {
        var smoothedPeak = 0.12f
        while (isActive) {
            delay(50L) // 20 updates per second

            if (currentIsPaused) {
                smoothedPeak = maxOf(0.08f, smoothedPeak * 0.80f)
                if (history.size >= barCount) {
                    history.removeAt(0)
                }
                history.add(0.08f)
            } else {
                val amp = currentAmplitude.coerceAtLeast(0)
                // Logarithmic conversion: ln(1 + amp) / ln(1 + 32767)
                // Maps 0..32767 to 0.0..1.0 with high sensitivity for normal human speech (800 - 8000)
                val rawNorm = (ln(1.0 + amp) / ln(1.0 + 32767.0)).toFloat()
                val targetLoudness = (rawNorm * 1.50f).coerceIn(0.12f, 1.0f)

                // Instant attack (spikes up immediately on syllable), smooth exponential decay
                smoothedPeak = if (targetLoudness > smoothedPeak) {
                    targetLoudness
                } else {
                    maxOf(0.12f, smoothedPeak * 0.74f)
                }

                // Add subtle organic micro-frequency flutter (±10%) so bars ripple like a real audio wave
                val flutter = 0.90f + (kotlin.random.Random.nextFloat() * 0.20f)
                val finalSample = (smoothedPeak * flutter).coerceIn(0.12f, 1.0f)

                if (history.size >= barCount) {
                    history.removeAt(0)
                }
                history.add(finalSample)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = modifier
            .width(68.dp)
            .height(20.dp)
    ) {
        history.forEachIndexed { index, targetHeightFraction ->
            val animatedHeight by animateFloatAsState(
                targetValue = targetHeightFraction,
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = 700f
                ),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedHeight.coerceIn(0.10f, 1f))
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isPaused) AccentAmber else AccentRed)
            )
        }
    }
}
