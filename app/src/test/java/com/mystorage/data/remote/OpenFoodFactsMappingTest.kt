package com.mystorage.data.remote

import com.mystorage.domain.model.ProductSource
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OpenFoodFactsMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `resposta real do OFF e convertida`() {
        // Resposta real de /api/v2/product/7894900011517 (campos filtrados)
        val body = """
            {"code":"7894900011517","product":{"brands":"Coca-Cola","code":"7894900011517",
             "image_front_url":"https://images.openfoodfacts.org/x.jpg",
             "product_name":"Refrigerante Coca-Cola 2Lt","product_name_pt":"Refrigerante Coca-Cola 2Lt",
             "quantity":"2l"},"status":1,"status_verbose":"product found"}
        """.trimIndent()

        val info = json.decodeFromString<OffResponse>(body).product!!.toProductInfo("7894900011517")!!

        // "2l" já está contido em "2Lt" → não duplica
        assertEquals("Refrigerante Coca-Cola 2Lt", info.name)
        assertEquals("Coca-Cola", info.brand)
        assertEquals(ProductSource.OPEN_FOOD_FACTS, info.source)
    }

    @Test
    fun `prefere nome em portugues, adiciona quantidade e pega a primeira marca`() {
        val product = OffProduct(productName = "Rice", productNamePt = "Arroz Branco", brands = "Tio João, Camil", quantity = "5 kg")
        val info = product.toProductInfo("123")!!
        assertEquals("Arroz Branco 5 kg", info.name)
        assertEquals("Tio João", info.brand)
    }

    @Test
    fun `sem nome utilizavel retorna null`() {
        assertNull(OffProduct(productName = " ", productNamePt = "").toProductInfo("123"))
    }

    @Test
    fun `produto nao encontrado decodifica com status 0`() {
        val res = json.decodeFromString<OffResponse>("""{"code":"1","status":0,"status_verbose":"product not found"}""")
        assertEquals(0, res.status)
        assertNull(res.product)
    }
}
