package com.mystorage.domain.repository

import com.mystorage.domain.model.Category
import com.mystorage.domain.model.NewStockItem
import com.mystorage.domain.model.Product
import com.mystorage.domain.model.ProductInfo
import com.mystorage.domain.model.StockSummary
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
}

interface ProductRepository {
    suspend fun findByBarcode(barcode: String): Product?
}

interface StockRepository {
    fun observeSummary(): Flow<List<StockSummary>>

    /** Cria o produto (se novo), o lote e a movimentação de entrada numa única transação. Retorna o id do lote. */
    suspend fun addStockItem(item: NewStockItem): String
}

/** Fonte externa de dados de produto (Open Food Facts, Cosmos...). */
interface ProductInfoSource {
    val name: String

    /** null = não encontrado. Lança IOException em falha de rede. */
    suspend fun lookup(barcode: String): ProductInfo?
}
