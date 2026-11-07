package com.nikitadevktad.fintrack.domain.model

data class PeriodSummary(
    val incomeInKopecks: Long,
    val expenseInKopecks: Long,
    val operationCount: Long
) {
    val differenceInKopecks: Long
        get() = incomeInKopecks - expenseInKopecks
}
