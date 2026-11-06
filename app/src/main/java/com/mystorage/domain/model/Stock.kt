package com.mystorage.domain.model

import java.time.LocalDate

/** Dados de um produto vindos de uma fonte externa (API) — ainda não salvos no catálogo. */
data class ProductInfo(
    val barcode: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val source: ProductSource,
)

/** Resultado da busca por código de barras (catálogo local → APIs). */
sealed interface LookupResult {
    data class Local(val product: Product) : LookupResult
    data class Remote(val info: ProductInfo) : LookupResult
    /** [offline] = alguma fonte falhou por rede, então "não encontrado" pode ser falso negativo. */
    data class NotFound(val barcode: String, val offline: Boolean) : LookupResult
}

/** Produto a ser criado junto com o primeiro lote. */
data class NewProduct(
    val barcode: String?,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val categoryId: String,
    val unit: MeasureUnit,
    val source: ProductSource,
)

/** Entrada de estoque: produto existente ([productId]) ou novo ([newProduct]). */
data class NewStockItem(
    val productId: String?,
    val newProduct: NewProduct?,
    val quantity: Double,
    val expiryDate: LocalDate,
    val expiryMonthOnly: Boolean,
    val location: String?,
) {
    init {
        require((productId == null) != (newProduct == null)) { "Informe productId OU newProduct" }
    }
}

/** Saída de estoque de um lote (consumo/descarte). */
data class LotMovement(
    val lotId: String,
    val type: MovementType,
    val quantity: Double,
)

/** Linha da tela de estoque: um produto com a soma dos lotes ativos. */
data class StockSummary(
    val productId: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val unit: MeasureUnit,
    val categoryId: String,
    val categoryName: String,
    val totalQuantity: Double,
    val nextExpiry: LocalDate,
    val lotCount: Int,
)
