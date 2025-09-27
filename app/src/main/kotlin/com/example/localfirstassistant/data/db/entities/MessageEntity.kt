package com.example.localfirstassistant.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Stores raw messages or notifications before they are normalized into tasks.
 * messageId + accountType uniquely identifies the source payload and allows idempotent sync.
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["account_type", "message_id"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "account_type")
    val accountType: String,
    @ColumnInfo(name = "message_id")
    val messageId: String,
    val subject: String?,
    val sender: String?,
    @ColumnInfo(name = "received_at")
    val receivedAt: Instant,
    val body: String,
    @ColumnInfo(name = "source_payload")
    val sourcePayload: String? = null,
    @ColumnInfo(name = "ingested_at")
    val ingestedAt: Instant = Instant.now()
)
