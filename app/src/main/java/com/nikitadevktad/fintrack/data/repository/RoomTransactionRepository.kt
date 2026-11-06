package com.nikitadevktad.fintrack.data.repository

import com.nikitadevktad.fintrack.data.local.dao.TransactionDao
import com.nikitadevktad.fintrack.data.local.entity.TransactionEntity
import com.nikitadevktad.fintrack.domain.model.Transaction
import com.nikitadevktad.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomTransactionRepository(
    private val transactionDao: TransactionDao,
    private val awaitInitialization: suspend () -> Unit
) : TransactionRepository {
    override fun observeFiltered(
        startEpochDay: Long?,
        endEpochDay: Long?,
        type: String?,
        categoryId: Long?
    ): Flow<List<Transaction>> =
        transactionDao.observeFiltered(startEpochDay, endEpochDay, type, categoryId)
            .map { transactions -> transactions.map(TransactionEntity::toDomain) }

    override suspend fun getById(id: Long): Transaction? {
        awaitInitialization()
        return transactionDao.getById(id)?.toDomain()
    }

    override suspend fun add(transaction: Transaction) {
        require(transaction.amountInKopecks > 0)
        awaitInitialization()
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun update(transaction: Transaction): Boolean {
        require(transaction.amountInKopecks > 0)
        awaitInitialization()
        return transactionDao.update(transaction.toEntity()) == 1
    }

    override suspend fun deleteById(id: Long): Boolean {
        awaitInitialization()
        return transactionDao.deleteById(id) == 1
    }
}

private fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountInKopecks = amountInKopecks,
    categoryId = categoryId,
    dateEpochDay = date.toEpochDay(),
    note = note
)

private fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amountInKopecks = amountInKopecks,
    categoryId = categoryId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    note = note
)
