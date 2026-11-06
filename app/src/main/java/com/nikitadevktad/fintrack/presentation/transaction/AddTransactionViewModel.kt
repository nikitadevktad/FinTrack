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
    val saveError: SaveError? = null,
    val deleteError: DeleteError? = null,
    val isLoading: Boolean = false,
    val loadError: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleteConfirmationOpen: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false
) {
    val visibleCategories: List<Category>
        get() = categories.filter { it.type == type }
}

enum class AmountError { EMPTY, INVALID, NOT_POSITIVE, OVERFLOW }

enum class SaveError { FAILED, NOT_FOUND }

enum class DeleteError { FAILED, NOT_FOUND }

class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionId: Long? = null
) : ViewModel() {
    private val _state = MutableStateFlow(AddTransactionState(isLoading = transactionId != null))
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()
    private var originalTransaction: Transaction? = null
    private var isPrefilled = false

    init {
        viewModelScope.launch {
            runCatching {
                val categories = categoryRepository.getAll()
                val transaction: Transaction? = if (transactionId == null) {
                    null
                } else {
                    transactionRepository.getById(transactionId)
                }
                categories to transaction
            }.onSuccess { (categories, transaction) ->
                if (transactionId != null && transaction == null) {
                    _state.value = _state.value.copy(isLoading = false, loadError = true)
                    return@onSuccess
                }
                if (transaction != null) {
                    val category = categories.firstOrNull { it.id == transaction.categoryId }
                    if (category == null) {
                        _state.value = _state.value.copy(isLoading = false, loadError = true)
                        return@onSuccess
                    }
                    originalTransaction = transaction
                    if (!isPrefilled) {
                        isPrefilled = true
                        _state.value = _state.value.copy(
                            type = category.type,
                            amount = formatAmountInput(transaction.amountInKopecks),
                            categoryId = transaction.categoryId,
                            note = transaction.note.orEmpty(),
                            categories = categories,
                            isLoading = false
                        )
                    }
                } else {
                    _state.value = _state.value.copy(categories = categories, isLoading = false)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, loadError = transactionId != null)
            }
        }
    }

    fun selectType(type: TransactionType) {
        val current = _state.value
        val selectedCategory = current.categories.firstOrNull { it.id == current.categoryId }
        _state.value = current.copy(
            type = type,
            categoryId = current.categoryId.takeIf { selectedCategory?.type == type },
            categoryError = false,
            saveError = null
        )
    }

    fun updateAmount(amount: String) {
        _state.value = _state.value.copy(amount = amount, amountError = null, saveError = null)
    }

    fun selectCategory(categoryId: Long) {
        _state.value = _state.value.copy(
            categoryId = categoryId,
            categoryError = false,
            isCategoryDialogOpen = false,
            saveError = null
        )
    }

    fun openCategoryDialog() {
        _state.value = _state.value.copy(isCategoryDialogOpen = true)
    }

    fun closeCategoryDialog() {
        _state.value = _state.value.copy(isCategoryDialogOpen = false)
    }

    fun updateNote(note: String) {
        _state.value = _state.value.copy(note = note, saveError = null)
    }

    fun save() {
        val current = _state.value
        if (current.isSaving || current.isDeleting) return
        val amountResult = parseAmountInKopecks(current.amount)
        val categoryIsValid = current.categoryId in current.visibleCategories.map(Category::id).toSet()
        if (amountResult !is AmountResult.Success || !categoryIsValid) {
            _state.value = current.copy(
                amountError = (amountResult as? AmountResult.Error)?.error,
                categoryError = !categoryIsValid
            )
            return
        }
        _state.value = current.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            val transaction = originalTransaction?.copy(
                amountInKopecks = amountResult.value,
                categoryId = current.categoryId!!,
                note = current.note.trim().ifEmpty { null }
            ) ?: Transaction(
                id = System.currentTimeMillis(),
                amountInKopecks = amountResult.value,
                categoryId = current.categoryId!!,
                date = LocalDate.now(),
                note = current.note.trim().ifEmpty { null }
            )
            if (originalTransaction != null) {
                runCatching { transactionRepository.update(transaction) }
                    .onSuccess { updated ->
                        _state.value = _state.value.copy(
                            isSaving = false,
                            isSaved = updated,
                            saveError = if (updated) null else SaveError.NOT_FOUND
                        )
                    }
                    .onFailure {
                        _state.value = _state.value.copy(isSaving = false, saveError = SaveError.FAILED)
                    }
            } else {
                runCatching { transactionRepository.add(transaction) }
                    .onSuccess {
                        _state.value = _state.value.copy(isSaving = false, isSaved = true)
                    }
                    .onFailure {
                        _state.value = _state.value.copy(isSaving = false, saveError = SaveError.FAILED)
                    }
            }
        }
    }

    fun openDeleteConfirmation() {
        if (originalTransaction != null && !_state.value.isSaving && !_state.value.isDeleting) {
            _state.value = _state.value.copy(isDeleteConfirmationOpen = true, deleteError = null)
        }
    }

    fun closeDeleteConfirmation() {
        if (!_state.value.isDeleting) {
            _state.value = _state.value.copy(isDeleteConfirmationOpen = false)
        }
    }

    fun delete() {
        val transaction = originalTransaction ?: return
        val current = _state.value
        if (current.isSaving || current.isDeleting) return
        _state.value = current.copy(
            isDeleteConfirmationOpen = false,
            isDeleting = true,
            deleteError = null
        )
        viewModelScope.launch {
            runCatching { transactionRepository.deleteById(transaction.id) }
                .onSuccess { deleted ->
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        isDeleted = deleted,
                        deleteError = if (deleted) null else DeleteError.NOT_FOUND
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(isDeleting = false, deleteError = DeleteError.FAILED)
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

private fun formatAmountInput(kopecks: Long): String = "%d,%02d".format(kopecks / 100, kopecks % 100)
