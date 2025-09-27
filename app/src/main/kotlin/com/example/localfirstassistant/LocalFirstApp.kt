package com.example.localfirstassistant

import android.app.Application
import androidx.work.Configuration
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.localfirstassistant.worker.mail.ImapSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Application entry point. Schedules periodic background sync for widgets and mail ingestion.
 */
class LocalFirstApp : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        scheduleMailSync()
    }

    private fun scheduleMailSync() {
        val workManager = WorkManager.getInstance(this)
        val request = PeriodicWorkRequestBuilder<ImapSyncWorker>(1, TimeUnit.HOURS)
            .addTag(ImapSyncWorker.UNIQUE_WORK_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            ImapSyncWorker.UNIQUE_WORK_TAG,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
}
