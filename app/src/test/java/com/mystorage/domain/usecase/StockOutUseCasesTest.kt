package com.mystorage.domain.usecase

import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.LotMovement
import com.mystorage.domain.model.MovementType
import com.mystorage.fakes.FakeStockRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class StockOutUseCasesTest {

    private val base = LocalDate.of(2026, 11, 6)

    private fun lot(id: String, qty: Double, expiresInDays: Long, addedDaysAgo: Long = 10) = Lot(
        id = id, productId = "p", quantity = qty, expiryDate = base.plusDays(expiresInDays),
        expiryMonthOnly = false, addedAt = base.minusDays(addedDaysAgo), location = null,
    )

    private class FakeStock(private val lots: List<Lot>) : FakeStockRepository() {
        val applied = mutableListOf<LotMovement>()
        override suspend fun getLotsOfProduct(productId: String) = lots
        override suspend fun applyMovements(movements: List<LotMovement>) { applied += movements }
    }

    @Test
    fun `FIFO retira primeiro do lote que vence antes`() {
        val plan = FifoPlanner.plan(listOf(lot("B", 2.0, 30), lot("A", 1.0, 5)), 2.0)
        assertEquals(listOf(LotMovement("A", MovementType.CONSUME, 1.0), LotMovement("B", MovementType.CONSUME, 1.0)), plan)
    }

    @Test
    fun `FIFO com mesma validade usa o lote mais antigo`() {
        val plan = FifoPlanner.plan(listOf(lot("novo", 1.0, 5, addedDaysAgo = 1), lot("velho", 1.0, 5, addedDaysAgo = 9)), 1.0)
        assertEquals("velho", plan.single().lotId)
    }

    @Test
    fun `FIFO com quantidade fracionada`() {
        val plan = FifoPlanner.plan(listOf(lot("A", 0.3, 1), lot("B", 1.0, 2)), 0.5)
        assertEquals(0.3, plan[0].quantity, 1e-9)
        assertEquals(0.2, plan[1].quantity, 1e-9)
    }

    @Test
    fun `nao permite consumir mais que o estoque`() {
        assertThrows<IllegalArgumentException> { FifoPlanner.plan(listOf(lot("A", 1.0, 5)), 2.0) }
    }

    @Test
    fun `consumir produto aplica o plano FIFO`() = runTest {
        val repo = FakeStock(listOf(lot("A", 3.0, 5)))
        ConsumeProductUseCase(repo)("p", 2.0)
        assertEquals(listOf(LotMovement("A", MovementType.CONSUME, 2.0)), repo.applied)
    }

    @Test
    fun `descartar lote inteiro`() = runTest {
        val repo = FakeStock(emptyList())
        RemoveFromLotUseCase(repo)(lot("A", 2.0, -1), 2.0, MovementType.DISCARD)
        assertEquals(listOf(LotMovement("A", MovementType.DISCARD, 2.0)), repo.applied)
    }

    @Test
    fun `retirada de lote valida tipo e quantidade`() = runTest {
        val useCase = RemoveFromLotUseCase(FakeStock(emptyList()))
        assertThrows<IllegalArgumentException> { useCase(lot("A", 1.0, 5), 2.0, MovementType.CONSUME) }
        assertThrows<IllegalArgumentException> { useCase(lot("A", 1.0, 5), 1.0, MovementType.IN) }
    }
}
