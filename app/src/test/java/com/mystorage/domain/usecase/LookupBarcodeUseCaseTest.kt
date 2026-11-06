package com.mystorage.domain.usecase

import com.mystorage.domain.model.LookupResult
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.Product
import com.mystorage.domain.model.ProductInfo
import com.mystorage.domain.model.ProductSource
import com.mystorage.domain.repository.ProductInfoSource
import com.mystorage.domain.repository.ProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class LookupBarcodeUseCaseTest {

    private val code = "7894900011517"

    private val localProduct = Product(
        id = "p1", barcode = code, name = "Coca-Cola 2L", brand = "Coca-Cola", imageUrl = null,
        categoryId = "cat-bebidas", unit = MeasureUnit.UN, minStock = null, source = ProductSource.OPEN_FOOD_FACTS,
    )

    private class FakeProducts(private val product: Product?) : ProductRepository {
        override suspend fun findByBarcode(barcode: String) = product?.takeIf { it.barcode == barcode }
    }

    private class FakeSource(
        override val name: String,
        private val result: () -> ProductInfo?,
    ) : ProductInfoSource {
        var calls = 0
        override suspend fun lookup(barcode: String): ProductInfo? {
            calls++
            return result()
        }
    }

    private fun info(source: ProductSource) = ProductInfo(code, "Coca-Cola 2L", "Coca-Cola", null, source)

    @Test
    fun `catalogo local tem prioridade e nao consulta APIs`() = runTest {
        val off = FakeSource("off") { info(ProductSource.OPEN_FOOD_FACTS) }
        val result = LookupBarcodeUseCase(FakeProducts(localProduct), listOf(off))(code)

        assertInstanceOf(LookupResult.Local::class.java, result)
        assertEquals(0, off.calls)
    }

    @Test
    fun `usa a proxima fonte quando a primeira nao encontra`() = runTest {
        val off = FakeSource("off") { null }
        val cosmos = FakeSource("cosmos") { info(ProductSource.COSMOS) }
        val result = LookupBarcodeUseCase(FakeProducts(null), listOf(off, cosmos))(code)

        assertEquals(ProductSource.COSMOS, (result as LookupResult.Remote).info.source)
        assertEquals(1, off.calls)
    }

    @Test
    fun `falha de rede numa fonte nao interrompe a busca`() = runTest {
        val off = FakeSource("off") { throw IOException("sem rede") }
        val cosmos = FakeSource("cosmos") { info(ProductSource.COSMOS) }
        val result = LookupBarcodeUseCase(FakeProducts(null), listOf(off, cosmos))(code)

        assertInstanceOf(LookupResult.Remote::class.java, result)
    }

    @Test
    fun `nao encontrado sinaliza offline quando houve falha de rede`() = runTest {
        val off = FakeSource("off") { throw IOException("sem rede") }
        val cosmos = FakeSource("cosmos") { null }
        val result = LookupBarcodeUseCase(FakeProducts(null), listOf(off, cosmos))(" $code ")

        result as LookupResult.NotFound
        assertEquals(code, result.barcode)
        assertTrue(result.offline)
    }
}
