package com.example.localfirstassistant.worker.mail

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.localfirstassistant.data.auth.TokenStore
import com.example.localfirstassistant.data.db.AppDatabase
import com.example.localfirstassistant.data.db.entities.MessageEntity
import com.example.localfirstassistant.data.db.entities.TaskMessageCrossRef
import com.example.localfirstassistant.data.db.entities.TaskSource
import com.example.localfirstassistant.data.parse.TaskNormalizer
import javax.mail.BodyPart
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.MimeUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Properties

/**
 * Periodically synchronizes Gmail (OAuth2) and Naver (app password) inboxes.
 * Only the latest 10 messages are normalized into tasks to keep the footprint low.
 */
class ImapSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val database by lazy {
        AppDatabaseProvider.get(appContext)
    }
    private val tokenStore by lazy { TokenStore.getInstance(appContext) }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            tokenStore.readGmailTokens()?.let { tokens ->
                if (tokens.isExpired()) {
                    // TODO Refresh token using OAuth2 refresh grant. Requires client secret on secure backend.
                    return@let
                }
                val email = tokens.email ?: return@let
                syncGmail(email, tokens.accessToken)
            }
            tokenStore.readNaverCredentials()?.let { credentials ->
                syncNaver(credentials.username, credentials.appPassword)
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    private fun syncGmail(email: String, accessToken: String) {
        val props = Properties().apply {
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.auth.mechanisms", "XOAUTH2")
        }
        val session = Session.getInstance(props)
        val store = session.getStore("imaps")
        store.connect("imap.gmail.com", 993, email, accessToken)
        store.use {
            val inbox = it.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            val messages = inbox.getMessages(Math.max(1, inbox.messageCount - 9), inbox.messageCount)
            messages.forEach { message ->
                persistMessage(message, TaskSource.GMAIL)
            }
            inbox.close(false)
        }
    }

    private fun syncNaver(username: String, appPassword: String) {
        val props = Properties().apply {
            put("mail.imap.ssl.enable", "true")
        }
        val session = Session.getInstance(props)
        val store = session.getStore("imap")
        store.connect("imap.naver.com", 993, username, appPassword)
        store.use {
            val inbox = it.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            val messages = inbox.getMessages(Math.max(1, inbox.messageCount - 9), inbox.messageCount)
            messages.forEach { message ->
                persistMessage(message, TaskSource.NAVER)
            }
            inbox.close(false)
        }
    }

    private fun persistMessage(message: Message, source: TaskSource) {
        val messageId = message.messageNumber.toString()
        val body = extractBody(message)
        val subject = decodeText(message.subject)
        val sender = message.from?.firstOrNull()?.toString()
        val receivedAt = message.receivedDate?.toInstant() ?: Instant.now()

        val messageRowId = database.messageDao().insert(
            MessageEntity(
                accountType = source.name,
                messageId = "$source-$messageId",
                subject = subject,
                sender = sender,
                receivedAt = receivedAt,
                body = body
            )
        )

        val normalized = TaskNormalizer.normalize(
            title = subject ?: body.take(80),
            body = body,
            source = source,
            reference = receivedAt
        )
        val taskId = database.taskDao().upsert(normalized)
        database.taskMessageDao().insert(
            TaskMessageCrossRef(
                task_id = taskId,
                message_id = messageRowId
            )
        )
    }

    private fun extractBody(message: Message): String {
        return when (val content = message.content) {
            is String -> content
            is Multipart -> {
                val builder = StringBuilder()
                for (i in 0 until content.count) {
                    val part = content.getBodyPart(i)
                    if (part.disposition == null || part.disposition.equals("inline", true)) {
                        builder.append(extractPart(part))
                    }
                }
                builder.toString()
            }
            else -> ""
        }
    }

    private fun extractPart(part: BodyPart): String {
        val content = part.content
        return when (content) {
            is String -> content
            is Multipart -> {
                val sb = StringBuilder()
                for (i in 0 until content.count) {
                    sb.append(extractPart(content.getBodyPart(i)))
                }
                sb.toString()
            }
            else -> ""
        }
    }

    private fun decodeText(text: String?): String? = text?.let { MimeUtility.decodeText(it) }

    companion object {
        const val UNIQUE_WORK_TAG = "imap_sync"
    }
}

private object AppDatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: androidx.room.Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "localfirst.db"
        ).build().also { instance = it }
    }
}
