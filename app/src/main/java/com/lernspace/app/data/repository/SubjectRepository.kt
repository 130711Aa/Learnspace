package com.lernspace.app.data.repository

import com.lernspace.app.data.local.dao.SubjectDao
import com.lernspace.app.data.local.dao.TopicDao
import com.lernspace.app.data.local.entity.SubjectEntity
import com.lernspace.app.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SubjectRepository(
    private val subjectDao: SubjectDao,
    private val topicDao: TopicDao
) {
    fun getAllActiveSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllActiveSubjects()

    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>> = topicDao.getTopicsForSubject(subjectId)

    suspend fun createSubject(name: String, description: String? = null, colorArgb: Long? = null): String {
        val id = UUID.randomUUID().toString()
        val subject = SubjectEntity(
            id = id,
            name = name,
            description = description,
            colorArgb = colorArgb
        )
        subjectDao.insertSubject(subject)
        return id
    }

    suspend fun createTopic(subjectId: String, name: String, targetDate: String? = null): String {
        val id = UUID.randomUUID().toString()
        val topic = TopicEntity(
            id = id,
            subjectId = subjectId,
            name = name,
            targetDate = targetDate
        )
        topicDao.insertTopic(topic)
        return id
    }

    suspend fun updateTopicStatus(topicId: String, status: String) {
        topicDao.updateStatus(topicId, status)
    }

    suspend fun seedInitialDataIfEmpty() {
        // No-op: clean production state
    }
}

