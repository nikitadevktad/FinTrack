package com.nikitadevktad.fintrack.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nikitadevktad.fintrack.FinTrackApplication
import com.nikitadevktad.fintrack.R
import com.nikitadevktad.fintrack.domain.model.Category
import com.nikitadevktad.fintrack.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val appContainer = (LocalContext.current.applicationContext as FinTrackApplication).appContainer
    val viewModel: HistoryViewModel = viewModel(factory = viewModelFactory {
        initializer {
            HistoryViewModel(
                appContainer.transactionRepository,
                appContainer.categoryRepository
            )
        }
    })
    val items by viewModel.items.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val categories by viewModel.availableCategories.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Button(
            onClick = onAddTransaction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.history_add_transaction))
        }
        HistoryFiltersSelector(filters = filters, categories = categories, viewModel = viewModel)
        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(
                        if (filters == HistoryFilters()) R.string.history_empty_state
                        else R.string.history_filtered_empty_state
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = HistoryItem::id) { item ->
                    Column(modifier = Modifier.clickable { onEditTransaction(item.id) }) {
                        Text(item.categoryName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            item.date.format(
                                DateTimeFormatter.ofPattern(
                                    "dd.MM.yyyy",
                                    Locale("ru")
                                )
                            )
                        )
                        Text(
                            stringResource(
                                if (item.type == TransactionType.INCOME) R.string.transaction_income_amount
                                else R.string.transaction_expense_amount,
                                formatRubles(item.amountInKopecks)
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFiltersSelector(
    filters: HistoryFilters,
    categories: List<Category>,
    viewModel: HistoryViewModel
) {
    val isCustomPeriodDialogOpen = remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.history_period_label),
            style = MaterialTheme.typography.titleMedium
        )
        FilterOption(
            selected = filters.period is HistoryPeriod.All,
            label = stringResource(R.string.history_all_operations),
            onClick = viewModel::selectAllOperations
        )
        FilterOption(
            selected = filters.period is HistoryPeriod.CurrentMonth,
            label = stringResource(R.string.history_current_month),
            onClick = viewModel::selectCurrentMonth
        )
        FilterOption(
            selected = filters.period is HistoryPeriod.Custom,
            label = stringResource(R.string.history_choose_period),
            onClick = { isCustomPeriodDialogOpen.value = true }
        )
        Text(
            stringResource(R.string.history_active_period, periodLabel(filters.period)),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(stringResource(R.string.history_type_label), style = MaterialTheme.typography.titleMedium)
        FilterOption(
            selected = filters.type == null,
            label = stringResource(R.string.history_all_types),
            onClick = { viewModel.selectType(null) }
        )
        FilterOption(
            selected = filters.type == TransactionType.INCOME,
            label = stringResource(R.string.history_incomes),
            onClick = { viewModel.selectType(TransactionType.INCOME) }
        )
        FilterOption(
            selected = filters.type == TransactionType.EXPENSE,
            label = stringResource(R.string.history_expenses),
            onClick = { viewModel.selectType(TransactionType.EXPENSE) }
        )
        Text(stringResource(R.string.history_category_label), style = MaterialTheme.typography.titleMedium)
        FilterOption(
            selected = filters.categoryId == null,
            label = stringResource(R.string.history_all_categories),
            onClick = { viewModel.selectCategory(null) }
        )
        categories
            .filter { filters.type == null || it.type == filters.type }
            .forEach { category ->
                FilterOption(
                    selected = filters.categoryId == category.id,
                    label = category.name,
                    onClick = { viewModel.selectCategory(category.id) }
                )
            }
        if (filters != HistoryFilters()) {
            TextButton(onClick = viewModel::resetFilters) {
                Text(stringResource(R.string.history_reset_filters))
            }
        }
    }
    if (isCustomPeriodDialogOpen.value) {
        val customPeriod = filters.period as? HistoryPeriod.Custom
        CustomPeriodDialog(
            initialStart = customPeriod?.start ?: LocalDate.now(),
            initialEnd = customPeriod?.end ?: LocalDate.now(),
            onConfirm = { start, end ->
                viewModel.selectCustomPeriod(start, end)
                isCustomPeriodDialogOpen.value = false
            },
            onDismiss = { isCustomPeriodDialogOpen.value = false }
        )
    }
}

@Composable
private fun FilterOption(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        TextButton(onClick = onClick) { Text(label) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomPeriodDialog(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val start = remember { mutableStateOf(initialStart) }
    val end = remember { mutableStateOf(initialEnd) }
    val editingStart = remember { mutableStateOf<Boolean?>(null) }
    val validationError = remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_choose_period)) },
        text = {
            Column {
                Text(stringResource(R.string.history_period_start))
                TextButton(onClick = { editingStart.value = true }) {
                    Text(start.value.format(historyDateFormatter))
                }
                Text(stringResource(R.string.history_period_end))
                TextButton(onClick = { editingStart.value = false }) {
                    Text(end.value.format(historyDateFormatter))
                }
                if (validationError.value) {
                    Text(
                        stringResource(R.string.history_period_invalid_range),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (start.value.isAfter(end.value)) {
                        validationError.value = true
                    } else {
                        onConfirm(start.value, end.value)
                    }
                }
            ) {
                Text(stringResource(R.string.history_period_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.transaction_cancel)) }
        }
    )
    editingStart.value?.let { isStart ->
        val selectedDate = if (isStart) start.value else end.value
        key(isStart, selectedDate) {
            val pickerState =
                rememberDatePickerState(initialSelectedDateMillis = selectedDate.toUtcMillis())
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
                dismissButton = {
                    TextButton(onClick = { editingStart.value = null }) {
                        Text(stringResource(R.string.transaction_cancel))
                    }
                }
            ) {
                DatePicker(state = pickerState)
            }
        }
    }
}

@Composable
private fun periodLabel(period: HistoryPeriod): String = when (period) {
    HistoryPeriod.All -> stringResource(R.string.history_all_operations)
    is HistoryPeriod.CurrentMonth -> stringResource(
        R.string.history_current_month_dates,
        period.start.format(historyDateFormatter),
        period.end.format(historyDateFormatter)
    )

    is HistoryPeriod.Custom -> stringResource(
        R.string.history_custom_period_dates,
        period.start.format(historyDateFormatter),
        period.end.format(historyDateFormatter)
    )
}

private val historyDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun formatRubles(kopecks: Long): String = "%d,%02d".format(
    Locale("ru"), kopecks / 100, kopecks % 100
)
