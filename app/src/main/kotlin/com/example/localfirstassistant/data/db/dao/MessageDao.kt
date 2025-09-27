package com.example.localfirstassistant.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.localfirstassistant.data.db.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<MessageEntity>): List<Long>

    @Query("SELECT * FROM messages WHERE account_type = :accountType ORDER BY received_at DESC LIMIT :limit")
    fun observeRecent(accountType: String, limit: Int): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE received_at < :olderThan")
    suspend fun pruneOlderThan(olderThan: java.time.Instant)
}
