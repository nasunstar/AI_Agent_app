package com.example.localfirstassistant.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Modifier
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.localfirstassistant.data.db.AppDatabase
import com.example.localfirstassistant.data.db.entities.TaskWithMessages
import com.example.localfirstassistant.worker.mail.ImapSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class TasksWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasksToday = loadTasks(context, "TODAY")
        val tasksWeek = loadTasks(context, "WEEK")
        val tasksMonth = loadTasks(context, "MONTH")
        provideContent {
            GlanceTheme {
                TasksWidgetContent(tasksToday, tasksWeek, tasksMonth)
            }
        }
    }

    private fun loadTasks(context: Context, bucket: String): List<TaskWithMessages> =
        runBlocking(Dispatchers.IO) {
            val database = AppWidgetDatabaseProvider.get(context)
            database.taskDao().observeTasksByBucket(bucket).first()
        }
}

class TasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasksWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val request = PeriodicWorkRequestBuilder<ImapSyncWorker>(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ImapSyncWorker.UNIQUE_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

@Composable
private fun TasksWidgetContent(today: List<TaskWithMessages>, week: List<TaskWithMessages>, month: List<TaskWithMessages>) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(text = "오늘", style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE)))
        TaskList(today)
        Text(text = "이번 주", style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE)))
        TaskList(week)
        Text(text = "이번 달", style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE)))
        TaskList(month)
    }
}

@Composable
private fun TaskList(tasks: List<TaskWithMessages>) {
    if (tasks.isEmpty()) {
        Text(text = "없음", style = TextStyle(color = ColorProvider(android.graphics.Color.LTGRAY)))
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            tasks.take(3).forEach { task ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = task.task.title, style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE)))
                    task.task.dueAt?.let {
                        Text(
                            text = DateTimeFormatter.ofPattern("MM/dd").withZone(ZoneId.of("Asia/Seoul")).format(it),
                            style = TextStyle(color = ColorProvider(android.graphics.Color.LTGRAY))
                        )
                    }
                }
            }
        }
    }
}

private object AppWidgetDatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "localfirst.db"
        ).build().also { instance = it }
    }
}
