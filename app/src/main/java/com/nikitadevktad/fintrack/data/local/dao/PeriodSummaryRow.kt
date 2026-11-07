package com.nikitadevktad.fintrack.data.local.dao

data class PeriodSummaryRow(
    val incomeInKopecks: Long,
    val expenseInKopecks: Long,
    val operationCount: Long
)
