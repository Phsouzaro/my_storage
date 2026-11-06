package com.mystorage.data.backup

import com.mystorage.data.local.entity.CategoryEntity
import com.mystorage.data.local.entity.LotEntity
import com.mystorage.data.local.entity.MovementEntity
import com.mystorage.data.local.entity.ProductEntity
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.MovementType
import com.mystorage.domain.model.ProductSource
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Conteúdo completo do banco (inclui registros com soft delete, para não perder histórico). */
data class BackupSnapshot(
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>,
    val lots: List<LotEntity>,
    val movements: List<MovementEntity>,
)

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Formato do arquivo de backup (ARCHITECTURE.md §8). Datas em ISO-8601, enums pelo nome.
 * [schemaVersion] permite ler backups antigos depois de migrações do banco.
 */
object BackupSerializer {

    const val SCHEMA_VERSION = 1
    private const val APP_ID = "my_storage"

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun toJson(snapshot: BackupSnapshot, exportedAt: Instant = Instant.now()): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            app = APP_ID,
            schemaVersion = SCHEMA_VERSION,
            exportedAt = exportedAt.toString(),
            categories = snapshot.categories.map { it.toDto() },
            products = snapshot.products.map { it.toDto() },
            lots = snapshot.lots.map { it.toDto() },
            movements = snapshot.movements.map { it.toDto() },
        ),
    )

    /** Lê e valida tudo antes de devolver — a importação só apaga os dados atuais se isto passar. */
    fun fromJson(text: String): BackupSnapshot {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupFormatException("Arquivo não é um backup válido do Meu Estoque", e)
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("Arquivo não é um backup válido do Meu Estoque", e)
        }
        if (file.app != APP_ID) throw BackupFormatException("Arquivo não é um backup do Meu Estoque")
        if (file.schemaVersion > SCHEMA_VERSION) {
            throw BackupFormatException("Backup feito numa versão mais nova do app. Atualize o app para importar.")
        }

        val snapshot = try {
            BackupSnapshot(
                categories = file.categories.map { it.toEntity() },
                products = file.products.map { it.toEntity() },
                lots = file.lots.map { it.toEntity() },
                movements = file.movements.map { it.toEntity() },
            )
        } catch (e: DateTimeParseException) {
            throw BackupFormatException("Data inválida no backup", e)
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("Valor inválido no backup", e)
        }
        validateReferences(snapshot)
        return snapshot
    }

    private fun validateReferences(s: BackupSnapshot) {
        val categoryIds = s.categories.mapTo(HashSet()) { it.id }
        val productIds = s.products.mapTo(HashSet()) { it.id }
        val lotIds = s.lots.mapTo(HashSet()) { it.id }
        s.products.firstOrNull { it.categoryId !in categoryIds }?.let {
            throw BackupFormatException("Backup inconsistente: produto \"${it.name}\" sem categoria")
        }
        s.lots.firstOrNull { it.productId !in productIds }?.let {
            throw BackupFormatException("Backup inconsistente: lote sem produto")
        }
        s.movements.firstOrNull { it.lotId !in lotIds }?.let {
            throw BackupFormatException("Backup inconsistente: movimentação sem lote")
        }
        val barcodes = s.products.mapNotNull { it.barcode }
        if (barcodes.size != barcodes.toSet().size) throw BackupFormatException("Backup inconsistente: código de barras repetido")
    }
}

// ---------- DTOs (formato do arquivo) ----------

@Serializable
private data class BackupFile(
    val app: String,
    val schemaVersion: Int,
    val exportedAt: String,
    val categories: List<CategoryDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val lots: List<LotDto> = emptyList(),
    val movements: List<MovementDto> = emptyList(),
)

@Serializable
private data class CategoryDto(
    val id: String, val name: String, val icon: String, val color: Long, val sortOrder: Int,
    val updatedAt: Long, val deletedAt: Long? = null,
)

@Serializable
private data class ProductDto(
    val id: String, val barcode: String? = null, val name: String, val brand: String? = null,
    val imageUrl: String? = null, val categoryId: String, val unit: MeasureUnit, val minStock: Double? = null,
    val source: ProductSource, val updatedAt: Long, val deletedAt: Long? = null,
)

@Serializable
private data class LotDto(
    val id: String, val productId: String, val quantity: Double, val expiryDate: String,
    val expiryMonthOnly: Boolean, val addedAt: String, val location: String? = null,
    val updatedAt: Long, val deletedAt: Long? = null,
)

@Serializable
private data class MovementDto(
    val id: String, val lotId: String, val type: MovementType, val quantity: Double, val createdAt: Long,
)

private fun CategoryEntity.toDto() = CategoryDto(id, name, icon, color, sortOrder, updatedAt, deletedAt)
private fun CategoryDto.toEntity() = CategoryEntity(id, name, icon, color, sortOrder, updatedAt, deletedAt)

private fun ProductEntity.toDto() =
    ProductDto(id, barcode, name, brand, imageUrl, categoryId, unit, minStock, source, updatedAt, deletedAt)
private fun ProductDto.toEntity() =
    ProductEntity(id, barcode, name, brand, imageUrl, categoryId, unit, minStock, source, updatedAt, deletedAt)

private fun LotEntity.toDto() = LotDto(
    id, productId, quantity, expiryDate.toString(), expiryMonthOnly, addedAt.toString(), location, updatedAt, deletedAt,
)
private fun LotDto.toEntity() = LotEntity(
    id, productId, quantity, LocalDate.parse(expiryDate), expiryMonthOnly, LocalDate.parse(addedAt), location,
    updatedAt, deletedAt,
)

private fun MovementEntity.toDto() = MovementDto(id, lotId, type, quantity, createdAt)
private fun MovementDto.toEntity() = MovementEntity(id, lotId, type, quantity, createdAt)
