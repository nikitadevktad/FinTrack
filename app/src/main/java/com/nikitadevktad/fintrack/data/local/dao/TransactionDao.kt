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

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN categories.type = 'INCOME' THEN transactions.amountInKopecks ELSE 0 END), 0) AS incomeInKopecks,
            COALESCE(SUM(CASE WHEN categories.type = 'EXPENSE' THEN transactions.amountInKopecks ELSE 0 END), 0) AS expenseInKopecks,
            COUNT(transactions.id) AS operationCount
        FROM transactions
        INNER JOIN categories ON transactions.categoryId = categories.id
        WHERE transactions.dateEpochDay >= :startEpochDay
          AND transactions.dateEpochDay < :endEpochDay
        """
    )
    fun observePeriodSummary(startEpochDay: Long, endEpochDay: Long): Flow<PeriodSummaryRow>

    @Query(
        """
        SELECT
            categories.id AS categoryId,
            categories.name AS categoryName,
            SUM(transactions.amountInKopecks) AS amountInKopecks
        FROM transactions
        INNER JOIN categories ON transactions.categoryId = categories.id
        WHERE categories.type = 'EXPENSE'
          AND transactions.dateEpochDay >= :startEpochDay
          AND transactions.dateEpochDay < :endEpochDay
        GROUP BY categories.id, categories.name
        ORDER BY amountInKopecks DESC, categoryName ASC, categoryId ASC
        """
    )
    fun observeCategoryExpenses(startEpochDay: Long, endEpochDay: Long): Flow<List<CategoryExpenseRow>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
