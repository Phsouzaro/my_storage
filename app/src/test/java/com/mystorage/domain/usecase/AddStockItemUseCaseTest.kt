package com.mystorage.domain.usecase

import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.NewProduct
import com.mystorage.domain.model.NewStockItem
import com.mystorage.domain.model.ProductSource
import com.mystorage.fakes.FakeStockRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AddStockItemUseCaseTest {

    private class CapturingRepo : FakeStockRepository() {
        var saved: NewStockItem? = null
        override suspend fun addStockItem(item: NewStockItem): String {
            saved = item
            return "lot-1"
        }
    }

    private val repo = CapturingRepo()
    private val useCase = AddStockItemUseCase(repo)

    private fun newItem(
        quantity: Double = 2.0,
        expiry: LocalDate = LocalDate.of(2027, 3, 10),
        monthOnly: Boolean = false,
        name: String = "  Arroz  ",
        brand: String? = "  ",
        location: String? = " ",
    ) = NewStockItem(
        productId = null,
        newProduct = NewProduct("789", name, brand, null, "cat-graos", MeasureUnit.UN, ProductSource.MANUAL),
        quantity = quantity,
        expiryDate = expiry,
        expiryMonthOnly = monthOnly,
        location = location,
    )

    @Test
    fun `validade so mes-ano vira o ultimo dia do mes`() = runTest {
        useCase(newItem(expiry = LocalDate.of(2027, 2, 3), monthOnly = true))
        assertEquals(LocalDate.of(2027, 2, 28), repo.saved!!.expiryDate)
    }

    @Test
    fun `normaliza textos em branco`() = runTest {
        useCase(newItem())
        val saved = repo.saved!!
        assertEquals("Arroz", saved.newProduct!!.name)
        assertNull(saved.newProduct.brand)
        assertNull(saved.location)
    }

    @Test
    fun `quantidade zero e rejeitada`() = runTest {
        assertThrows<IllegalArgumentException> { useCase(newItem(quantity = 0.0)) }
    }

    @Test
    fun `nome em branco e rejeitado`() = runTest {
        assertThrows<IllegalArgumentException> { useCase(newItem(name = "   ")) }
    }
}
