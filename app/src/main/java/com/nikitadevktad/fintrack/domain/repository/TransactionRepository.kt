package com.nikitadevktad.fintrack.domain.repository

import com.nikitadevktad.fintrack.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeFiltered(
        startEpochDay: Long?,
        endEpochDay: Long?,
        type: String?,
        categoryId: Long?
    ): Flow<List<Transaction>>

    suspend fun getById(id: Long): Transaction?

    /**
     * Идентификатор новой операции назначает вызывающий код.
     */
    suspend fun add(transaction: Transaction)

    suspend fun update(transaction: Transaction): Boolean

    suspend fun deleteById(id: Long): Boolean
}
