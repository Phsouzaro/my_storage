package com.mystorage.domain.model

data class AppSettings(
    val warningDays: Int = ExpiryThresholds.DEFAULT_WARNING_DAYS,
    /** Hora do dia (0–23) da verificação diária. */
    val notifyHour: Int = DEFAULT_NOTIFY_HOUR,
) {
    val thresholds: ExpiryThresholds get() = ExpiryThresholds(warningDays = warningDays)

    companion object {
        const val DEFAULT_NOTIFY_HOUR = 9
        val WARNING_DAY_OPTIONS = listOf(30, 45, 60, 90)
    }
}
