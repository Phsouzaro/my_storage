package com.mystorage.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpiryWorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val DAILY_WORK = "expiry_check_daily"
        private const val NOW_WORK = "expiry_check_now"

        /** Tempo até a próxima ocorrência de [hour]:00 (hoje, se ainda não passou; senão amanhã). */
        fun delayUntil(hour: Int, now: LocalDateTime): Duration {
            var next = now.toLocalDate().atTime(LocalTime.of(hour, 0))
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }

    /**
     * Agenda a verificação diária.
     * @param replace true quando o horário mudou (recria o agendamento); false no start do app (mantém o existente).
     */
    fun scheduleDaily(hour: Int, replace: Boolean = false) {
        val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(hour, LocalDateTime.now()).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Executa já, avisando tudo que está na janela de alerta. */
    fun runNow() {
        val request = OneTimeWorkRequestBuilder<ExpiryCheckWorker>()
            .setInputData(workDataOf(ExpiryCheckWorker.KEY_FORCE to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW_WORK, ExistingWorkPolicy.REPLACE, request)
    }
}
