package com.mystorage.data.backup

import androidx.room.withTransaction
import com.mystorage.data.local.AppDatabase
import com.mystorage.data.local.dao.BackupDao
import com.mystorage.domain.repository.BackupCounts
import com.mystorage.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomBackupRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: BackupDao,
) : BackupRepository {

    override suspend fun exportJson(): Pair<String, BackupCounts> {
        val snapshot = db.withTransaction {
            BackupSnapshot(dao.allCategories(), dao.allProducts(), dao.allLots(), dao.allMovements())
        }
        val json = withContext(Dispatchers.Default) { BackupSerializer.toJson(snapshot) }
        return json to snapshot.counts()
    }

    override suspend fun importJson(json: String): BackupCounts {
        // Valida tudo antes de tocar no banco: arquivo inválido não apaga nada.
        val snapshot = withContext(Dispatchers.Default) { BackupSerializer.fromJson(json) }
        db.withTransaction {
            dao.deleteMovements()
            dao.deleteLots()
            dao.deleteProducts()
            dao.deleteCategories()
            dao.insertCategories(snapshot.categories)
            dao.insertProducts(snapshot.products)
            dao.insertLots(snapshot.lots)
            dao.insertMovements(snapshot.movements)
        }
        return snapshot.counts()
    }

    private fun BackupSnapshot.counts() = BackupCounts(
        products = products.count { it.deletedAt == null },
        lots = lots.count { it.deletedAt == null && it.quantity > 0 },
    )
}
