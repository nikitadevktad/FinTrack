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

data class HistoryFilters(
    val period: HistoryPeriod = HistoryPeriod.All,
    val type: TransactionType? = null,
    val categoryId: Long? = null
)

class HistoryViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {
    private val categories = MutableStateFlow<List<Category>>(emptyList())
    private val selectedFilters = MutableStateFlow(HistoryFilters())

    val filters: StateFlow<HistoryFilters> = selectedFilters.asStateFlow()
    val availableCategories: StateFlow<List<Category>> = categories.asStateFlow()

    val items: StateFlow<List<HistoryItem>> = selectedFilters.flatMapLatest { filters ->
        val (startEpochDay, endEpochDay) = filters.period.toEpochDayRange()
        transactionRepository.observeFiltered(
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay,
            type = filters.type?.name,
            categoryId = filters.categoryId
        )
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
        selectedFilters.value = selectedFilters.value.copy(period = HistoryPeriod.All)
    }

    fun selectCurrentMonth() {
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1)
        selectedFilters.value = selectedFilters.value.copy(
            period = HistoryPeriod.CurrentMonth(start, start.plusMonths(1).minusDays(1))
        )
    }

    fun selectCustomPeriod(start: LocalDate, end: LocalDate) {
        require(!start.isAfter(end))
        selectedFilters.value = selectedFilters.value.copy(period = HistoryPeriod.Custom(start, end))
    }

    fun selectType(type: TransactionType?) {
        val current = selectedFilters.value
        val selectedCategory = categories.value.firstOrNull { it.id == current.categoryId }
        selectedFilters.value = current.copy(
            type = type,
            categoryId = current.categoryId.takeIf { type == null || selectedCategory?.type == type }
        )
    }

    fun selectCategory(categoryId: Long?) {
        val category = categories.value.firstOrNull { it.id == categoryId }
        if (categoryId == null || category?.type == selectedFilters.value.type || selectedFilters.value.type == null) {
            selectedFilters.value = selectedFilters.value.copy(categoryId = categoryId)
        }
    }

    fun resetFilters() {
        selectedFilters.value = HistoryFilters()
    }
}

private fun HistoryPeriod.toEpochDayRange(): Pair<Long?, Long?> = when (this) {
    HistoryPeriod.All -> null to null
    is HistoryPeriod.CurrentMonth -> start.toEpochDay() to end.plusDays(1).toEpochDay()
    is HistoryPeriod.Custom -> start.toEpochDay() to end.plusDays(1).toEpochDay()
}
