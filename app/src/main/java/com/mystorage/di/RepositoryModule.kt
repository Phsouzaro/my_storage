package com.mystorage.di

import com.mystorage.data.backup.RoomBackupRepository
import com.mystorage.data.repository.RoomCategoryRepository
import com.mystorage.domain.repository.BackupRepository
import com.mystorage.data.repository.RoomProductRepository
import com.mystorage.data.repository.RoomStockRepository
import com.mystorage.domain.repository.CategoryRepository
import com.mystorage.domain.repository.ProductRepository
import com.mystorage.domain.repository.StockRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository
    @Binds abstract fun bindProductRepository(impl: RoomProductRepository): ProductRepository
    @Binds abstract fun bindStockRepository(impl: RoomStockRepository): StockRepository
    @Binds abstract fun bindBackupRepository(impl: RoomBackupRepository): BackupRepository
}
