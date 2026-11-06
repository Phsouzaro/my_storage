package com.mystorage.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExpiryStatusTest {

    private val today = LocalDate.of(2026, 11, 6)

    @ParameterizedTest(name = "vence em {0} dias -> {1}")
    @CsvSource(
        "-1, EXPIRED",
        "0, CRITICAL",
        "7, CRITICAL",
        "8, WARNING",
        "60, WARNING",
        "61, OK",
        "365, OK",
    )
    fun `status segue os prazos padrao`(daysAhead: Long, expected: ExpiryStatus) {
        assertEquals(expected, expiryStatusOf(today.plusDays(daysAhead), today))
    }

    @Test
    fun `prazo de alerta configuravel`() {
        val thresholds = ExpiryThresholds(warningDays = 30)
        assertEquals(ExpiryStatus.OK, expiryStatusOf(today.plusDays(45), today, thresholds))
        assertEquals(ExpiryStatus.WARNING, expiryStatusOf(today.plusDays(30), today, thresholds))
    }

    @Test
    fun `critico maior que alerta e invalido`() {
        assertThrows<IllegalArgumentException> { ExpiryThresholds(warningDays = 5, criticalDays = 7) }
    }
}
