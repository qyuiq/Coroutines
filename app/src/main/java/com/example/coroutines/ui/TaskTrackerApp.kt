package com.example.coroutines.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.coroutines.data.Category
import com.example.coroutines.data.Task
import com.example.coroutines.data.TaskWithCategory
import com.example.coroutines.viewmodel.TaskTrackerViewModel
import com.example.coroutines.viewmodel.UiEvent
import kotlinx.coroutines.flow.collect

private const val CategoriesRoute = "categories"
private const val TasksRoute = "tasks"
private const val EditorRoute = "editor"

@Composable
fun TaskTrackerApp(viewModel: TaskTrackerViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, viewModel.events) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                if (event is UiEvent.Message) snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    NavHost(navController = navController, startDestination = CategoriesRoute) {
        composable(CategoriesRoute) {
            CategoriesScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                openTasks = { navController.navigate(TasksRoute) },
            )
        }
        composable(TasksRoute) {
            TasksScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                openCategories = { navController.popBackStack() },
                openEditor = { taskId -> navController.navigate("$EditorRoute/$taskId") },
            )
        }
        composable("$EditorRoute/{taskId}") { entry ->
            TaskEditorScreen(
                taskId = entry.arguments?.getString("taskId")?.toLongOrNull() ?: 0L,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                close = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesScreen(
    viewModel: TaskTrackerViewModel,
    snackbarHostState: SnackbarHostState,
    openTasks: () -> Unit,
) {
    val categories by viewModel.categoriesWithTaskCount.collectAsStateWithLifecycle()
    var addDialogVisible by rememberSaveable { mutableStateOf(false) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Категории") },
                actions = { TextButtonCompat("Задачи", openTasks) },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { addDialogVisible = true }) { Text("+") } },
    ) { padding ->
        if (categories.isEmpty()) {
            EmptyContent("Добавьте первую категорию для начала работы", Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryItem(
                        category = Category(category.id, category.name, category.colorHex),
                        taskCount = category.taskCount,
                        delete = { deletingCategory = Category(category.id, category.name, category.colorHex) },
                    )
                }
            }
        }
    }
    if (addDialogVisible) AddCategoryDialog(
        dismiss = { addDialogVisible = false },
        save = { name, color -> viewModel.addCategory(name, color); addDialogVisible = false },
    )
    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Удалить категорию?") },
            text = { Text("Все задачи этой категории будут удалены без возможности восстановления.") },
            confirmButton = { Button(onClick = { viewModel.deleteCategory(category); deletingCategory = null }) { Text("Удалить") } },
            dismissButton = { OutlinedButton(onClick = { deletingCategory = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun CategoryItem(category: Category, taskCount: Int, delete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).background(Color(category.colorHex)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text("Задач: $taskCount", style = MaterialTheme.typography.bodyMedium)
            }
            TextButtonCompat("Удалить", delete)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksScreen(
    viewModel: TaskTrackerViewModel,
    snackbarHostState: SnackbarHostState,
    openCategories: () -> Unit,
    openEditor: (Long) -> Unit,
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    var filterExpanded by remember { mutableStateOf(false) }
    var deletingTask by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Задачи") },
                navigationIcon = { TextButtonCompat("Назад", openCategories) },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { openEditor(0L) }) { Text("+") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedButton(onClick = { filterExpanded = true }) {
                    Text(categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Все категории")
                }
                DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                    DropdownMenuItem(text = { Text("Все категории") }, onClick = {
                        viewModel.selectCategory(null); filterExpanded = false
                    })
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category.name) }, onClick = {
                            viewModel.selectCategory(category.id); filterExpanded = false
                        })
                    }
                }
            }
            if (tasks.isEmpty()) EmptyContent("Задач по выбранному фильтру нет", Modifier.weight(1f))
            else LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tasks, key = { it.task.id }) { taskWithCategory ->
                    TaskItem(
                        item = taskWithCategory,
                        changeCompleted = { viewModel.setTaskCompleted(taskWithCategory.task, it) },
                        edit = { openEditor(taskWithCategory.task.id) },
                        delete = { deletingTask = taskWithCategory.task },
                    )
                }
            }
        }
    }
    deletingTask?.let { task ->
        AlertDialog(
            onDismissRequest = { deletingTask = null },
            title = { Text("Удалить задачу?") },
            text = { Text(task.title) },
            confirmButton = { Button(onClick = { viewModel.deleteTask(task); deletingTask = null }) { Text("Удалить") } },
            dismissButton = { OutlinedButton(onClick = { deletingTask = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun TaskItem(
    item: TaskWithCategory,
    changeCompleted: (Boolean) -> Unit,
    edit: () -> Unit,
    delete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = edit)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.task.isCompleted, onCheckedChange = changeCompleted)
            Column(Modifier.weight(1f)) {
                Text(
                    item.task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.task.isCompleted) TextDecoration.LineThrough else null,
                )
                if (item.task.description.isNotBlank()) Text(item.task.description)
                Text(item.category.name, color = Color(item.category.colorHex), style = MaterialTheme.typography.labelLarge)
            }
            TextButtonCompat("Удалить", delete)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorScreen(
    taskId: Long,
    viewModel: TaskTrackerViewModel,
    snackbarHostState: SnackbarHostState,
    close: () -> Unit,
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var existingTask by remember { mutableStateOf<Task?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (taskId != 0L) {
            viewModel.getTask(taskId)?.let { task ->
                existingTask = task
                title = task.title
                description = task.description
                categoryId = task.categoryId
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text(if (taskId == 0L) "Новая задача" else "Редактирование") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Box {
                OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(categories.firstOrNull { it.id == categoryId }?.name ?: "Выберите категорию")
                }
                DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category.name) }, onClick = {
                            categoryId = category.id
                            categoryExpanded = false
                        })
                    }
                }
            }
            Button(
                onClick = {
                    if (existingTask == null) viewModel.addTask(title, description, categoryId)
                    else viewModel.updateTask(existingTask!!, title, description, categoryId)
                    if (title.isNotBlank() && categoryId != null) close()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Сохранить") }
            OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
        }
    }
}

@Composable
private fun AddCategoryDialog(dismiss: () -> Unit, save: (String, Long) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableStateOf(CategoryColors.first()) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Новая категория") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryColors.forEach { color ->
                        Box(
                            Modifier.size(32.dp).background(Color(color)).clickable { selectedColor = color },
                            contentAlignment = Alignment.Center,
                        ) { if (color == selectedColor) Text("✓", color = Color.White) }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { save(name, selectedColor) }) { Text("Создать") } },
        dismissButton = { OutlinedButton(onClick = dismiss) { Text("Отмена") } },
    )
}

@Composable
private fun EmptyContent(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text) }
}

@Composable
private fun TextButtonCompat(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(text) }
}

private val CategoryColors = listOf(0xFF1565C0, 0xFF2E7D32, 0xFF6A1B9A, 0xFFC62828, 0xFFEF6C00)
