package com.mystorage.di

import android.content.Context
import androidx.room.Room
import com.mystorage.data.local.AppDatabase
import com.mystorage.data.local.SeedCategories
import com.mystorage.data.local.dao.CategoryDao
import com.mystorage.data.local.dao.LotDao
import com.mystorage.data.local.dao.MovementDao
import com.mystorage.data.local.dao.ProductDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addCallback(SeedCategories.callback)
            .build()

    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
    @Provides fun provideLotDao(db: AppDatabase): LotDao = db.lotDao()
    @Provides fun provideMovementDao(db: AppDatabase): MovementDao = db.movementDao()
}
