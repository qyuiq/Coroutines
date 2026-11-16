package com.example.coroutines

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coroutines.data.AppDatabase
import com.example.coroutines.data.Category
import com.example.coroutines.data.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun taskCrudReturnsTaskWithItsCategory() = runBlocking {
        val categoryId = database.categoryDao().insert(Category(name = "Учёба", colorHex = 0xFF1565C0))
        val taskId = database.taskDao().insert(
            Task(title = "Подготовить лабораторную", description = "Room и Flow", categoryId = categoryId),
        )

        val inserted = database.taskDao().getById(taskId)
        requireNotNull(inserted)
        database.taskDao().update(inserted.copy(title = "Сдать лабораторную", isCompleted = true))

        val tasks = database.taskDao().observeAllWithCategory().first()
        assertEquals(1, tasks.size)
        assertEquals("Сдать лабораторную", tasks.single().task.title)
        assertEquals("Учёба", tasks.single().category.name)
        assertEquals(1, database.categoryDao().observeAllWithTaskCount().first().single().taskCount)
    }

    @Test
    fun deletingCategoryCascadesToItsTasks() = runBlocking {
        val category = Category(name = "Дом", colorHex = 0xFF2E7D32)
        val categoryId = database.categoryDao().insert(category)
        val taskId = database.taskDao().insert(Task(title = "Купить продукты", description = "", categoryId = categoryId))

        database.categoryDao().delete(category.copy(id = categoryId))

        assertNull(database.taskDao().getById(taskId))
        assertFalse(database.taskDao().observeAllWithCategory().first().isNotEmpty())
    }
}
