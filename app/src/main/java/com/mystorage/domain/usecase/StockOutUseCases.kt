package com.mystorage.domain.usecase

import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.LotMovement
import com.mystorage.domain.model.MovementType
import com.mystorage.domain.repository.StockRepository
import javax.inject.Inject

/** Tolerância para comparar quantidades fracionadas (kg, L). */
internal const val QTY_EPSILON = 1e-9

/**
 * Divide uma retirada entre os lotes, do que vence primeiro para o último (FIFO por validade).
 * Ex.: lotes [A=1 (vence antes), B=2], retirar 2 → A:1, B:1.
 */
object FifoPlanner {
    fun plan(lots: List<Lot>, quantity: Double): List<LotMovement> {
        require(quantity > 0) { "Quantidade deve ser maior que zero" }
        val available = lots.filter { it.quantity > QTY_EPSILON }.sortedWith(compareBy({ it.expiryDate }, { it.addedAt }))
        require(quantity <= available.sumOf { it.quantity } + QTY_EPSILON) { "Quantidade maior que o estoque" }

        var remaining = quantity
        val result = mutableListOf<LotMovement>()
        for (lot in available) {
            if (remaining <= QTY_EPSILON) break
            val take = minOf(lot.quantity, remaining)
            result += LotMovement(lot.id, MovementType.CONSUME, take)
            remaining -= take
        }
        return result
    }
}

/** "Consumir" no nível do produto: retira dos lotes que vencem primeiro. */
class ConsumeProductUseCase @Inject constructor(
    private val stockRepository: StockRepository,
) {
    suspend operator fun invoke(productId: String, quantity: Double) {
        val lots = stockRepository.getLotsOfProduct(productId)
        stockRepository.applyMovements(FifoPlanner.plan(lots, quantity))
    }
}

/** Retirada de um lote específico (consumo ou descarte). */
class RemoveFromLotUseCase @Inject constructor(
    private val stockRepository: StockRepository,
) {
    suspend operator fun invoke(lot: Lot, quantity: Double, type: MovementType) {
        require(type == MovementType.CONSUME || type == MovementType.DISCARD) { "Tipo de saída inválido" }
        require(quantity > 0) { "Quantidade deve ser maior que zero" }
        require(quantity <= lot.quantity + QTY_EPSILON) { "Quantidade maior que o lote" }
        stockRepository.applyMovements(listOf(LotMovement(lot.id, type, minOf(quantity, lot.quantity))))
    }
}
