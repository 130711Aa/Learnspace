package com.lernspace.app.core.ai

data class GeneratedFlashcard(
    val front: String,
    val back: String,
    val hint: String = ""
)
