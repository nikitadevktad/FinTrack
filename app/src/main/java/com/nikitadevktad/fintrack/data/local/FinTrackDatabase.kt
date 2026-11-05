package com.nikitadevktad.fintrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nikitadevktad.fintrack.data.local.dao.CategoryDao
import com.nikitadevktad.fintrack.data.local.dao.TransactionDao
import com.nikitadevktad.fintrack.data.local.entity.CategoryEntity
import com.nikitadevktad.fintrack.data.local.entity.TransactionEntity

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class FinTrackDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao
}
