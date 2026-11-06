package com.mystorage.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystorage.data.local.dao.CategoryDao
import com.mystorage.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StockViewModel @Inject constructor(
    categoryDao: CategoryDao, // TODO: trocar por CategoryRepository (domain) ao criar a camada de repositório
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryDao.observeAll()
        .map { list -> list.map { Category(it.id, it.name, it.icon, it.color, it.sortOrder) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
