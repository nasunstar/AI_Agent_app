// data/db/AppDatabase.kt
package com.example.database_project.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// --- DAO들 ---
import com.example.database_project.dao.EventDao
import com.example.database_project.dao.UserDao
import com.example.database_project.data.dao.NoteDao
import com.example.database_project.dao.ContactDao           // ✅ 추가
import com.example.database_project.dao.EventTypeDao        // ✅ 추가

// --- Entity들 ---
// (Note는 data.entity에 있으니 기존 import 그대로 유지)
import com.example.database_project.data.entity.Note

import com.example.database_project.entity.Contact
import com.example.database_project.entity.Event
import com.example.database_project.entity.EventDetail
import com.example.database_project.entity.EventNotification
import com.example.database_project.entity.EventType
import com.example.database_project.entity.User

@Database(
    entities = [
        User::class,
        EventType::class,
        Contact::class,
        Event::class,
        EventDetail::class,
        EventNotification::class,
        Note::class            // ✅ EmailSummary 삭제
    ],
    version = 5,               // ✅ 스키마 변경했으니 버전 5로 올림
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    // --- DAO getters ---
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao          // ✅ 추가
    abstract fun eventTypeDao(): EventTypeDao      // ✅ 추가
    abstract fun eventDao(): EventDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // 개발 단계 편의: 스키마 바뀌면 DB 드랍 후 재생성
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
