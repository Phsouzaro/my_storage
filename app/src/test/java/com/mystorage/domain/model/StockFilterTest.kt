package com.mystorage.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StockFilterTest {

    private val today = LocalDate.of(2026, 11, 6)
    private val thresholds = ExpiryThresholds()

    private fun item(name: String, brand: String? = null, category: String = "cat-graos", daysLeft: Long = 100) = StockSummary(
        productId = name, name = name, brand = brand, imageUrl = null, unit = MeasureUnit.UN,
        categoryId = category, categoryName = category, totalQuantity = 1.0,
        nextExpiry = today.plusDays(daysLeft), lotCount = 1,
    )

    private val items = listOf(
        item("Feijão Carioca", brand = "Camil", daysLeft = 4),
        item("Arroz Branco", brand = "Tio João", daysLeft = 40),
        item("Leite Integral", category = "cat-laticinios", daysLeft = -2),
        item("Coca-Cola 2L", category = "cat-bebidas", daysLeft = 200),
    )

    private fun names(filter: StockFilter) = filter.apply(items, today, thresholds).map { it.name }

    @Test
    fun `sem filtro retorna tudo`() {
        assertEquals(4, names(StockFilter()).size)
        assertFalse(StockFilter().isActive)
    }

    @Test
    fun `busca ignora acento e maiusculas`() {
        assertEquals(listOf("Feijão Carioca"), names(StockFilter(query = "FEIJAO")))
    }

    @Test
    fun `busca tambem olha a marca e exige todos os termos`() {
        assertEquals(listOf("Arroz Branco"), names(StockFilter(query = "joao arroz")))
        assertTrue(names(StockFilter(query = "arroz camil")).isEmpty())
    }

    @Test
    fun `filtra por categoria`() {
        assertEquals(listOf("Leite Integral"), names(StockFilter(categoryId = "cat-laticinios")))
    }

    @Test
    fun `filtra por status`() {
        assertEquals(listOf("Leite Integral"), names(StockFilter(status = StatusFilter.EXPIRED)))
        assertEquals(listOf("Feijão Carioca"), names(StockFilter(status = StatusFilter.CRITICAL)))
        assertEquals(listOf("Arroz Branco"), names(StockFilter(status = StatusFilter.WARNING)))
    }

    @Test
    fun `filtros se combinam`() {
        val f = StockFilter(query = "a", categoryId = "cat-graos", status = StatusFilter.CRITICAL)
        assertEquals(listOf("Feijão Carioca"), names(f))
        assertTrue(f.isActive)
    }
}
