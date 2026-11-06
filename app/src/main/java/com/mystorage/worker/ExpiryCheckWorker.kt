package com.mystorage.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mystorage.domain.model.ExpiryAlertPlanner
import com.mystorage.domain.repository.SettingsRepository
import com.mystorage.domain.repository.StockRepository
import com.mystorage.notification.ExpiryNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Verificação diária (ARCHITECTURE.md §7): avisa os lotes que cruzaram um marco
 * (60 → 30 → 7 dias → vencido) desde a última execução, numa notificação agrupada.
 */
@HiltWorker
class ExpiryCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stockRepository: StockRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: ExpiryNotifier,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_FORCE = "force"
        const val KEY_ALERT_COUNT = "alert_count"
    }

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val force = inputData.getBoolean(KEY_FORCE, false)
        val settings = settingsRepository.get()

        val alerts = ExpiryAlertPlanner.newAlerts(
            lots = stockRepository.getActiveLots(),
            today = today,
            lastCheck = settingsRepository.getLastExpiryCheck(),
            thresholds = settings.thresholds,
            force = force,
        )
        notifier.notify(alerts, today)

        // Só avança a data quando havia permissão — senão os avisos se perderiam
        // e seriam reavaliados (com o marco já cruzado) assim que a permissão for dada.
        if (notifier.canNotify()) settingsRepository.setLastExpiryCheck(today)

        return Result.success(workDataOf(KEY_ALERT_COUNT to alerts.size))
    }
}
