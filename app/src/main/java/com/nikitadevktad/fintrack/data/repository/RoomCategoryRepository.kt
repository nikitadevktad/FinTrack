package com.nikitadevktad.fintrack.data.repository

import com.nikitadevktad.fintrack.data.local.dao.CategoryDao
import com.nikitadevktad.fintrack.data.local.entity.CategoryEntity
import com.nikitadevktad.fintrack.domain.model.Category
import com.nikitadevktad.fintrack.domain.model.TransactionType
import com.nikitadevktad.fintrack.domain.repository.CategoryRepository

class RoomCategoryRepository(
    private val categoryDao: CategoryDao,
    private val awaitInitialization: suspend () -> Unit
) : CategoryRepository {
    override suspend fun getAll(): List<Category> {
        awaitInitialization()
        return categoryDao.getAll().map(CategoryEntity::toDomain)
    }
}

internal fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = TransactionType.valueOf(type)
)
