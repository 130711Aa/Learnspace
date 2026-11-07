package com.lernspace.app

import android.app.Application
import com.lernspace.app.data.local.LernspaceDatabase

class LernspaceApplication : Application() {

    val database: LernspaceDatabase by lazy {
        LernspaceDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
