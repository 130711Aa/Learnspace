package com.lernspace.app

import android.app.Application
import com.lernspace.app.core.ai.GeminiApiClient
import com.lernspace.app.core.ai.LocalSlmManager
import com.lernspace.app.core.supabase.BankSoalAttemptManager
import com.lernspace.app.data.local.AcademicDatabaseManager
import com.lernspace.app.data.local.AppSettingsManager
import com.lernspace.app.data.local.LernspaceDatabase
import com.lernspace.app.data.repository.SubjectRepository
import com.lernspace.app.data.repository.TaskRepository

class LernspaceApplication : Application() {

    val database: LernspaceDatabase
        get() = LernspaceDatabase.getInstance(this)

    val taskRepository: TaskRepository
        get() = TaskRepository(database.taskDao())

    val subjectRepository: SubjectRepository
        get() = SubjectRepository(database.subjectDao(), database.topicDao())

    override fun onCreate() {
        super.onCreate()
        AppSettingsManager.init(this)
        BankSoalAttemptManager.init(this)
        AcademicDatabaseManager.init(database, this)
        LocalSlmManager.init(this)
        GeminiApiClient.init(this)
    }
}


