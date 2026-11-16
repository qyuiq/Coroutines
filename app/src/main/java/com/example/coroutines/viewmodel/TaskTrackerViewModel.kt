package com.example.coroutines.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.coroutines.data.AppRepository
import com.example.coroutines.data.Category
import com.example.coroutines.data.CategoryWithTaskCount
import com.example.coroutines.data.Task
import com.example.coroutines.data.TaskWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
}

class TaskTrackerViewModel(
    private val repository: AppRepository,
) : ViewModel() {
    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categoriesWithTaskCount: StateFlow<List<CategoryWithTaskCount>> =
        repository.observeCategoriesWithTaskCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val tasks: StateFlow<List<TaskWithCategory>> = _selectedCategoryId
        .flatMapLatest { categoryId ->
            if (categoryId == null) repository.observeTasks()
            else repository.observeTasksByCategory(categoryId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun addCategory(name: String, colorHex: Long) {
        if (name.isBlank()) {
            sendMessage("Введите название категории")
            return
        }
        runDatabaseAction("Категория добавлена") { repository.addCategory(name, colorHex) }
    }

    fun deleteCategory(category: Category) {
        runDatabaseAction("Категория и её задачи удалены") { repository.deleteCategory(category) }
    }

    fun addTask(title: String, description: String, categoryId: Long?) {
        when {
            title.isBlank() -> sendMessage("Введите название задачи")
            categoryId == null -> sendMessage("Выберите категорию")
            else -> runDatabaseAction("Задача создана") {
                repository.addTask(title, description, categoryId)
            }
        }
    }

    fun updateTask(task: Task, title: String, description: String, categoryId: Long?) {
        when {
            title.isBlank() -> sendMessage("Введите название задачи")
            categoryId == null -> sendMessage("Выберите категорию")
            else -> runDatabaseAction("Задача сохранена") {
                repository.updateTask(
                    task.copy(
                        title = title.trim(),
                        description = description.trim(),
                        categoryId = categoryId,
                    ),
                )
            }
        }
    }

    fun setTaskCompleted(task: Task, isCompleted: Boolean) {
        runDatabaseAction(if (isCompleted) "Задача выполнена" else "Задача возвращена в работу") {
            repository.setTaskCompleted(task, isCompleted)
        }
    }

    fun deleteTask(task: Task) {
        runDatabaseAction("Задача удалена") { repository.deleteTask(task) }
    }

    suspend fun getTask(id: Long): Task? = withContext(Dispatchers.IO) { repository.getTask(id) }

    private fun sendMessage(text: String) {
        _events.tryEmit(UiEvent.Message(text))
    }

    private fun runDatabaseAction(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) { action() }
                _events.emit(UiEvent.Message(successMessage))
            } catch (exception: Exception) {
                _events.emit(UiEvent.Message("Ошибка: ${exception.message ?: "не удалось выполнить операцию"}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class TaskTrackerViewModelFactory(
    private val repository: AppRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TaskTrackerViewModel::class.java))
        return TaskTrackerViewModel(repository) as T
    }
}
