package com.nikitadevktad.fintrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikitadevktad.fintrack.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>
}
