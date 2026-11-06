package com.mystorage.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ExpiryStatus { OK, WARNING, CRITICAL, EXPIRED }

/** Prazos (em dias) que definem os status. [warningDays] é configurável pelo usuário. */
data class ExpiryThresholds(
    val warningDays: Int = DEFAULT_WARNING_DAYS,
    val criticalDays: Int = DEFAULT_CRITICAL_DAYS,
) {
    init {
        require(criticalDays in 0..warningDays) { "criticalDays deve estar entre 0 e warningDays" }
    }

    companion object {
        const val DEFAULT_WARNING_DAYS = 60
        const val DEFAULT_CRITICAL_DAYS = 7
    }
}

fun daysUntilExpiry(expiryDate: LocalDate, today: LocalDate): Long =
    ChronoUnit.DAYS.between(today, expiryDate)

fun expiryStatusOf(
    expiryDate: LocalDate,
    today: LocalDate,
    thresholds: ExpiryThresholds = ExpiryThresholds(),
): ExpiryStatus {
    val days = daysUntilExpiry(expiryDate, today)
    return when {
        days < 0 -> ExpiryStatus.EXPIRED
        days <= thresholds.criticalDays -> ExpiryStatus.CRITICAL
        days <= thresholds.warningDays -> ExpiryStatus.WARNING
        else -> ExpiryStatus.OK
    }
}

fun Lot.expiryStatus(today: LocalDate, thresholds: ExpiryThresholds = ExpiryThresholds()): ExpiryStatus =
    expiryStatusOf(expiryDate, today, thresholds)
