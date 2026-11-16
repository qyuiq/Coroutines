package com.example.coroutines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.coroutines.data.AppDatabase
import com.example.coroutines.data.AppRepository
import com.example.coroutines.ui.TaskTrackerApp
import com.example.coroutines.ui.theme.CoroutinesTheme
import com.example.coroutines.viewmodel.TaskTrackerViewModel
import com.example.coroutines.viewmodel.TaskTrackerViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TaskTrackerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "task_tracker.db",
        ).build()
        val repository = AppRepository(database.categoryDao(), database.taskDao())
        viewModel = ViewModelProvider(this, TaskTrackerViewModelFactory(repository))[TaskTrackerViewModel::class.java]
        setContent {
            CoroutinesTheme {
                TaskTrackerApp(viewModel)
            }
        }
    }
}
