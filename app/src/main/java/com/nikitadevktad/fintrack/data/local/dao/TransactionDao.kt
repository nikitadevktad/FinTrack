package com.nikitadevktad.fintrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nikitadevktad.fintrack.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Query(
        """
        SELECT transactions.* FROM transactions
        INNER JOIN categories ON transactions.categoryId = categories.id
        WHERE (:startEpochDay IS NULL OR transactions.dateEpochDay >= :startEpochDay)
          AND (:endEpochDay IS NULL OR transactions.dateEpochDay < :endEpochDay)
          AND (:type IS NULL OR categories.type = :type)
          AND (:categoryId IS NULL OR transactions.categoryId = :categoryId)
        ORDER BY transactions.dateEpochDay DESC, transactions.id DESC
        """
    )
    fun observeFiltered(
        startEpochDay: Long?,
        endEpochDay: Long?,
        type: String?,
        categoryId: Long?
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
