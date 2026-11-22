package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lernspace.app.core.mimolab.MimoSimulation
import com.lernspace.app.core.mimolab.MimoSubject

@Entity(
    tableName = "mimo_simulations",
    indices = [
        Index("subject"),
        Index("createdAt"),
        Index("isFavorite")
    ]
)
data class MimoSimulationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subject: String,
    val conceptTopic: String,
    val description: String,
    val htmlContent: String,
    val isFavorite: Boolean = false,
    val isCommunityOrigin: Boolean = false,
    val authorName: String = "Mimo AI",
    val likesCount: Int = 0,
    val viewsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

fun MimoSimulationEntity.toDomain(): MimoSimulation {
    return MimoSimulation(
        id = id,
        title = title,
        subject = MimoSubject.fromString(subject),
        conceptTopic = conceptTopic,
        description = description,
        htmlContent = htmlContent,
        isFavorite = isFavorite,
        isCommunityOrigin = isCommunityOrigin,
        authorName = authorName,
        likesCount = likesCount,
        viewsCount = viewsCount,
        createdAt = createdAt
    )
}

fun MimoSimulation.toEntity(): MimoSimulationEntity {
    return MimoSimulationEntity(
        id = id,
        title = title,
        subject = subject.name,
        conceptTopic = conceptTopic,
        description = description,
        htmlContent = htmlContent,
        isFavorite = isFavorite,
        isCommunityOrigin = isCommunityOrigin,
        authorName = authorName,
        likesCount = likesCount,
        viewsCount = viewsCount,
        createdAt = createdAt
    )
}
