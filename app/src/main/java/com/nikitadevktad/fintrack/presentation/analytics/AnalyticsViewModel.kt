package com.nikitadevktad.fintrack.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikitadevktad.fintrack.domain.model.PeriodSummary
import com.nikitadevktad.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AnalyticsPeriod(val start: LocalDate, val end: LocalDate)

sealed interface AnalyticsUiState {
    val period: AnalyticsPeriod

    data class Loading(override val period: AnalyticsPeriod) : AnalyticsUiState
    data class Empty(override val period: AnalyticsPeriod) : AnalyticsUiState
    data class Content(
        override val period: AnalyticsPeriod,
        val summary: PeriodSummary
    ) : AnalyticsUiState

    data class Error(override val period: AnalyticsPeriod) : AnalyticsUiState
}

class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(currentMonthPeriod())

    val uiState: StateFlow<AnalyticsUiState> = selectedPeriod.flatMapLatest { period ->
        transactionRepository.observePeriodSummary(
            startEpochDay = period.start.toEpochDay(),
            endEpochDay = period.end.plusDays(1).toEpochDay()
        )
            .map { summary ->
                if (summary.operationCount == 0L) AnalyticsUiState.Empty(period)
                else AnalyticsUiState.Content(period, summary)
            }
            .onStart { emit(AnalyticsUiState.Loading(period)) }
            .catch { emit(AnalyticsUiState.Error(period)) }
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

private fun currentMonthPeriod(): AnalyticsPeriod {
    val start = LocalDate.now().withDayOfMonth(1)
    return AnalyticsPeriod(start, start.plusMonths(1).minusDays(1))
}
