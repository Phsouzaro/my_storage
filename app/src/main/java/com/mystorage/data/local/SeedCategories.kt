package com.mystorage.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Categorias padrão inseridas na criação do banco. IDs fixos para serem estáveis entre instalações. */
object SeedCategories {

    data class Seed(val id: String, val name: String, val icon: String, val color: Long)

    val defaults = listOf(
        Seed("cat-graos", "Grãos e Cereais", "grain", 0xFFC8A165),
        Seed("cat-massas", "Massas", "pasta", 0xFFF2C14E),
        Seed("cat-enlatados", "Enlatados e Conservas", "can", 0xFF9E9E9E),
        Seed("cat-molhos", "Molhos e Temperos", "spice", 0xFFD84315),
        Seed("cat-oleos", "Óleos e Gorduras", "oil", 0xFFFBC02D),
        Seed("cat-doces", "Açúcares e Doces", "sweet", 0xFFEC407A),
        Seed("cat-bebidas", "Bebidas", "drink", 0xFF1E88E5),
        Seed("cat-laticinios", "Laticínios", "dairy", 0xFFFFF59D),
        Seed("cat-congelados", "Congelados", "frozen", 0xFF4FC3F7),
        Seed("cat-limpeza", "Higiene e Limpeza", "cleaning", 0xFF26A69A),
        Seed("cat-outros", "Outros", "other", 0xFF8D6E63),
    )

    val callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            defaults.forEachIndexed { index, seed ->
                db.execSQL(
                    "INSERT INTO categories (id, name, icon, color, sortOrder, updatedAt, deletedAt) VALUES (?, ?, ?, ?, ?, ?, NULL)",
                    arrayOf<Any>(seed.id, seed.name, seed.icon, seed.color, index, now),
                )
            }
        }
    }
}
