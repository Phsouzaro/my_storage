package com.mystorage.data.repository

import androidx.room.withTransaction
import com.mystorage.data.local.AppDatabase
import com.mystorage.data.local.dao.CategoryDao
import com.mystorage.data.local.dao.LotDao
import com.mystorage.data.local.dao.MovementDao
import com.mystorage.data.local.dao.ProductDao
import com.mystorage.data.local.entity.CategoryEntity
import com.mystorage.data.local.entity.LotEntity
import com.mystorage.data.local.entity.MovementEntity
import com.mystorage.data.local.entity.ProductEntity
import com.mystorage.data.local.toDomain
import com.mystorage.domain.model.Category
import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.LotMovement
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

    override fun observeProductCounts(): Flow<Map<String, Int>> =
        dao.observeProductCounts().map { rows -> rows.associate { it.categoryId to it.count } }

    override suspend fun getAll(): List<Category> = dao.getAll().map { it.toDomain() }

    override suspend fun productCount(categoryId: String): Int = dao.productCount(categoryId)

    override suspend fun save(category: Category) {
        val existing = dao.findById(category.id)
        dao.upsert(
            CategoryEntity(
                id = category.id,
                name = category.name,
                icon = category.icon,
                color = category.color,
                sortOrder = category.sortOrder,
                updatedAt = System.currentTimeMillis(),
                deletedAt = existing?.deletedAt,
            ),
        )
    }

    override suspend fun delete(categoryId: String) = dao.softDelete(categoryId, System.currentTimeMillis())
}

class RoomProductRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: ProductDao,
    private val lotDao: LotDao,
) : ProductRepository {
    override suspend fun findByBarcode(barcode: String): Product? = dao.findByBarcode(barcode)?.toDomain()

    override suspend fun findById(id: String): Product? = dao.findById(id)?.toDomain()

    override fun observeById(id: String): Flow<Product?> =
        dao.observeById(id).map { it?.takeIf { p -> p.deletedAt == null }?.toDomain() }

    override suspend fun update(product: Product) {
        val current = requireNotNull(dao.findById(product.id)) { "Produto não encontrado" }
        dao.upsert(
            current.copy(
                name = product.name,
                brand = product.brand,
                categoryId = product.categoryId,
                unit = product.unit,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun delete(productId: String) = db.withTransaction {
        val now = System.currentTimeMillis()
        lotDao.softDeleteByProduct(productId, now)
        dao.softDelete(productId, now)
    }
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

    override fun observeLotsOfProduct(productId: String): Flow<List<Lot>> =
        lotDao.observeByProduct(productId).map { list -> list.map { it.toDomain() } }

    override suspend fun getLotsOfProduct(productId: String): List<Lot> =
        lotDao.getByProduct(productId).map { it.toDomain() }

    override suspend fun applyMovements(movements: List<LotMovement>) = db.withTransaction {
        val now = System.currentTimeMillis()
        movements.forEach { m ->
            require(m.type != MovementType.IN) { "applyMovements é só para saídas" }
            val lot = requireNotNull(lotDao.findById(m.lotId)) { "Lote ${m.lotId} não encontrado" }
            require(m.quantity <= lot.quantity + 1e-9) { "Quantidade maior que o lote" }
            // Arredonda resíduos de ponto flutuante (ex.: 0.30000000000000004) para zero.
            val remaining = (lot.quantity - m.quantity).let { if (it < 1e-9) 0.0 else it }
            lotDao.updateQuantity(lot.id, remaining, now)
            movementDao.insert(
                MovementEntity(
                    id = UUID.randomUUID().toString(),
                    lotId = lot.id,
                    type = m.type,
                    quantity = m.quantity,
                    createdAt = now,
                ),
            )
        }
    }

    override suspend fun addStockItem(item: NewStockItem): String = db.withTransaction {
        val now = System.currentTimeMillis()

        val productId = item.productId ?: run {
            val p = requireNotNull(item.newProduct)
            val sameBarcode = p.barcode?.let { productDao.findByBarcodeIncludingDeleted(it) }
            when {
                // Código já ativo (ex.: escaneado duas vezes rápido): reaproveita o produto.
                sameBarcode != null && sameBarcode.deletedAt == null -> sameBarcode.id
                // Código de um produto excluído: o barcode é único, então "ressuscita" o registro com os dados novos.
                sameBarcode != null -> sameBarcode.id.also {
                    productDao.upsert(
                        sameBarcode.copy(
                            name = p.name,
                            brand = p.brand,
                            imageUrl = p.imageUrl,
                            categoryId = p.categoryId,
                            unit = p.unit,
                            source = p.source,
                            updatedAt = now,
                            deletedAt = null,
                        ),
                    )
                }
                else -> UUID.randomUUID().toString().also { id ->
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

    override suspend fun updateLot(lot: Lot) = db.withTransaction {
        val current = requireNotNull(lotDao.findById(lot.id)) { "Lote não encontrado" }
        val now = System.currentTimeMillis()
        lotDao.upsert(
            current.copy(
                quantity = lot.quantity,
                expiryDate = lot.expiryDate,
                expiryMonthOnly = lot.expiryMonthOnly,
                location = lot.location,
                updatedAt = now,
            ),
        )
        val delta = lot.quantity - current.quantity
        if (kotlin.math.abs(delta) > 1e-9) {
            movementDao.insert(
                MovementEntity(
                    id = UUID.randomUUID().toString(),
                    lotId = lot.id,
                    type = MovementType.ADJUST,
                    quantity = delta,
                    createdAt = now,
                ),
            )
        }
    }

    override suspend fun deleteLot(lotId: String) = lotDao.softDelete(lotId, System.currentTimeMillis())
}
