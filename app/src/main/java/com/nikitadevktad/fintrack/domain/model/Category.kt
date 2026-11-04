package com.nikitadevktad.fintrack.domain.model

data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType
)
