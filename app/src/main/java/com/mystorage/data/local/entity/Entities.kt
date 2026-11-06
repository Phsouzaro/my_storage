package com.mystorage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.MovementType
import com.mystorage.domain.model.ProductSource
import java.time.LocalDate

/*
 * Convenções (ver ARCHITECTURE.md §5):
 * - id = UUID em String (preparado para sync futuro)
 * - updatedAt = epoch millis da última alteração
 * - deletedAt = soft delete (null = ativo)
 */

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: Long,
    val sortOrder: Int,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
        ),
    ],
    indices = [
        Index(value = ["barcode"], unique = true),
        Index("categoryId"),
    ],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val barcode: String?,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val categoryId: String,
    val unit: MeasureUnit,
    val minStock: Double?,
    val source: ProductSource,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "lots",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
        ),
    ],
    indices = [Index("productId"), Index("expiryDate")],
)
data class LotEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val quantity: Double,
    val expiryDate: LocalDate,
    val expiryMonthOnly: Boolean,
    val addedAt: LocalDate,
    val location: String?,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = LotEntity::class,
            parentColumns = ["id"],
            childColumns = ["lotId"],
        ),
    ],
    indices = [Index("lotId")],
)
data class MovementEntity(
    @PrimaryKey val id: String,
    val lotId: String,
    val type: MovementType,
    val quantity: Double,
    val createdAt: Long,
)
