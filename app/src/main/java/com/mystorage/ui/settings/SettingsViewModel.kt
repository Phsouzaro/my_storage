package com.mystorage.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystorage.domain.model.AppSettings
import com.mystorage.domain.repository.SettingsRepository
import com.mystorage.worker.ExpiryWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scheduler: ExpiryWorkScheduler,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWarningDays(days: Int) = viewModelScope.launch { settingsRepository.setWarningDays(days) }

    fun setNotifyHour(hour: Int) = viewModelScope.launch {
        settingsRepository.setNotifyHour(hour)
        scheduler.scheduleDaily(hour, replace = true)
    }

    fun checkNow() = scheduler.runNow()
}
