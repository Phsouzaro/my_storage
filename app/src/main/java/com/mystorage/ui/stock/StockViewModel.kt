package com.mystorage.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystorage.domain.model.StockSummary
import com.mystorage.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StockViewModel @Inject constructor(
    stockRepository: StockRepository,
) : ViewModel() {

    /** null = carregando. */
    val items: StateFlow<List<StockSummary>?> = stockRepository.observeSummary()
        .map<List<StockSummary>, List<StockSummary>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
