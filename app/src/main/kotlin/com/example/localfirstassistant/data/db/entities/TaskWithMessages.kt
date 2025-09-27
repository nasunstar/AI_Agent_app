package com.example.localfirstassistant.data.db.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** Composite relation used for UI and processing. */
data class TaskWithMessages(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskMessageCrossRef::class,
            parentColumn = "task_id",
            entityColumn = "message_id"
        )
    )
    val messages: List<MessageEntity>
)
