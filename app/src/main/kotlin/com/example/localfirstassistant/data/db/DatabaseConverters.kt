package com.example.localfirstassistant.data.db

import androidx.room.TypeConverter
import com.example.localfirstassistant.data.db.entities.TaskSource
import com.example.localfirstassistant.data.db.entities.TaskStatus
import java.time.Instant

/**
 * Room converters for Java time and enums. Keep deterministic for export/import.
 */
class DatabaseConverters {
    @TypeConverter
    fun fromEpoch(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun toEpoch(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromStatus(name: String?): TaskStatus? = name?.let(TaskStatus::valueOf)

    @TypeConverter
    fun toStatus(status: TaskStatus?): String? = status?.name

    @TypeConverter
    fun fromSource(name: String?): TaskSource? = name?.let(TaskSource::valueOf)

    @TypeConverter
    fun toSource(source: TaskSource?): String? = source?.name
}
