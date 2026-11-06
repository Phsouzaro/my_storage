package com.mystorage.domain.repository

import com.mystorage.domain.model.AppSettings
import com.mystorage.domain.model.Category
import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.LotMovement
import com.mystorage.domain.model.LotWithProduct
import java.time.LocalDate
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
    suspend fun findById(id: String): Product?
    fun observeById(id: String): Flow<Product?>
}

interface StockRepository {
    fun observeSummary(): Flow<List<StockSummary>>

    /** Lotes ativos com dados do produto, ordenados pela validade. */
    fun observeActiveLots(): Flow<List<LotWithProduct>>

    suspend fun getActiveLots(): List<LotWithProduct>

    /** Lotes com saldo de um produto, do que vence primeiro para o último. */
    fun observeLotsOfProduct(productId: String): Flow<List<Lot>>

    suspend fun getLotsOfProduct(productId: String): List<Lot>

    /** Aplica saídas (baixa a quantidade dos lotes e registra os movimentos) numa única transação. */
    suspend fun applyMovements(movements: List<LotMovement>)

    /** Cria o produto (se novo), o lote e a movimentação de entrada numa única transação. Retorna o id do lote. */
    suspend fun addStockItem(item: NewStockItem): String
}

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun get(): AppSettings
    suspend fun setWarningDays(days: Int)
    suspend fun setNotifyHour(hour: Int)

    /** Data da última verificação de validade (para o worker saber o que é novidade). */
    suspend fun getLastExpiryCheck(): LocalDate?
    suspend fun setLastExpiryCheck(date: LocalDate)
}

/** Fonte externa de dados de produto (Open Food Facts, Cosmos...). */
interface ProductInfoSource {
    val name: String

    /** null = não encontrado. Lança IOException em falha de rede. */
    suspend fun lookup(barcode: String): ProductInfo?
}
