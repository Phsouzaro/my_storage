package com.mystorage.data.backup

import com.mystorage.data.local.entity.CategoryEntity
import com.mystorage.data.local.entity.LotEntity
import com.mystorage.data.local.entity.MovementEntity
import com.mystorage.data.local.entity.ProductEntity
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.MovementType
import com.mystorage.domain.model.ProductSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class BackupSerializerTest {

    private val snapshot = BackupSnapshot(
        categories = listOf(CategoryEntity("cat-bebidas", "Bebidas", "drink", 0xFF1E88E5, 6, 1L)),
        products = listOf(
            ProductEntity(
                "p1", "7894900011517", "Coca-Cola 2L", "Coca-Cola", "https://x/y.jpg", "cat-bebidas",
                MeasureUnit.UN, null, ProductSource.OPEN_FOOD_FACTS, 2L,
            ),
            ProductEntity("p2", null, "Arroz", null, null, "cat-bebidas", MeasureUnit.KG, 2.0, ProductSource.MANUAL, 3L, deletedAt = 9L),
        ),
        lots = listOf(
            LotEntity("l1", "p1", 1.5, LocalDate.of(2026, 11, 30), true, LocalDate.of(2026, 11, 6), "Despensa", 4L),
        ),
        movements = listOf(MovementEntity("m1", "l1", MovementType.IN, 1.5, 5L)),
    )

    @Test
    fun `ida e volta preserva todos os dados`() {
        val json = BackupSerializer.toJson(snapshot, Instant.parse("2026-11-06T10:00:00Z"))
        assertEquals(snapshot, BackupSerializer.fromJson(json))
    }

    @Test
    fun `json tem cabecalho legivel com datas ISO`() {
        val json = BackupSerializer.toJson(snapshot, Instant.parse("2026-11-06T10:00:00Z"))
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"exportedAt\": \"2026-11-06T10:00:00Z\""))
        assertTrue(json.contains("\"expiryDate\": \"2026-11-30\""))
    }

    @Test
    fun `arquivo que nao e json e rejeitado`() {
        val e = assertThrows<BackupFormatException> { BackupSerializer.fromJson("isso não é json") }
        assertTrue(e.message!!.contains("não é um backup"))
    }

    @Test
    fun `json de outro app e rejeitado`() {
        assertThrows<BackupFormatException> { BackupSerializer.fromJson("""{"app":"outro","schemaVersion":1,"exportedAt":"x"}""") }
    }

    @Test
    fun `backup de versao mais nova e rejeitado`() {
        val e = assertThrows<BackupFormatException> {
            BackupSerializer.fromJson("""{"app":"my_storage","schemaVersion":99,"exportedAt":"x"}""")
        }
        assertTrue(e.message!!.contains("versão mais nova"))
    }

    @Test
    fun `referencia quebrada e rejeitada antes de importar`() {
        val broken = snapshot.copy(lots = snapshot.lots + snapshot.lots.first().copy(id = "l2", productId = "nao-existe"))
        assertThrows<BackupFormatException> { BackupSerializer.fromJson(BackupSerializer.toJson(broken)) }
    }

    @Test
    fun `data invalida e rejeitada`() {
        val json = BackupSerializer.toJson(snapshot).replace("2026-11-30", "30/11/2026")
        assertThrows<BackupFormatException> { BackupSerializer.fromJson(json) }
    }
}
