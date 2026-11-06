package com.nikitadevktad.fintrack.presentation.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.nikitadevktad.fintrack.domain.model.TransactionType

@Composable
fun AddTransactionScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    transactionId: Long? = null,
    modifier: Modifier = Modifier
) {
    val container = (LocalContext.current.applicationContext as FinTrackApplication).appContainer
    val viewModel: AddTransactionViewModel = viewModel(factory = viewModelFactory {
        initializer {
            AddTransactionViewModel(
                transactionRepository = container.transactionRepository,
                categoryRepository = container.categoryRepository,
                transactionId = transactionId
            )
        }
    })
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }
    if (state.loadError) {
        LoadErrorContent(onCancel, modifier)
    } else if (state.isLoading) {
        LoadingContent(modifier)
    } else {
        AddTransactionContent(state, viewModel, onCancel, transactionId != null, modifier)
    }
}

@Composable
private fun AddTransactionContent(
    state: AddTransactionState,
    viewModel: AddTransactionViewModel,
    onCancel: () -> Unit,
    isEditing: Boolean,
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(if (isEditing) R.string.edit_transaction_title else R.string.add_transaction_title))
        Text(stringResource(R.string.transaction_type))
        Row {
            TypeOption(state.type, TransactionType.INCOME, R.string.transaction_income, viewModel::selectType)
            TypeOption(state.type, TransactionType.EXPENSE, R.string.transaction_expense, viewModel::selectType)
        }
        OutlinedTextField(
            value = state.amount,
            onValueChange = viewModel::updateAmount,
            label = { Text(stringResource(R.string.transaction_amount)) },
            isError = state.amountError != null,
            supportingText = state.amountError?.let { { Text(stringResource(it.stringRes)) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        CategoryField(state, viewModel)
        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::updateNote,
            label = { Text(stringResource(R.string.transaction_note)) },
            modifier = Modifier.fillMaxWidth()
        )
        state.saveError?.let { Text(stringResource(it.stringRes)) }
        Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            if (state.isSaving) CircularProgressIndicator() else Text(stringResource(R.string.transaction_save))
        }
        TextButton(onClick = onCancel, enabled = !state.isSaving, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.transaction_cancel))
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadErrorContent(onCancel: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.transaction_load_error))
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.transaction_back_to_history))
        }
    }
}

@Composable
private fun TypeOption(selected: TransactionType, type: TransactionType, labelRes: Int, onSelect: (TransactionType) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected == type, onClick = { onSelect(type) })
        TextButton(onClick = { onSelect(type) }) { Text(stringResource(labelRes)) }
    }
}

@Composable
private fun CategoryField(state: AddTransactionState, viewModel: AddTransactionViewModel) {
    val selected = state.visibleCategories.firstOrNull { it.id == state.categoryId }
    Text(stringResource(R.string.transaction_category))
    Button(onClick = viewModel::openCategoryDialog, modifier = Modifier.fillMaxWidth()) {
        Text(selected?.name ?: stringResource(R.string.transaction_category_not_selected))
    }
    if (state.categoryError) Text(stringResource(R.string.transaction_category_required))
    if (state.isCategoryDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::closeCategoryDialog,
            title = { Text(stringResource(R.string.transaction_select_category)) },
            text = {
                Column {
                    state.visibleCategories.forEach { category ->
                        TextButton(onClick = { viewModel.selectCategory(category.id) }) { Text(category.name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::closeCategoryDialog) { Text(stringResource(R.string.transaction_cancel)) }
            }
        )
    }
}

private val AmountError.stringRes: Int
    get() = when (this) {
        AmountError.EMPTY -> R.string.transaction_amount_empty
        AmountError.INVALID -> R.string.transaction_amount_invalid
        AmountError.NOT_POSITIVE -> R.string.transaction_amount_positive
        AmountError.OVERFLOW -> R.string.transaction_amount_overflow
    }

private val SaveError.stringRes: Int
    get() = when (this) {
        SaveError.FAILED -> R.string.transaction_save_error
        SaveError.NOT_FOUND -> R.string.transaction_update_not_found
    }
