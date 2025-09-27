package com.example.localfirstassistant.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.localfirstassistant.data.db.entities.TaskMessageCrossRef

@Dao
interface TaskMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: TaskMessageCrossRef)

    @Query("DELETE FROM task_message_cross_ref WHERE task_id = :taskId")
    suspend fun deleteForTask(taskId: Long)
}
