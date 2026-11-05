package com.nikitadevktad.fintrack.domain.repository

import com.nikitadevktad.fintrack.domain.model.Category

interface CategoryRepository {
    suspend fun getAll(): List<Category>
}
