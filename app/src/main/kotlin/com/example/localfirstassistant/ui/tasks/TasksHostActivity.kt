package com.example.localfirstassistant.ui.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.localfirstassistant.data.db.AppDatabase
import com.example.localfirstassistant.ui.tasks.state.TasksViewModel

/**
 * Hosts the Compose task dashboard shown after authentication.
 */
class TasksHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "localfirst.db"
        ).build()
        setContent {
            MaterialTheme {
                Surface {
                    val vm: TasksViewModel = viewModel(factory = TasksViewModel.Factory(database))
                    TasksScreen(viewModel = vm)
                }
            }
        }
    }
}
