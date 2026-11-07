package com.nikitadevktad.fintrack.presentation.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikitadevktad.fintrack.domain.model.Category
import com.nikitadevktad.fintrack.domain.model.TransactionType
import com.nikitadevktad.fintrack.domain.repository.CategoryRepository
import com.nikitadevktad.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OverviewItem(
    val id: Long,
    val categoryName: String,
    val type: TransactionType,
    val date: LocalDate,
    val amountInKopecks: Long
)

class OverviewViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {
    private val categories = MutableStateFlow<List<Category>>(emptyList())

    val items = transactionRepository.observeFiltered(
        startEpochDay = null,
        endEpochDay = null,
        type = null,
        categoryId = null
    )
        .combine(categories) { transactions, allCategories ->
            val categoriesById = allCategories.associateBy(Category::id)
            transactions.mapNotNull { transaction ->
                categoriesById[transaction.categoryId]?.let { category ->
                    OverviewItem(
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
}
