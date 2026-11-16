package com.example.coroutines.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Category>>

    @Query(
        "SELECT c.id, c.name, c.colorHex, COUNT(t.id) AS taskCount " +
            "FROM categories c LEFT JOIN tasks t ON c.id = t.categoryId " +
            "GROUP BY c.id ORDER BY c.name COLLATE NOCASE",
    )
    fun observeAllWithTaskCount(): Flow<List<CategoryWithTaskCount>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}
