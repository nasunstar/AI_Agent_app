package com.example.localfirstassistant.ui.tasks.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.localfirstassistant.data.db.AppDatabase
import com.example.localfirstassistant.data.db.entities.TaskStatus
import com.example.localfirstassistant.data.db.entities.TaskWithMessages
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel exposing buckets for Today / This Week / This Month to Compose UI.
 */
class TasksViewModel(private val database: AppDatabase) : ViewModel() {

    val todayTasks: StateFlow<List<TaskWithMessages>> =
        database.taskDao().observeTasksByBucket("TODAY").stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weekTasks: StateFlow<List<TaskWithMessages>> =
        database.taskDao().observeTasksByBucket("WEEK").stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val monthTasks: StateFlow<List<TaskWithMessages>> =
        database.taskDao().observeTasksByBucket("MONTH").stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingReview: StateFlow<List<TaskWithMessages>> =
        database.taskDao().observeByStatus(TaskStatus.REVIEW).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun markTaskCompleted(taskId: Long) {
        viewModelScope.launch {
            val dao = database.taskDao()
            val existing = dao.observeByStatus(TaskStatus.PENDING).firstOrNull()?.firstOrNull { it.task.id == taskId }?.task
            existing?.let {
                dao.upsert(it.copy(status = TaskStatus.COMPLETED, updatedAt = java.time.Instant.now()))
            }
        }
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TasksViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
