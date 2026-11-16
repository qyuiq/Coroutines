package com.example.coroutines.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY isCompleted, createdAt DESC")
    fun observeAllWithCategory(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId ORDER BY isCompleted, createdAt DESC")
    fun observeByCategoryWithCategory(categoryId: Long): Flow<List<TaskWithCategory>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}
