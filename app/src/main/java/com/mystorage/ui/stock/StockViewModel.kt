package com.mystorage.ui.stock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystorage.domain.model.Category
import com.mystorage.domain.model.StatusFilter
import com.mystorage.domain.model.StockFilter
import com.mystorage.domain.model.StockSummary
import com.mystorage.domain.repository.CategoryRepository
import com.mystorage.domain.repository.SettingsRepository
import com.mystorage.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class StockUiState(
    val isLoading: Boolean = true,
    /** Itens após o filtro. */
    val items: List<StockSummary> = emptyList(),
    /** Quantos produtos há no estoque sem filtro (para diferenciar "vazio" de "nada encontrado"). */
    val totalCount: Int = 0,
    val filter: StockFilter = StockFilter(),
    /** Só as categorias que têm produto em estoque. */
    val categories: List<Category> = emptyList(),
    val warningDays: Int = 60,
)

@HiltViewModel
class StockViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    stockRepository: StockRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    // Filtro guardado no SavedStateHandle: sobrevive à rotação e à volta da tela de produto.
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val categoryId = savedStateHandle.getStateFlow<String?>(KEY_CATEGORY, null)
    private val status = savedStateHandle.getStateFlow(KEY_STATUS, StatusFilter.ALL.name)

    private val filter = combine(query, categoryId, status) { q, c, s -> StockFilter(q, c, StatusFilter.valueOf(s)) }

    val state: StateFlow<StockUiState> = combine(
        stockRepository.observeSummary(),
        categoryRepository.observeAll(),
        settingsRepository.observe(),
        filter,
    ) { items, categories, settings, f ->
        val usedCategoryIds = items.mapTo(HashSet()) { it.categoryId }
        StockUiState(
            isLoading = false,
            items = f.apply(items, LocalDate.now(), settings.thresholds),
            totalCount = items.size,
            filter = f,
            categories = categories.filter { it.id in usedCategoryIds },
            warningDays = settings.warningDays,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockUiState())

    fun onQueryChange(value: String) { savedStateHandle[KEY_QUERY] = value }
    fun onCategoryChange(id: String?) { savedStateHandle[KEY_CATEGORY] = id }
    fun onStatusChange(value: StatusFilter) { savedStateHandle[KEY_STATUS] = value.name }

    fun clearFilters() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_CATEGORY] = null
        savedStateHandle[KEY_STATUS] = StatusFilter.ALL.name
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_CATEGORY = "categoryId"
        const val KEY_STATUS = "status"
    }
}
