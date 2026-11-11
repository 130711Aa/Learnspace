package com.lernspace.app.core.ai

enum class SlmModelSpec(
    val id: String,
    val displayName: String,
    val badge: String,
    val description: String,
    val fileName: String,
    val downloadUrl: String,
    val approximateSizeLabel: String,
    val expectedBytes: Long
) {
    QWEN_2_5_0_5B(
        id = "qwen2.5-0.5b-q4",
        displayName = "Qwen 2.5 0.5B Instruct",
        badge = "Ringan & Cepat",
        description = "Ukuran compact untuk tablet 6-8 GB RAM. Cepat untuk ringkasan singkat, kartu dasar, dan parsing jadwal.",
        fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
        approximateSizeLabel = "~469 MB",
        expectedBytes = 491_400_032L
    ),
    QWEN_2_5_1_5B(
        id = "qwen2.5-1.5b-q4",
        displayName = "Qwen 2.5 1.5B Instruct",
        badge = "Rekomendasi 8 GB RAM",
        description = "Kualitas Bahasa Indonesia, penjelasan materi, dan pembuatan flashcard jauh lebih akurat. Tetap aman dengan auto-unload.",
        fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
        approximateSizeLabel = "~1.04 GB",
        expectedBytes = 1_117_320_736L
    )
}
