package com.mystorage.fakes

import com.mystorage.domain.model.Category
import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.LotMovement
import com.mystorage.domain.model.LotWithProduct
import com.mystorage.domain.model.NewStockItem
import com.mystorage.domain.model.Product
import com.mystorage.domain.model.StockSummary
import com.mystorage.domain.repository.CategoryRepository
import com.mystorage.domain.repository.ProductRepository
import com.mystorage.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/** Bases para fakes de teste: cada teste sobrescreve só o que usa; o resto falha se for chamado. */
open class FakeStockRepository : StockRepository {
    override fun observeSummary(): Flow<List<StockSummary>> = emptyFlow()
    override fun observeActiveLots(): Flow<List<LotWithProduct>> = emptyFlow()
    override suspend fun getActiveLots(): List<LotWithProduct> = emptyList()
    override fun observeLotsOfProduct(productId: String): Flow<List<Lot>> = emptyFlow()
    override suspend fun getLotsOfProduct(productId: String): List<Lot> = emptyList()
    override suspend fun applyMovements(movements: List<LotMovement>): Unit = error("não usado")
    override suspend fun addStockItem(item: NewStockItem): String = error("não usado")
    override suspend fun updateLot(lot: Lot): Unit = error("não usado")
    override suspend fun deleteLot(lotId: String): Unit = error("não usado")
}

open class FakeProductRepository(private val products: List<Product> = emptyList()) : ProductRepository {
    val updated = mutableListOf<Product>()
    override suspend fun findByBarcode(barcode: String) = products.firstOrNull { it.barcode == barcode }
    override suspend fun findById(id: String) = products.firstOrNull { it.id == id }
    override fun observeById(id: String): Flow<Product?> = flowOf(products.firstOrNull { it.id == id })
    override suspend fun update(product: Product) { updated += product }
    override suspend fun delete(productId: String): Unit = error("não usado")
}

open class FakeCategoryRepository(
    initial: List<Category> = emptyList(),
    private val counts: Map<String, Int> = emptyMap(),
) : CategoryRepository {
    val categories = initial.toMutableList()
    val deleted = mutableListOf<String>()
    override fun observeAll(): Flow<List<Category>> = flowOf(categories)
    override fun observeProductCounts(): Flow<Map<String, Int>> = flowOf(counts)
    override suspend fun getAll(): List<Category> = categories.toList()
    override suspend fun productCount(categoryId: String) = counts[categoryId] ?: 0
    override suspend fun save(category: Category) {
        categories.removeAll { it.id == category.id }
        categories += category
    }
    override suspend fun delete(categoryId: String) { deleted += categoryId }
}
