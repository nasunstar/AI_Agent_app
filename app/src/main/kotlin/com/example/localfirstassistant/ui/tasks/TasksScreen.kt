package com.example.localfirstassistant.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localfirstassistant.data.db.entities.TaskWithMessages
import com.example.localfirstassistant.ui.tasks.state.TasksViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd HH:mm").withZone(ZoneId.of("Asia/Seoul"))

@Composable
fun TasksScreen(viewModel: TasksViewModel) {
    val today by viewModel.todayTasks.collectAsState()
    val week by viewModel.weekTasks.collectAsState()
    val month by viewModel.monthTasks.collectAsState()
    val review by viewModel.pendingReview.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BucketSection(title = "오늘 할 일", tasks = today, onAction = viewModel::markTaskCompleted) }
        item { BucketSection(title = "이번 주", tasks = week, onAction = viewModel::markTaskCompleted) }
        item { BucketSection(title = "이번 달", tasks = month, onAction = viewModel::markTaskCompleted) }
        item {
            if (review.isNotEmpty()) {
                Text(text = "검토 필요", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                review.forEach { task ->
                    TaskCard(task = task, onAction = viewModel::markTaskCompleted, actionLabel = "검토 완료")
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BucketSection(title: String, tasks: List<TaskWithMessages>, onAction: (Long) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text(text = "등록된 작업이 없습니다", style = MaterialTheme.typography.bodyMedium)
        } else {
            tasks.forEachIndexed { index, task ->
                TaskCard(task = task, onAction = onAction)
                if (index < tasks.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskWithMessages, onAction: (Long) -> Unit, actionLabel: String = "완료") {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = task.task.title, style = MaterialTheme.typography.titleMedium)
            task.task.description?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            task.task.dueAt?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "기한: ${timeFormatter.format(it)}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (task.messages.isNotEmpty()) {
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "원본 ${task.messages.size}건", style = MaterialTheme.typography.labelSmall)
                task.messages.firstOrNull()?.let { message ->
                    Text(text = message.subject ?: message.body.take(120))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ElevatedButton(onClick = { onAction(task.task.id) }) {
                Text(actionLabel)
            }
        }
    }
}
