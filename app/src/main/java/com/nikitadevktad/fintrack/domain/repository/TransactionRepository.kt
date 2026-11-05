package com.nikitadevktad.fintrack.domain.repository

import com.nikitadevktad.fintrack.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>

    /**
     * Идентификатор новой операции назначает вызывающий код.
     */
    suspend fun add(transaction: Transaction)
}
