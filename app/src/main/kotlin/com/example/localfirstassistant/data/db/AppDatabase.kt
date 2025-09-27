package com.example.localfirstassistant.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.localfirstassistant.data.db.dao.MessageDao
import com.example.localfirstassistant.data.db.dao.TaskDao
import com.example.localfirstassistant.data.db.dao.TaskMessageDao
import com.example.localfirstassistant.data.db.entities.MessageEntity
import com.example.localfirstassistant.data.db.entities.TaskEntity
import com.example.localfirstassistant.data.db.entities.TaskMessageCrossRef

@Database(
    entities = [MessageEntity::class, TaskEntity::class, TaskMessageCrossRef::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun taskDao(): TaskDao
    abstract fun taskMessageDao(): TaskMessageDao
}
