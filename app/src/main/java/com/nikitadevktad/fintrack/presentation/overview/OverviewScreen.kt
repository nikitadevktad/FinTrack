package com.nikitadevktad.fintrack.presentation.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
fun OverviewScreen(modifier: Modifier = Modifier) {
    val appContainer = (LocalContext.current.applicationContext as FinTrackApplication).appContainer
    val viewModel: OverviewViewModel = viewModel(factory = viewModelFactory {
        initializer {
            OverviewViewModel(
                transactionRepository = appContainer.transactionRepository,
                categoryRepository = appContainer.categoryRepository
            )
        }
    })
    val items by viewModel.items.collectAsStateWithLifecycle()

    if (items.isNotEmpty()) {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(items, key = OverviewItem::id) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(item.categoryName, style = MaterialTheme.typography.titleMedium)
                    Text(item.date.format(overviewDateFormatter))
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
    } else {
        OverviewEmptyState(modifier)
    }
}

@Composable
private fun OverviewEmptyState(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.finlogo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .width(240.dp)
                .padding(bottom = 24.dp)
        )
        Text(
            text = stringResource(R.string.overview_empty_state),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private val overviewDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))

private fun formatRubles(kopecks: Long): String = "%d,%02d".format(
    Locale("ru"), kopecks / 100, kopecks % 100
)
