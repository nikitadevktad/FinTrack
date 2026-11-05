package com.nikitadevktad.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class TransactionEntity(
    @PrimaryKey val id: Long,
    val amountInKopecks: Long,
    val categoryId: Long,
    val dateEpochDay: Long,
    val note: String?
)
