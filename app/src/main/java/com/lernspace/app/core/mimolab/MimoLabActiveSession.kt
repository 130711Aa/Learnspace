package com.lernspace.app.core.mimolab

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared state between Mimo Lab, Mimo AI Assistant, and Note Workspace.
 * Enables zero-latency handoff of floating simulations and canvas snapshots.
 */
object MimoLabActiveSession {

    /**
     * Currently active simulation running in floating Picture-in-Picture window over Note Canvas.
     */
    var activeFloatingSimulation: MimoSimulation? by mutableStateOf(null)

    /**
     * Bitmap snapshot pending to be placed into Note Canvas as an image element.
     */
    var pendingSnapshotBitmap: Bitmap? by mutableStateOf(null)

    fun clearFloatingSimulation() {
        activeFloatingSimulation = null
    }

    fun consumePendingSnapshot(): Bitmap? {
        val bmp = pendingSnapshotBitmap
        pendingSnapshotBitmap = null
        return bmp
    }
}
