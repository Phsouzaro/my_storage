package com.mystorage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mystorage.data.local.dao.BackupDao
import com.mystorage.data.local.dao.CategoryDao
import com.mystorage.data.local.dao.LotDao
import com.mystorage.data.local.dao.MovementDao
import com.mystorage.data.local.dao.ProductDao
import com.mystorage.data.local.entity.CategoryEntity
import com.mystorage.data.local.entity.LotEntity
import com.mystorage.data.local.entity.MovementEntity
import com.mystorage.data.local.entity.ProductEntity

@Database(
    entities = [CategoryEntity::class, ProductEntity::class, LotEntity::class, MovementEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun lotDao(): LotDao
    abstract fun movementDao(): MovementDao
    abstract fun backupDao(): BackupDao

    companion object {
        /** Nome do arquivo; também referenciado em res/xml/backup_rules.xml e data_extraction_rules.xml. */
        const val NAME = "my_storage.db"
    }
}
