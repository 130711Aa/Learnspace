package com.lernspace.app.ui.workspace.document

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.ui.theme.*

@Composable
fun InsertLinkDialog(
    initialText: String = "",
    onDismiss: () -> Unit,
    onApplyLink: (text: String, url: String) -> Unit
) {
    var linkText by remember { mutableStateOf(initialText) }
    var linkUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(min = 360.dp, max = 440.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, tint = AccentBlue)
                    Text("Sisipkan Tautan (Link)", style = MaterialTheme.typography.titleMedium, color = LernspaceDark)
                }

                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    label = { Text("Teks Tautan") },
                    placeholder = { Text("Contoh: Materi Kuliah Bab 3") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = LernspaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text("Alamat Tautan (URL)") },
                    placeholder = { Text("https://contoh.com/artikel") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = LernspaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = LernspaceTextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val targetText = linkText.ifBlank { linkUrl }
                            val targetUrl = if (!linkUrl.startsWith("http://") && !linkUrl.startsWith("https://")) {
                                "https://$linkUrl"
                            } else linkUrl
                            onApplyLink(targetText, targetUrl)
                            onDismiss()
                        },
                        enabled = linkUrl.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Terapkan Tautan")
                    }
                }
            }
        }
    }
}
