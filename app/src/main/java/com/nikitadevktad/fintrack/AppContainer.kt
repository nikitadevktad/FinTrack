package com.nikitadevktad.fintrack

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.nikitadevktad.fintrack.data.local.FinTrackDatabase
import com.nikitadevktad.fintrack.data.local.entity.CategoryEntity
import com.nikitadevktad.fintrack.data.repository.RoomCategoryRepository
import com.nikitadevktad.fintrack.data.repository.RoomTransactionRepository
import com.nikitadevktad.fintrack.domain.model.TransactionType
import com.nikitadevktad.fintrack.domain.repository.CategoryRepository
import com.nikitadevktad.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        FinTrackDatabase::class.java,
        "fintrack.db"
    ).build()

    private val initialization = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
        database.withTransaction {
            database.categoryDao().insertAll(initialCategories)
        }
    }

    val categoryRepository: CategoryRepository = RoomCategoryRepository(
        categoryDao = database.categoryDao(),
        awaitInitialization = initialization::await
    )

    val transactionRepository: TransactionRepository = RoomTransactionRepository(
        transactionDao = database.transactionDao(),
        awaitInitialization = initialization::await
    )
}

private val initialCategories = listOf(
    CategoryEntity(id = 1, name = "Зарплата", type = TransactionType.INCOME.name),
    CategoryEntity(id = 2, name = "Другие доходы", type = TransactionType.INCOME.name),
    CategoryEntity(id = 3, name = "Продукты", type = TransactionType.EXPENSE.name),
    CategoryEntity(id = 4, name = "Транспорт", type = TransactionType.EXPENSE.name),
    CategoryEntity(id = 5, name = "Другие расходы", type = TransactionType.EXPENSE.name)
)
