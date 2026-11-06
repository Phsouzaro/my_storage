package com.mystorage.domain.model

import java.time.LocalDate

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: Long,
    val sortOrder: Int,
)

data class Product(
    val id: String,
    val barcode: String?,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val categoryId: String,
    val unit: MeasureUnit,
    val minStock: Double?,
    val source: ProductSource,
)

data class Lot(
    val id: String,
    val productId: String,
    val quantity: Double,
    val expiryDate: LocalDate,
    /** Embalagem traz só MM/AAAA; [expiryDate] guarda o último dia do mês. */
    val expiryMonthOnly: Boolean,
    val addedAt: LocalDate,
    val location: String?,
)

data class Movement(
    val id: String,
    val lotId: String,
    val type: MovementType,
    val quantity: Double,
    val createdAt: Long,
)

enum class MeasureUnit { UN, KG, G, L, ML }

enum class ProductSource { MANUAL, OPEN_FOOD_FACTS, COSMOS }

enum class MovementType { IN, CONSUME, DISCARD, ADJUST }
