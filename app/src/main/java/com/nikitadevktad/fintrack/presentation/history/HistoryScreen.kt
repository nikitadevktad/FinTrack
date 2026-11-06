package com.nikitadevktad.fintrack.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        initializer { HistoryViewModel(appContainer.transactionRepository, appContainer.categoryRepository) }
    })
    val items by viewModel.items.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Button(
            onClick = onAddTransaction,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(stringResource(R.string.history_add_transaction))
        }
        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.history_empty_state), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = HistoryItem::id) { item ->
                    Column(modifier = Modifier.clickable { onEditTransaction(item.id) }) {
                        Text(item.categoryName, style = MaterialTheme.typography.titleMedium)
                        Text(item.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))))
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

private fun formatRubles(kopecks: Long): String = "%d,%02d".format(
    Locale("ru"), kopecks / 100, kopecks % 100
)
