package com.mystorage.data.repository

import androidx.room.withTransaction
import com.mystorage.data.local.AppDatabase
import com.mystorage.data.local.dao.CategoryDao
import com.mystorage.data.local.dao.LotDao
import com.mystorage.data.local.dao.MovementDao
import com.mystorage.data.local.dao.ProductDao
import com.mystorage.data.local.entity.LotEntity
import com.mystorage.data.local.entity.MovementEntity
import com.mystorage.data.local.entity.ProductEntity
import com.mystorage.data.local.toDomain
import com.mystorage.domain.model.Category
import com.mystorage.domain.model.LotWithProduct
import com.mystorage.domain.model.MovementType
import com.mystorage.domain.model.NewStockItem
import com.mystorage.domain.model.Product
import com.mystorage.domain.model.StockSummary
import com.mystorage.domain.repository.CategoryRepository
import com.mystorage.domain.repository.ProductRepository
import com.mystorage.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
}

class RoomProductRepository @Inject constructor(
    private val dao: ProductDao,
) : ProductRepository {
    override suspend fun findByBarcode(barcode: String): Product? = dao.findByBarcode(barcode)?.toDomain()
}

class RoomStockRepository @Inject constructor(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val lotDao: LotDao,
    private val movementDao: MovementDao,
) : StockRepository {

    override fun observeSummary(): Flow<List<StockSummary>> =
        lotDao.observeStockSummary().map { rows -> rows.map { it.toDomain() } }

    override fun observeActiveLots(): Flow<List<LotWithProduct>> =
        lotDao.observeActiveWithProduct().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getActiveLots(): List<LotWithProduct> = lotDao.getActiveWithProduct().map { it.toDomain() }

    override suspend fun addStockItem(item: NewStockItem): String = db.withTransaction {
        val now = System.currentTimeMillis()

        val productId = item.productId ?: run {
            val p = requireNotNull(item.newProduct)
            // Se o código já existe (ex.: escaneado duas vezes rápido), reaproveita o produto.
            p.barcode?.let { productDao.findByBarcode(it) }?.id
                ?: UUID.randomUUID().toString().also { id ->
                    productDao.upsert(
                        ProductEntity(
                            id = id,
                            barcode = p.barcode,
                            name = p.name,
                            brand = p.brand,
                            imageUrl = p.imageUrl,
                            categoryId = p.categoryId,
                            unit = p.unit,
                            minStock = null,
                            source = p.source,
                            updatedAt = now,
                        ),
                    )
                }
        }

        val lotId = UUID.randomUUID().toString()
        lotDao.upsert(
            LotEntity(
                id = lotId,
                productId = productId,
                quantity = item.quantity,
                expiryDate = item.expiryDate,
                expiryMonthOnly = item.expiryMonthOnly,
                addedAt = LocalDate.now(),
                location = item.location,
                updatedAt = now,
            ),
        )
        movementDao.insert(
            MovementEntity(
                id = UUID.randomUUID().toString(),
                lotId = lotId,
                type = MovementType.IN,
                quantity = item.quantity,
                createdAt = now,
            ),
        )
        lotId
    }
}
