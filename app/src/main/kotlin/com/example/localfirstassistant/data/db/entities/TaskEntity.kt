package com.example.localfirstassistant.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Normalized task record derived from messages, notifications, or OCR results.
 */
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["status", "due_bucket"])])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "due_at")
    val dueAt: Instant?,
    @ColumnInfo(name = "due_bucket")
    val dueBucket: String,
    @ColumnInfo(name = "score")
    val score: Double,
    @ColumnInfo(name = "status")
    val status: TaskStatus = TaskStatus.PENDING,
    @ColumnInfo(name = "source")
    val source: TaskSource,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)

enum class TaskStatus { PENDING, REVIEW, COMPLETED, SNOOZED }

enum class TaskSource { GMAIL, NAVER, SMS, KAKAO, OCR, OTHER }
