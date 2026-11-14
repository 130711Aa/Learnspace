package com.lernspace.app.ui.workspace.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Backward compatibility wrapper delegating to [FloatingGraphWindow].
 */
@Composable
fun InteractiveGraphDialog(
    functionRaw: String,
    isDarkTheme: Boolean = false,
    onDismissRequest: () -> Unit,
    onInsertGraphToNote: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingGraphWindow(
        equationRaw = functionRaw,
        isDarkTheme = isDarkTheme,
        onClose = onDismissRequest,
        onInsertGraphToNote = onInsertGraphToNote,
        modifier = modifier
    )
}
