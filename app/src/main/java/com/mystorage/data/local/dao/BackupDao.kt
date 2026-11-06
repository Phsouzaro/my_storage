package com.mystorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mystorage.data.local.entity.CategoryEntity
import com.mystorage.data.local.entity.LotEntity
import com.mystorage.data.local.entity.MovementEntity
import com.mystorage.data.local.entity.ProductEntity

/** Acesso "em massa" usado só pelo backup (exportar tudo / substituir tudo). */
@Dao
interface BackupDao {
    @Query("SELECT * FROM categories") suspend fun allCategories(): List<CategoryEntity>
    @Query("SELECT * FROM products") suspend fun allProducts(): List<ProductEntity>
    @Query("SELECT * FROM lots") suspend fun allLots(): List<LotEntity>
    @Query("SELECT * FROM movements") suspend fun allMovements(): List<MovementEntity>

    // Ordem de exclusão respeita as chaves estrangeiras (filhos antes dos pais).
    @Query("DELETE FROM movements") suspend fun deleteMovements()
    @Query("DELETE FROM lots") suspend fun deleteLots()
    @Query("DELETE FROM products") suspend fun deleteProducts()
    @Query("DELETE FROM categories") suspend fun deleteCategories()

    @Insert suspend fun insertCategories(items: List<CategoryEntity>)
    @Insert suspend fun insertProducts(items: List<ProductEntity>)
    @Insert suspend fun insertLots(items: List<LotEntity>)
    @Insert suspend fun insertMovements(items: List<MovementEntity>)
}
