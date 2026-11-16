package com.example.coroutines.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao,
) {
    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    fun observeCategoriesWithTaskCount(): Flow<List<CategoryWithTaskCount>> =
        categoryDao.observeAllWithTaskCount()

    fun observeTasks(): Flow<List<TaskWithCategory>> = taskDao.observeAllWithCategory()

    fun observeTasksByCategory(categoryId: Long): Flow<List<TaskWithCategory>> =
        taskDao.observeByCategoryWithCategory(categoryId)

    suspend fun getTask(id: Long): Task? = taskDao.getById(id)

    suspend fun addCategory(name: String, colorHex: Long): Long =
        categoryDao.insert(Category(name = name.trim(), colorHex = colorHex))

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun addTask(
        title: String,
        description: String,
        categoryId: Long,
    ): Long = taskDao.insert(
        Task(title = title.trim(), description = description.trim(), categoryId = categoryId),
    )

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun setTaskCompleted(task: Task, isCompleted: Boolean) =
        taskDao.update(task.copy(isCompleted = isCompleted))

    suspend fun deleteTask(task: Task) = taskDao.delete(task)
}
