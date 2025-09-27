package com.example.localfirstassistant.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Links a normalized task back to the raw message that produced it. Allows traceability.
 */
@Entity(
    tableName = "task_message_cross_ref",
    primaryKeys = ["task_id", "message_id"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["message_id"])]
)
data class TaskMessageCrossRef(
    val task_id: Long,
    val message_id: Long
)
