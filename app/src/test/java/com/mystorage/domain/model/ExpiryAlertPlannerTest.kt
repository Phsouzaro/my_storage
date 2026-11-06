package com.mystorage.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class ExpiryAlertPlannerTest {

    private val today = LocalDate.of(2026, 11, 6)
    private val yesterday = today.minusDays(1)
    private val thresholds = ExpiryThresholds() // 60 / 7

    private fun lot(daysLeft: Long, addedDaysAgo: Long = 100, id: String = "l$daysLeft") = LotWithProduct(
        lotId = id, productId = "p", productName = "Produto $id", brand = null, imageUrl = null,
        unit = MeasureUnit.UN, quantity = 1.0, expiryDate = today.plusDays(daysLeft),
        expiryMonthOnly = false, addedAt = today.minusDays(addedDaysAgo),
    )

    private fun alerts(vararg lots: LotWithProduct, lastCheck: LocalDate? = yesterday) =
        ExpiryAlertPlanner.newAlerts(lots.toList(), today, lastCheck, thresholds)

    @ParameterizedTest(name = "{0} dias -> {1}")
    @CsvSource("61, ", "60, WARNING", "31, WARNING", "30, THIRTY_DAYS", "8, THIRTY_DAYS", "7, CRITICAL", "0, CRITICAL", "-1, EXPIRED")
    fun `nivel por dias restantes`(days: Long, expected: AlertLevel?) {
        assertEquals(expected, ExpiryAlertPlanner.levelOf(days, thresholds))
    }

    @Test
    fun `marco de 30 dias some quando o prazo de alerta e menor que 30`() {
        val t = ExpiryThresholds(warningDays = 20)
        assertEquals(AlertLevel.WARNING, ExpiryAlertPlanner.levelOf(15, t))
    }

    @Test
    fun `avisa so quem cruzou um marco desde ontem`() {
        val result = alerts(
            lot(60), // ontem 61 -> hoje 60: entrou na janela
            lot(59), // ontem 60 -> nada novo
            lot(30), // ontem 31 -> cruzou 30
            lot(7),  // ontem 8 -> cruzou 7
            lot(5),  // ontem 6 -> nada novo
            lot(-1), // ontem 0 -> venceu
            lot(-2), // já estava vencido
        )
        assertEquals(listOf("l-1", "l7", "l30", "l60"), result.map { it.lot.lotId })
        assertEquals(AlertLevel.EXPIRED, result.first().level)
    }

    @Test
    fun `lote cadastrado hoje nao gera aviso`() {
        assertTrue(alerts(lot(5, addedDaysAgo = 0)).isEmpty())
    }

    @Test
    fun `varios dias sem rodar ainda detecta o cruzamento`() {
        val result = alerts(lot(25), lastCheck = today.minusDays(10)) // há 10 dias: 35 dias -> WARNING
        assertEquals(AlertLevel.THIRTY_DAYS, result.single().level)
    }

    @Test
    fun `primeira execucao avisa tudo que esta na janela`() {
        val result = alerts(lot(100), lot(40), lot(-3), lastCheck = null)
        assertEquals(2, result.size)
    }

    @Test
    fun `forcar avisa tudo que esta na janela`() {
        val result = ExpiryAlertPlanner.newAlerts(
            listOf(lot(59), lot(5), lot(100)), today, yesterday, thresholds, force = true,
        )
        assertEquals(listOf("l5", "l59"), result.map { it.lot.lotId })
    }
}
