package com.nikitadevktad.fintrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikitadevktad.fintrack.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY name ASC, id ASC")
    suspend fun getAll(): List<CategoryEntity>
}
