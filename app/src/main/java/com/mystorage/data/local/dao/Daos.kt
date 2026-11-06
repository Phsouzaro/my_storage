package com.mystorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.mystorage.data.local.entity.CategoryEntity
import com.mystorage.data.local.entity.LotEntity
import com.mystorage.data.local.entity.MovementEntity
import com.mystorage.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(category: CategoryEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE deletedAt IS NULL ORDER BY name")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode AND deletedAt IS NULL LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun findById(id: String): ProductEntity?

    @Upsert
    suspend fun upsert(product: ProductEntity)
}

@Dao
interface LotDao {
    /** Lotes ativos (não excluídos e com saldo), do que vence primeiro para o último. */
    @Query("SELECT * FROM lots WHERE deletedAt IS NULL AND quantity > 0 ORDER BY expiryDate")
    fun observeActive(): Flow<List<LotEntity>>

    @Query("SELECT * FROM lots WHERE productId = :productId AND deletedAt IS NULL AND quantity > 0 ORDER BY expiryDate")
    fun observeByProduct(productId: String): Flow<List<LotEntity>>

    /** Lotes ativos que vencem até [limitEpochDay] (inclusive), incluindo os já vencidos. */
    @Query("SELECT * FROM lots WHERE deletedAt IS NULL AND quantity > 0 AND expiryDate <= :limitEpochDay ORDER BY expiryDate")
    suspend fun findExpiringUntil(limitEpochDay: Long): List<LotEntity>

    @Upsert
    suspend fun upsert(lot: LotEntity)
}

@Dao
interface MovementDao {
    @Insert
    suspend fun insert(movement: MovementEntity)

    @Query("SELECT * FROM movements WHERE lotId = :lotId ORDER BY createdAt DESC")
    fun observeByLot(lotId: String): Flow<List<MovementEntity>>
}
