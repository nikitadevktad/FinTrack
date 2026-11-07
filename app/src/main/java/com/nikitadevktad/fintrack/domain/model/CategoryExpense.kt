package com.nikitadevktad.fintrack.domain.model

data class CategoryExpense(
    val categoryId: Long,
    val categoryName: String,
    val amountInKopecks: Long
)
