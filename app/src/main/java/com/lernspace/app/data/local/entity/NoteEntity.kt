package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index("subjectId"),
        Index("topicId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val snippet: String = "",
    val subject: String = "",
    val subjectId: String? = null,
    val topicId: String? = null,
    val topicName: String? = null,
    val isScratchpad: Boolean = false,
    val paperStyle: String = "RULED",
    val pdfUri: String? = null,
    val pdfTotalPages: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null
)
