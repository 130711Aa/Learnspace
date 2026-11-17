package com.lernspace.app.ui.quiz

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lernspace.app.core.supabase.SupabaseClient
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AddQuestionDialog(
    initialSubject: String? = null,
    onDismiss: () -> Unit,
    onQuestionAdded: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val subjects = AcademicWorkspaceState.subjects

    var subjectName by remember { mutableStateOf(initialSubject ?: subjects.firstOrNull()?.name ?: "Kalkulus Lanjut") }
    var topicTitle by remember { mutableStateOf("") }
    var questionText by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("MEDIUM") }
    var explanation by remember { mutableStateOf("") }

    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctOptionKey by remember { mutableStateOf("A") }

    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = LernspaceDark)
                        Text(
                            text = "Tambah Soal ke Bank Soal Cloud",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = LernspaceTextSecondary)
                    }
                }

                HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.6f))

                // Mata Kuliah & Topik
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Mata Kuliah") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = topicTitle,
                        onValueChange = { topicTitle = it },
                        label = { Text("Topik / Bab") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("misal: Integral Lipat Dua") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Difficulty Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tingkat Kesulitan", fontSize = 12.sp, color = LernspaceTextSecondary, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EASY" to "Mudah (Easy)", "MEDIUM" to "Sedang (Medium)", "HARD" to "Sulit (Hard)").forEach { (key, label) ->
                            FilterChip(
                                selected = difficulty == key,
                                onClick = { difficulty = key },
                                label = { Text(label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Teks Soal
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Pertanyaan (Dukung format LaTeX $...$)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = { Text("Tulis soal di sini. Gunakan $ untuk rumus matematika.") },
                    shape = RoundedCornerShape(10.dp)
                )

                // Pilihan Jawaban
                Text("Pilihan Jawaban (Pilih radio untuk jawaban yang benar):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)

                listOf(
                    Triple("A", optA) { it: String -> optA = it },
                    Triple("B", optB) { it: String -> optB = it },
                    Triple("C", optC) { it: String -> optC = it },
                    Triple("D", optD) { it: String -> optD = it }
                ).forEach { (key, value, setter) ->
                    val isCorrect = correctOptionKey == key
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { correctOptionKey = key }) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = "Pilih $key sebagai jawaban benar",
                                tint = if (isCorrect) LernspaceDark else LernspaceTextSecondary
                            )
                        }
                        OutlinedTextField(
                            value = value,
                            onValueChange = setter,
                            label = { Text("Pilihan $key" + if (isCorrect) " (KUNCI JAWABAN)" else "") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Pembahasan
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Pembahasan / Solusi (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("Langkah-langkah penyelesaian untuk membantu siswa memahami.") },
                    shape = RoundedCornerShape(10.dp)
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (subjectName.isBlank() || topicTitle.isBlank() || questionText.isBlank() || optA.isBlank() || optB.isBlank()) {
                                Toast.makeText(context, "Mata kuliah, topik, soal, dan minimal opsi A & B wajib diisi!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            coroutineScope.launch {
                                val opts = mutableListOf(
                                    optA to (correctOptionKey == "A"),
                                    optB to (correctOptionKey == "B")
                                )
                                if (optC.isNotBlank()) opts.add(optC to (correctOptionKey == "C"))
                                if (optD.isNotBlank()) opts.add(optD to (correctOptionKey == "D"))

                                val res = SupabaseClient.insertQuestion(
                                    subjectName = subjectName.trim(),
                                    topicTitle = topicTitle.trim(),
                                    questionText = questionText.trim(),
                                    difficulty = difficulty,
                                    explanation = explanation.trim(),
                                    options = opts
                                )
                                isSubmitting = false
                                res.onSuccess {
                                    Toast.makeText(context, "Soal berhasil ditambahkan ke Bank Soal Supabase!", Toast.LENGTH_SHORT).show()
                                    onQuestionAdded()
                                    onDismiss()
                                }.onFailure { err ->
                                    Toast.makeText(context, "Gagal: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LernspaceSurfaceCard, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyimpan ke Cloud...")
                        } else {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan ke Bank Soal")
                        }
                    }
                }
            }
        }
    }
}
