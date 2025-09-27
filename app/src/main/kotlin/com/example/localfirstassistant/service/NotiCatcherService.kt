package com.example.localfirstassistant.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.localfirstassistant.data.db.AppDatabase
import com.example.localfirstassistant.data.db.entities.MessageEntity
import com.example.localfirstassistant.data.db.entities.TaskMessageCrossRef
import com.example.localfirstassistant.data.db.entities.TaskSource
import com.example.localfirstassistant.data.parse.TaskNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Captures SMS / KakaoTalk / Mail app notifications and normalizes them to tasks in the same pipeline.
 */
class NotiCatcherService : NotificationListenerService() {

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val database by lazy { AppDatabaseProvider.get(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val packageName = sbn.packageName ?: return
        val source = when {
            packageName.contains("sms", ignoreCase = true) -> TaskSource.SMS
            packageName.contains("kakao", ignoreCase = true) -> TaskSource.KAKAO
            packageName.contains("mail", ignoreCase = true) -> TaskSource.OTHER
            else -> return
        }
        val extras = sbn.notification.extras
        val title = extras.getString(NotificationCompat.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() ?: return
        val body = listOfNotNull(title, text, extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString())
            .joinToString("\n")
        scope.launch {
            val messageId = database.messageDao().insert(
                MessageEntity(
                    accountType = source.name,
                    messageId = "${source.name}-${sbn.key}",
                    subject = title,
                    sender = null,
                    receivedAt = Instant.ofEpochMilli(sbn.postTime),
                    body = body
                )
            )
            val normalized = TaskNormalizer.normalize(
                title = title,
                body = body,
                source = source,
                reference = Instant.ofEpochMilli(sbn.postTime)
            )
            val taskId = database.taskDao().upsert(normalized)
            database.taskMessageDao().insert(
                TaskMessageCrossRef(
                    task_id = taskId,
                    message_id = messageId
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

private object AppDatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: android.content.Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "localfirst.db"
        ).build().also { instance = it }
    }
}
