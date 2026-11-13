package com.lernspace.app.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

data class AudioRecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitude: Int = 0 // 0 to 32767
)

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val totalDurationMs: Int = 0
)

/**
 * Controller for recording and playing audio lectures tied to a specific note.
 * Stores audio files under files/notes/audio/<noteId>.m4a (AAC/MP4 compressed for small size & high voice clarity).
 */
class NoteAudioController(
    private val context: Context,
    private val noteId: String
) {
    private val audioDir: File
        get() = File(context.filesDir, "notes/audio").apply { if (!exists()) mkdirs() }

    val audioFile: File
        get() = File(audioDir, "$noteId.m4a")

    var recordingState by mutableStateOf(AudioRecordingState())
        private set

    var playbackState by mutableStateOf(AudioPlaybackState())
        private set

    var hasExistingRecording by mutableStateOf(false)
        private set

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var recordingStartTime = 0L
    private var pausedAccumulatedMs = 0L

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var recordingTickerJob: Job? = null
    private var playbackTickerJob: Job? = null

    init {
        checkExistingAudio()
    }

    fun checkExistingAudio(): Boolean {
        val exists = audioFile.exists() && audioFile.length() > 0
        hasExistingRecording = exists
        if (exists && !playbackState.isPlaying && playbackState.totalDurationMs == 0) {
            loadDuration()
        }
        return exists
    }

    private fun loadDuration() {
        if (!audioFile.exists()) return
        try {
            val mp = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
            }
            playbackState = playbackState.copy(totalDurationMs = mp.duration)
            mp.release()
        } catch (_: Exception) {}
    }

    fun startRecording(): Boolean {
        if (recordingState.isRecording) return true
        stopPlayback()

        return try {
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(64000) // 64kbps: clear voice, very small file (~28MB per hour)
            mr.setAudioSamplingRate(44100)
            mr.setOutputFile(audioFile.absolutePath)
            mr.prepare()
            mr.start()

            recorder = mr
            recordingStartTime = SystemClock.elapsedRealtime()
            pausedAccumulatedMs = 0L
            recordingState = AudioRecordingState(isRecording = true, isPaused = false, elapsedMs = 0L)

            startRecordingTicker()
            true
        } catch (e: Exception) {
            android.util.Log.e("NoteAudioController", "Failed to start recording: ${e.message}", e)
            try {
                recorder?.release()
            } catch (_: Exception) {}
            recorder = null
            false
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recordingState.isRecording && !recordingState.isPaused) {
            try {
                recorder?.pause()
                pausedAccumulatedMs += SystemClock.elapsedRealtime() - recordingStartTime
                recordingState = recordingState.copy(isPaused = true)
            } catch (_: Exception) {}
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recordingState.isRecording && recordingState.isPaused) {
            try {
                recorder?.resume()
                recordingStartTime = SystemClock.elapsedRealtime()
                recordingState = recordingState.copy(isPaused = false)
            } catch (_: Exception) {}
        }
    }

    fun stopRecording() {
        if (!recordingState.isRecording) return
        recordingTickerJob?.cancel()
        recordingTickerJob = null

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
            recordingState = AudioRecordingState(isRecording = false)
            checkExistingAudio()
        }
    }

    private fun startRecordingTicker() {
        recordingTickerJob?.cancel()
        recordingTickerJob = scope.launch {
            while (isActive && recordingState.isRecording) {
                delay(60) // ~17 fps for silky smooth live waveform rolling
                if (!recordingState.isPaused) {
                    val currentElapsed = pausedAccumulatedMs + (SystemClock.elapsedRealtime() - recordingStartTime)
                    val amp = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
                    recordingState = recordingState.copy(
                        elapsedMs = currentElapsed,
                        amplitude = amp
                    )
                }
            }
        }
    }

    // ── PLAYBACK ─────────────────────────────────────────────────────────────
    fun togglePlayback() {
        if (playbackState.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        if (recordingState.isRecording) return
        if (!audioFile.exists()) return

        if (player != null && !playbackState.isPlaying) {
            player?.start()
            playbackState = playbackState.copy(isPlaying = true)
            startPlaybackTicker()
            return
        }

        try {
            val mp = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    playbackState = playbackState.copy(isPlaying = false, currentPositionMs = 0)
                    playbackTickerJob?.cancel()
                }
            }
            player = mp
            mp.start()
            playbackState = AudioPlaybackState(
                isPlaying = true,
                currentPositionMs = 0,
                totalDurationMs = mp.duration
            )
            startPlaybackTicker()
        } catch (e: Exception) {
            stopPlayback()
        }
    }

    fun pausePlayback() {
        try {
            player?.pause()
            playbackState = playbackState.copy(isPlaying = false)
            playbackTickerJob?.cancel()
        } catch (_: Exception) {}
    }

    fun seekTo(positionMs: Int) {
        try {
            player?.seekTo(positionMs)
            playbackState = playbackState.copy(currentPositionMs = positionMs)
        } catch (_: Exception) {}
    }

    fun stopPlayback() {
        playbackTickerJob?.cancel()
        playbackTickerJob = null
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            player = null
            playbackState = playbackState.copy(isPlaying = false, currentPositionMs = 0)
        }
    }

    fun deleteRecording() {
        stopPlayback()
        stopRecording()
        if (audioFile.exists()) {
            audioFile.delete()
        }
        hasExistingRecording = false
        playbackState = AudioPlaybackState()
    }

    private fun startPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = scope.launch {
            while (isActive && playbackState.isPlaying) {
                delay(200)
                player?.let { mp ->
                    if (mp.isPlaying) {
                        playbackState = playbackState.copy(
                            currentPositionMs = mp.currentPosition,
                            totalDurationMs = mp.duration
                        )
                    }
                }
            }
        }
    }

    fun release() {
        stopRecording()
        stopPlayback()
        scope.cancel()
    }

    companion object {
        fun formatDuration(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
}
