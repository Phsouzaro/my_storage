package com.mystorage.data.local

import com.mystorage.data.local.dao.LotWithProductRow
import com.mystorage.data.local.dao.StockSummaryRow
import com.mystorage.domain.model.LotWithProduct
import com.mystorage.data.local.entity.CategoryEntity
import com.mystorage.data.local.entity.ProductEntity
import com.mystorage.domain.model.Category
import com.mystorage.domain.model.Product
import com.mystorage.domain.model.StockSummary

fun CategoryEntity.toDomain() = Category(id, name, icon, color, sortOrder)

fun ProductEntity.toDomain() = Product(
    id = id,
    barcode = barcode,
    name = name,
    brand = brand,
    imageUrl = imageUrl,
    categoryId = categoryId,
    unit = unit,
    minStock = minStock,
    source = source,
)

fun LotWithProductRow.toDomain() = LotWithProduct(
    lotId = lotId,
    productId = productId,
    productName = productName,
    brand = brand,
    imageUrl = imageUrl,
    unit = unit,
    quantity = quantity,
    expiryDate = expiryDate,
    expiryMonthOnly = expiryMonthOnly,
    addedAt = addedAt,
)

fun StockSummaryRow.toDomain() = StockSummary(
    productId = productId,
    name = name,
    brand = brand,
    imageUrl = imageUrl,
    unit = unit,
    categoryName = categoryName,
    totalQuantity = totalQuantity,
    nextExpiry = nextExpiry,
    lotCount = lotCount,
)
