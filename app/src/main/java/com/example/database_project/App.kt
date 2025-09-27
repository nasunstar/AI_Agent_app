// App.kt
package com.example.database_project

import android.app.Application
import androidx.room.Room
import com.example.database_project.data.db.AppDatabase

class App : Application() {
    companion object {
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "my_database"
        )
            .fallbackToDestructiveMigration() // 개발 중엔 편리, 나중엔 Migration 정의
            .build()
    }
}
