package com.mystorage.domain.usecase

import com.mystorage.domain.model.Category
import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.Product
import com.mystorage.domain.model.ProductSource
import com.mystorage.fakes.FakeCategoryRepository
import com.mystorage.fakes.FakeProductRepository
import com.mystorage.fakes.FakeStockRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class EditUseCasesTest {

    private val product = Product(
        id = "p1", barcode = "789", name = "Arroz", brand = "Camil", imageUrl = null,
        categoryId = "cat-graos", unit = MeasureUnit.UN, minStock = null, source = ProductSource.MANUAL,
    )

    private val graos = Category("cat-graos", "Grãos e Cereais", "grain", 1, 0)
    private val bebidas = Category("cat-bebidas", "Bebidas", "drink", 2, 1)

    // ---------- Produto ----------

    @Test
    fun `editar produto normaliza textos e preserva o resto`() = runTest {
        val repo = FakeProductRepository(listOf(product))
        UpdateProductUseCase(repo)("p1", "  Arroz Integral ", "  ", "cat-bebidas", MeasureUnit.KG)

        val saved = repo.updated.single()
        assertEquals("Arroz Integral", saved.name)
        assertNull(saved.brand)
        assertEquals("cat-bebidas", saved.categoryId)
        assertEquals(MeasureUnit.KG, saved.unit)
        assertEquals("789", saved.barcode)
    }

    @Test
    fun `editar produto exige nome`() = runTest {
        assertThrows<IllegalArgumentException> {
            UpdateProductUseCase(FakeProductRepository(listOf(product)))("p1", " ", null, "cat-graos", MeasureUnit.UN)
        }
    }

    // ---------- Lote ----------

    private class CapturingStock : FakeStockRepository() {
        var updated: Lot? = null
        override suspend fun updateLot(lot: Lot) { updated = lot }
    }

    private val lot = Lot("l1", "p1", 2.0, LocalDate.of(2027, 2, 3), false, LocalDate.of(2026, 11, 1), null)

    @Test
    fun `editar lote com mes-ano vai para o fim do mes e limpa local vazio`() = runTest {
        val repo = CapturingStock()
        UpdateLotUseCase(repo)(lot.copy(expiryMonthOnly = true, location = "  "))
        assertEquals(LocalDate.of(2027, 2, 28), repo.updated!!.expiryDate)
        assertNull(repo.updated!!.location)
    }

    @Test
    fun `editar lote nao aceita quantidade zero`() = runTest {
        assertThrows<IllegalArgumentException> { UpdateLotUseCase(CapturingStock())(lot.copy(quantity = 0.0)) }
    }

    // ---------- Categoria ----------

    @Test
    fun `nova categoria entra no fim da ordem`() = runTest {
        val repo = FakeCategoryRepository(listOf(graos, bebidas))
        SaveCategoryUseCase(repo)(null, " Pet ", 0xFF00FF00)
        val created = repo.categories.last()
        assertEquals("Pet", created.name)
        assertEquals(2, created.sortOrder)
        assertTrue(created.id.startsWith("cat-"))
    }

    @Test
    fun `nome repetido e rejeitado mesmo com acento diferente`() = runTest {
        val repo = FakeCategoryRepository(listOf(graos, bebidas))
        assertThrows<IllegalArgumentException> { SaveCategoryUseCase(repo)(null, "graos e cereais", 0) }
    }

    @Test
    fun `renomear a propria categoria com o mesmo nome e permitido`() = runTest {
        val repo = FakeCategoryRepository(listOf(graos))
        SaveCategoryUseCase(repo)("cat-graos", "Grãos e Cereais", 99)
        assertEquals(99L, repo.categories.single().color)
    }

    @Test
    fun `nao exclui categoria com produtos`() = runTest {
        val repo = FakeCategoryRepository(listOf(graos), counts = mapOf("cat-graos" to 3))
        val e = assertThrows<CategoryInUseException> { DeleteCategoryUseCase(repo)("cat-graos") }
        assertEquals(3, e.productCount)
        assertTrue(repo.deleted.isEmpty())
    }

    @Test
    fun `exclui categoria vazia`() = runTest {
        val repo = FakeCategoryRepository(listOf(graos, bebidas))
        DeleteCategoryUseCase(repo)("cat-bebidas")
        assertEquals(listOf("cat-bebidas"), repo.deleted)
    }
}
