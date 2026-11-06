package com.mystorage.domain.model

import java.time.LocalDate

/** Lote ativo com os dados do produto — usado pela Home e pelo worker de alertas. */
data class LotWithProduct(
    val lotId: String,
    val productId: String,
    val productName: String,
    val brand: String?,
    val imageUrl: String?,
    val unit: MeasureUnit,
    val quantity: Double,
    val expiryDate: LocalDate,
    val expiryMonthOnly: Boolean,
    val addedAt: LocalDate,
)

/** Marcos de alerta, do mais distante ao mais urgente (a ordem importa). */
enum class AlertLevel { WARNING, THIRTY_DAYS, CRITICAL, EXPIRED }

data class ExpiryAlert(val lot: LotWithProduct, val level: AlertLevel)

/**
 * Decide quais lotes devem gerar notificação.
 *
 * Um lote notifica quando **cruza um marco** (60 → 30 → 7 dias → vencido) entre a última
 * verificação e hoje — assim o mesmo aviso não se repete todo dia. Não precisa guardar estado
 * por lote: basta comparar o marco de hoje com o marco na data de referência, que é a última
 * verificação ou a data de entrada do lote (o que for mais recente — quem acabou de cadastrar
 * um item já sabe da validade dele).
 */
object ExpiryAlertPlanner {

    private const val THIRTY_DAYS = 30

    fun levelOf(daysLeft: Long, thresholds: ExpiryThresholds): AlertLevel? = when {
        daysLeft < 0 -> AlertLevel.EXPIRED
        daysLeft <= thresholds.criticalDays -> AlertLevel.CRITICAL
        daysLeft <= THIRTY_DAYS && THIRTY_DAYS < thresholds.warningDays -> AlertLevel.THIRTY_DAYS
        daysLeft <= thresholds.warningDays -> AlertLevel.WARNING
        else -> null
    }

    /**
     * @param lastCheck data da última verificação; null = primeira vez (avisa tudo que está na janela).
     * @param force true = avisa tudo que está na janela (botão "Verificar agora").
     */
    fun newAlerts(
        lots: List<LotWithProduct>,
        today: LocalDate,
        lastCheck: LocalDate?,
        thresholds: ExpiryThresholds,
        force: Boolean = false,
    ): List<ExpiryAlert> = lots.mapNotNull { lot ->
        val levelNow = levelOf(daysUntilExpiry(lot.expiryDate, today), thresholds) ?: return@mapNotNull null
        if (force || lastCheck == null) return@mapNotNull ExpiryAlert(lot, levelNow)

        val reference = maxOf(lastCheck, lot.addedAt)
        val levelBefore = levelOf(daysUntilExpiry(lot.expiryDate, reference), thresholds)
        if (levelBefore == null || levelNow > levelBefore) ExpiryAlert(lot, levelNow) else null
    }.sortedWith(compareByDescending<ExpiryAlert> { it.level }.thenBy { it.lot.expiryDate })
}
