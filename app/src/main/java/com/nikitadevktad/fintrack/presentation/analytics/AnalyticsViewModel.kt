package com.nikitadevktad.fintrack.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikitadevktad.fintrack.domain.model.CategoryExpense
import com.nikitadevktad.fintrack.domain.model.PeriodSummary
import com.nikitadevktad.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AnalyticsPeriod(val start: LocalDate, val end: LocalDate)

sealed interface AnalyticsUiState {
    val period: AnalyticsPeriod

    data class Loading(override val period: AnalyticsPeriod) : AnalyticsUiState
    data class Content(
        override val period: AnalyticsPeriod,
        val summary: PeriodSummary,
        val categoryExpenses: CategoryExpensesUiState
    ) : AnalyticsUiState

    data class Error(override val period: AnalyticsPeriod) : AnalyticsUiState
}

sealed interface CategoryExpensesUiState {
    data object Loading : CategoryExpensesUiState
    data class Content(val expenses: List<CategoryExpense>) : CategoryExpensesUiState
    data object Empty : CategoryExpensesUiState
    data object Error : CategoryExpensesUiState
}

class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(currentMonthPeriod())

    val uiState: StateFlow<AnalyticsUiState> = selectedPeriod.flatMapLatest { period ->
        val startEpochDay = period.start.toEpochDay()
        val endEpochDay = period.end.plusDays(1).toEpochDay()
        val summary = transactionRepository.observePeriodSummary(
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay
        )
            .map<PeriodSummary, SummaryResult> { SummaryResult.Value(it) }
            .catch { emit(SummaryResult.Error) }
        val categoryExpenses = transactionRepository.observeCategoryExpenses(startEpochDay, endEpochDay)
            .map<List<CategoryExpense>, CategoryExpensesUiState> { expenses ->
                if (expenses.isEmpty()) CategoryExpensesUiState.Empty
                else CategoryExpensesUiState.Content(expenses)
            }
            .onStart { emit(CategoryExpensesUiState.Loading) }
            .catch { emit(CategoryExpensesUiState.Error) }
        combine(summary, categoryExpenses) { summaryResult, expensesResult ->
            when (summaryResult) {
                SummaryResult.Error -> AnalyticsUiState.Error(period)
                is SummaryResult.Value -> AnalyticsUiState.Content(period, summaryResult.summary, expensesResult)
            }
        }
            .onStart { emit(AnalyticsUiState.Loading(period)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState.Loading(selectedPeriod.value)
    )

    fun selectCurrentMonth() {
        selectedPeriod.value = currentMonthPeriod()
    }

    fun selectCustomPeriod(start: LocalDate, end: LocalDate): Boolean {
        if (start.isAfter(end)) return false
        selectedPeriod.value = AnalyticsPeriod(start, end)
        return true
    }
}

private sealed interface SummaryResult {
    data class Value(val summary: PeriodSummary) : SummaryResult
    data object Error : SummaryResult
}

private fun currentMonthPeriod(): AnalyticsPeriod {
    val start = LocalDate.now().withDayOfMonth(1)
    return AnalyticsPeriod(start, start.plusMonths(1).minusDays(1))
}
