package com.mystorage.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystorage.domain.model.ExpiryStatus
import com.mystorage.domain.model.LotWithProduct
import com.mystorage.domain.model.expiryStatusOf
import com.mystorage.domain.repository.SettingsRepository
import com.mystorage.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val warningDays: Int = 60,
    val totalLots: Int = 0,
    val expired: List<LotWithProduct> = emptyList(),
    val critical: List<LotWithProduct> = emptyList(),
    val warning: List<LotWithProduct> = emptyList(),
) {
    val hasAlerts: Boolean get() = expired.isNotEmpty() || critical.isNotEmpty() || warning.isNotEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    stockRepository: StockRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        stockRepository.observeActiveLots(),
        settingsRepository.observe(),
    ) { lots, settings ->
        val today = LocalDate.now()
        val byStatus = lots.groupBy { expiryStatusOf(it.expiryDate, today, settings.thresholds) }
        HomeUiState(
            isLoading = false,
            warningDays = settings.warningDays,
            totalLots = lots.size,
            expired = byStatus[ExpiryStatus.EXPIRED].orEmpty(),
            critical = byStatus[ExpiryStatus.CRITICAL].orEmpty(),
            warning = byStatus[ExpiryStatus.WARNING].orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
