package com.nikitadevktad.fintrack.presentation.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nikitadevktad.fintrack.R

@Composable
fun OverviewScreen(modifier: Modifier = Modifier) {
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
