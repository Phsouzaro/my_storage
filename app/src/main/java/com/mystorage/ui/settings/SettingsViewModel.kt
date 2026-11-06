package com.mystorage.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystorage.data.backup.BackupFileManager
import com.mystorage.data.backup.BackupFormatException
import com.mystorage.domain.model.AppSettings
import com.mystorage.domain.repository.BackupRepository
import com.mystorage.domain.repository.SettingsRepository
import com.mystorage.worker.ExpiryWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scheduler: ExpiryWorkScheduler,
    private val backupRepository: BackupRepository,
    private val backupFiles: BackupFileManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** true enquanto exporta/importa (desabilita os botões). */
    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun setWarningDays(days: Int) = viewModelScope.launch { settingsRepository.setWarningDays(days) }

    fun setNotifyHour(hour: Int) = viewModelScope.launch {
        settingsRepository.setNotifyHour(hour)
        scheduler.scheduleDaily(hour, replace = true)
    }

    fun checkNow() = scheduler.runNow()

    fun suggestedBackupName() = BackupFileManager.suggestedFileName()

    fun exportTo(uri: Uri) = runBackup {
        val (json, counts) = backupRepository.exportJson()
        backupFiles.write(uri, json)
        "Backup salvo: ${counts.products} produtos, ${counts.lots} lotes"
    }

    fun importFrom(uri: Uri) = runBackup {
        val counts = backupRepository.importJson(backupFiles.read(uri))
        "Backup restaurado: ${counts.products} produtos, ${counts.lots} lotes"
    }

    private fun runBackup(action: suspend () -> String) {
        if (_backupBusy.value) return
        _backupBusy.value = true
        viewModelScope.launch {
            val message = try {
                action()
            } catch (e: BackupFormatException) {
                e.message ?: "Arquivo de backup inválido"
            } catch (e: IOException) {
                "Erro ao acessar o arquivo: ${e.message}"
            } catch (e: Exception) {
                "Não foi possível concluir: ${e.message}"
            }
            _backupBusy.value = false
            _messages.send(message)
        }
    }
}
