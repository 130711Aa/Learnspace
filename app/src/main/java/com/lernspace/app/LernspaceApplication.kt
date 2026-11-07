package com.lernspace.app

import android.app.Application
import com.lernspace.app.data.local.LernspaceDatabase
import com.lernspace.app.data.repository.SubjectRepository
import com.lernspace.app.data.repository.TaskRepository

class LernspaceApplication : Application() {

    val database: LernspaceDatabase by lazy {
        LernspaceDatabase.getInstance(this)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao())
    }

    val subjectRepository: SubjectRepository by lazy {
        SubjectRepository(database.subjectDao(), database.topicDao())
    }

    override fun onCreate() {
        super.onCreate()
    }
}


