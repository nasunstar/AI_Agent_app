package com.example.localfirstassistant.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.localfirstassistant.data.db.entities.TaskEntity
import com.example.localfirstassistant.data.db.entities.TaskStatus
import com.example.localfirstassistant.data.db.entities.TaskWithMessages
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Transaction
    @Query("SELECT * FROM tasks WHERE due_bucket = :bucket ORDER BY due_at ASC NULLS LAST")
    fun observeTasksByBucket(bucket: String): Flow<List<TaskWithMessages>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY due_at ASC NULLS LAST")
    fun observeByStatus(status: TaskStatus): Flow<List<TaskWithMessages>>

    @Query("DELETE FROM tasks WHERE updated_at < :olderThan AND status = :status")
    suspend fun pruneResolved(status: TaskStatus, olderThan: java.time.Instant)
}
