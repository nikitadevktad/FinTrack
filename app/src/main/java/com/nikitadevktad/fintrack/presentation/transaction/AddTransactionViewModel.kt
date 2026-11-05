package com.nikitadevktad.fintrack.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikitadevktad.fintrack.domain.model.Category
import com.nikitadevktad.fintrack.domain.model.Transaction
import com.nikitadevktad.fintrack.domain.model.TransactionType
import com.nikitadevktad.fintrack.domain.repository.CategoryRepository
import com.nikitadevktad.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddTransactionState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val amountError: AmountError? = null,
    val categoryError: Boolean = false,
    val isCategoryDialogOpen: Boolean = false,
    val saveError: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
) {
    val visibleCategories: List<Category>
        get() = categories.filter { it.type == type }
}

enum class AmountError { EMPTY, INVALID, NOT_POSITIVE, OVERFLOW }

class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(categories = categoryRepository.getAll())
        }
    }

    fun selectType(type: TransactionType) {
        val current = _state.value
        val selectedCategory = current.categories.firstOrNull { it.id == current.categoryId }
        _state.value = current.copy(
            type = type,
            categoryId = current.categoryId.takeIf { selectedCategory?.type == type },
            categoryError = false,
            saveError = false
        )
    }

    fun updateAmount(amount: String) {
        _state.value = _state.value.copy(amount = amount, amountError = null, saveError = false)
    }

    fun selectCategory(categoryId: Long) {
        _state.value = _state.value.copy(
            categoryId = categoryId,
            categoryError = false,
            isCategoryDialogOpen = false,
            saveError = false
        )
    }

    fun openCategoryDialog() {
        _state.value = _state.value.copy(isCategoryDialogOpen = true)
    }

    fun closeCategoryDialog() {
        _state.value = _state.value.copy(isCategoryDialogOpen = false)
    }

    fun updateNote(note: String) {
        _state.value = _state.value.copy(note = note, saveError = false)
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        val amountResult = parseAmountInKopecks(current.amount)
        val categoryIsValid = current.categoryId in current.visibleCategories.map(Category::id).toSet()
        if (amountResult !is AmountResult.Success || !categoryIsValid) {
            _state.value = current.copy(
                amountError = (amountResult as? AmountResult.Error)?.error,
                categoryError = !categoryIsValid
            )
            return
        }
        _state.value = current.copy(isSaving = true, saveError = false)
        viewModelScope.launch {
            runCatching {
                transactionRepository.add(
                    Transaction(
                        id = System.currentTimeMillis(),
                        amountInKopecks = amountResult.value,
                        categoryId = current.categoryId!!,
                        date = LocalDate.now(),
                        note = current.note.trim().ifEmpty { null }
                    )
                )
            }.onSuccess {
                _state.value = _state.value.copy(isSaving = false, isSaved = true)
            }.onFailure {
                _state.value = _state.value.copy(isSaving = false, saveError = true)
            }
        }
    }
}

private sealed interface AmountResult {
    data class Success(val value: Long) : AmountResult
    data class Error(val error: AmountError) : AmountResult
}

private fun parseAmountInKopecks(input: String): AmountResult {
    if (input.isBlank()) return AmountResult.Error(AmountError.EMPTY)
    if (!input.matches(Regex("\\d+(?:[.,]\\d{0,2})?"))) {
        return AmountResult.Error(AmountError.INVALID)
    }
    val normalized = input.replace(',', '.')
    val parts = normalized.split('.')
    val rubles = parts[0].toLongOrNull() ?: return AmountResult.Error(AmountError.OVERFLOW)
    val kopecks = parts.getOrNull(1).orEmpty().padEnd(2, '0').toLong()
    val result = rubles.checkedTimes(100)?.checkedPlus(kopecks)
        ?: return AmountResult.Error(AmountError.OVERFLOW)
    return if (result == 0L) AmountResult.Error(AmountError.NOT_POSITIVE) else AmountResult.Success(result)
}

private fun Long.checkedTimes(other: Long): Long? = try {
    Math.multiplyExact(this, other)
} catch (_: ArithmeticException) {
    null
}

private fun Long.checkedPlus(other: Long): Long? = try {
    Math.addExact(this, other)
} catch (_: ArithmeticException) {
    null
}
