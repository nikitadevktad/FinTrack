package com.nikitadevktad.fintrack.domain.model

import java.time.LocalDate

data class Transaction(
    val id: Long,
    val amountInKopecks: Long,
    val categoryId: Long,
    val date: LocalDate,
    val note: String?
) {
    init {
        require(amountInKopecks > 0)
    }
}
