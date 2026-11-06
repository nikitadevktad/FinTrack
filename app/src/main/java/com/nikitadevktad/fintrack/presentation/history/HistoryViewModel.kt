package com.nikitadevktad.fintrack.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikitadevktad.fintrack.domain.model.Category
import com.nikitadevktad.fintrack.domain.model.TransactionType
import com.nikitadevktad.fintrack.domain.repository.CategoryRepository
import com.nikitadevktad.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HistoryItem(
    val id: Long,
    val categoryName: String,
    val type: TransactionType,
    val date: LocalDate,
    val amountInKopecks: Long
)

sealed interface HistoryPeriod {
    data object All : HistoryPeriod
    data class CurrentMonth(val start: LocalDate, val end: LocalDate) : HistoryPeriod
    data class Custom(val start: LocalDate, val end: LocalDate) : HistoryPeriod
}

class HistoryViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {
    private val categories = MutableStateFlow<List<Category>>(emptyList())
    private val selectedPeriod = MutableStateFlow<HistoryPeriod>(HistoryPeriod.All)

    val period: StateFlow<HistoryPeriod> = selectedPeriod.asStateFlow()

    val items: StateFlow<List<HistoryItem>> = selectedPeriod.flatMapLatest { selected ->
        when (selected) {
            HistoryPeriod.All -> transactionRepository.observeAll()
            is HistoryPeriod.CurrentMonth -> transactionRepository.observeBetween(
                selected.start.toEpochDay(),
                selected.end.plusDays(1).toEpochDay()
            )
            is HistoryPeriod.Custom -> transactionRepository.observeBetween(
                selected.start.toEpochDay(),
                selected.end.plusDays(1).toEpochDay()
            )
        }
    }
        .combine(categories) { transactions, allCategories ->
            val categoriesById = allCategories.associateBy(Category::id)
            transactions.mapNotNull { transaction ->
                categoriesById[transaction.categoryId]?.let { category ->
                    HistoryItem(
                        id = transaction.id,
                        categoryName = category.name,
                        type = category.type,
                        date = transaction.date,
                        amountInKopecks = transaction.amountInKopecks
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            categories.value = categoryRepository.getAll()
        }
    }

    fun selectAllOperations() {
        selectedPeriod.value = HistoryPeriod.All
    }

    fun selectCurrentMonth() {
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1)
        selectedPeriod.value = HistoryPeriod.CurrentMonth(start, start.plusMonths(1).minusDays(1))
    }

    fun selectCustomPeriod(start: LocalDate, end: LocalDate) {
        require(!start.isAfter(end))
        selectedPeriod.value = HistoryPeriod.Custom(start, end)
    }
}
