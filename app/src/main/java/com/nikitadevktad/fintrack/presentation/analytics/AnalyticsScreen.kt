package com.nikitadevktad.fintrack.presentation.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nikitadevktad.fintrack.FinTrackApplication
import com.nikitadevktad.fintrack.R
import com.nikitadevktad.fintrack.domain.model.CategoryExpense
import com.nikitadevktad.fintrack.domain.model.PeriodSummary
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier) {
    val appContainer = (LocalContext.current.applicationContext as FinTrackApplication).appContainer
    val viewModel: AnalyticsViewModel = viewModel(factory = viewModelFactory {
        initializer { AnalyticsViewModel(appContainer.transactionRepository) }
    })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isPeriodDialogOpen = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.analytics_period_label), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(
                R.string.analytics_period_dates,
                state.period.start.format(analyticsDateFormatter),
                state.period.end.format(analyticsDateFormatter)
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { isPeriodDialogOpen.value = true }) {
                Text(stringResource(R.string.analytics_choose_period))
            }
            TextButton(onClick = viewModel::selectCurrentMonth) {
                Text(stringResource(R.string.analytics_current_month))
            }
        }
        when (val current = state) {
            is AnalyticsUiState.Loading -> AnalyticsMessage(R.string.analytics_loading)
            is AnalyticsUiState.Error -> AnalyticsMessage(R.string.analytics_error, isError = true)
            is AnalyticsUiState.Content -> {
                Summary(current.summary)
                CategoryExpenses(current.categoryExpenses)
            }
        }
    }
    if (isPeriodDialogOpen.value) {
        AnalyticsPeriodDialog(
            initialPeriod = state.period,
            onConfirm = viewModel::selectCustomPeriod,
            onDismiss = { isPeriodDialogOpen.value = false }
        )
    }
}

@Composable
private fun AnalyticsMessage(textRes: Int, isError: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun Summary(summary: PeriodSummary) {
    Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryValue(R.string.analytics_income, formatRubles(summary.incomeInKopecks))
        SummaryValue(R.string.analytics_expense, formatRubles(summary.expenseInKopecks))
        SummaryValue(R.string.analytics_difference, formatRubles(summary.differenceInKopecks))
    }
}

@Composable
private fun CategoryExpenses(state: CategoryExpensesUiState) {
    Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.analytics_category_expenses), style = MaterialTheme.typography.titleLarge)
        when (state) {
            CategoryExpensesUiState.Loading -> Text(stringResource(R.string.analytics_category_expenses_loading))
            CategoryExpensesUiState.Empty -> Text(stringResource(R.string.analytics_category_expenses_empty))
            CategoryExpensesUiState.Error -> Text(
                stringResource(R.string.analytics_category_expenses_error),
                color = MaterialTheme.colorScheme.error
            )
            is CategoryExpensesUiState.Content -> CategoryExpenseList(state.expenses)
        }
    }
}

@Composable
private fun CategoryExpenseList(expenses: List<CategoryExpense>) {
    val totalInKopecks = expenses.fold(BigDecimal.ZERO) { total, expense ->
        total + BigDecimal.valueOf(expense.amountInKopecks)
    }
    val maxAmountInKopecks = expenses.maxOfOrNull(CategoryExpense::amountInKopecks) ?: 0L
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        expenses.forEach { expense ->
            CategoryExpenseChartRow(expense, totalInKopecks, maxAmountInKopecks)
        }
    }
}

@Composable
private fun CategoryExpenseChartRow(
    expense: CategoryExpense,
    totalInKopecks: BigDecimal,
    maxAmountInKopecks: Long
) {
    val amount = formatRubles(expense.amountInKopecks)
    val percentage = formatPercentage(expense.amountInKopecks, totalInKopecks)
    val description = stringResource(
        R.string.analytics_category_expense_accessibility,
        expense.categoryName,
        amount,
        percentage
    )
    val barFraction = if (maxAmountInKopecks > 0L) {
        (expense.amountInKopecks.toDouble() / maxAmountInKopecks.toDouble())
            .toFloat()
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(expense.categoryName, style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.analytics_category_expense_value, amount, percentage),
            style = MaterialTheme.typography.bodyMedium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clearAndSetSemantics {}
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun SummaryValue(labelRes: Int, value: String) {
    Column {
        Text(stringResource(labelRes), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.analytics_amount, value), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AnalyticsPeriodDialog(
    initialPeriod: AnalyticsPeriod,
    onConfirm: (LocalDate, LocalDate) -> Boolean,
    onDismiss: () -> Unit
) {
    val start = remember { mutableStateOf(initialPeriod.start) }
    val end = remember { mutableStateOf(initialPeriod.end) }
    val editingStart = remember { mutableStateOf<Boolean?>(null) }
    val validationError = remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.analytics_choose_period)) },
        text = {
            Column {
                Text(stringResource(R.string.history_period_start))
                TextButton(onClick = { editingStart.value = true }) { Text(start.value.format(analyticsDateFormatter)) }
                Text(stringResource(R.string.history_period_end))
                TextButton(onClick = { editingStart.value = false }) { Text(end.value.format(analyticsDateFormatter)) }
                if (validationError.value) {
                    Text(stringResource(R.string.analytics_period_invalid_range), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                validationError.value = !onConfirm(start.value, end.value)
                if (!validationError.value) onDismiss()
            }) { Text(stringResource(R.string.history_period_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.transaction_cancel)) } }
    )
    editingStart.value?.let { isStart ->
        val selectedDate = if (isStart) start.value else end.value
        key(isStart, selectedDate) {
            val pickerState = rememberDatePickerState(selectedDate.toUtcMillis())
            DatePickerDialog(
                onDismissRequest = { editingStart.value = null },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.toUtcLocalDate()?.let { date ->
                            if (isStart) start.value = date else end.value = date
                            validationError.value = false
                        }
                        editingStart.value = null
                    }) { Text(stringResource(R.string.transaction_date_confirm)) }
                },
                dismissButton = { TextButton(onClick = { editingStart.value = null }) { Text(stringResource(R.string.transaction_cancel)) } }
            ) { DatePicker(state = pickerState) }
        }
    }
}

private val analyticsDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun formatRubles(kopecks: Long): String {
    val sign = if (kopecks < 0) "-" else ""
    val absolute = BigInteger.valueOf(kopecks).abs()
    val hundred = BigInteger.valueOf(100)
    val rubles = absolute / hundred
    val remainder = (absolute % hundred).toLong()
    return if (remainder == 0L) "$sign$rubles"
    else "%s%s,%02d".format(Locale("ru"), sign, rubles, remainder)
}

private fun formatPercentage(amountInKopecks: Long, totalInKopecks: BigDecimal): String {
    if (totalInKopecks.signum() == 0) return "0,00"
    return BigDecimal.valueOf(amountInKopecks)
        .multiply(BigDecimal.valueOf(100))
        .divide(totalInKopecks, 2, RoundingMode.HALF_UP)
        .toPlainString()
        .replace('.', ',')
}
