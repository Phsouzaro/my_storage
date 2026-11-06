package com.mystorage.domain.usecase

import com.mystorage.domain.model.Category
import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.normalizeForSearch
import com.mystorage.domain.repository.CategoryRepository
import com.mystorage.domain.repository.ProductRepository
import com.mystorage.domain.repository.StockRepository
import java.util.UUID
import javax.inject.Inject

// ---------- Produto ----------

class UpdateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
) {
    suspend operator fun invoke(productId: String, name: String, brand: String?, categoryId: String, unit: MeasureUnit) {
        require(name.isNotBlank()) { "Nome do produto é obrigatório" }
        val current = requireNotNull(productRepository.findById(productId)) { "Produto não encontrado" }
        productRepository.update(
            current.copy(
                name = name.trim(),
                brand = brand?.trim()?.takeIf { it.isNotEmpty() },
                categoryId = categoryId,
                unit = unit,
            ),
        )
    }
}

class DeleteProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
) {
    suspend operator fun invoke(productId: String) = productRepository.delete(productId)
}

// ---------- Lote ----------

class UpdateLotUseCase @Inject constructor(
    private val stockRepository: StockRepository,
) {
    suspend operator fun invoke(lot: Lot) {
        require(lot.quantity > 0) { "Quantidade deve ser maior que zero (para zerar, use Consumir ou Descartar)" }
        stockRepository.updateLot(
            lot.copy(
                expiryDate = if (lot.expiryMonthOnly) lot.expiryDate.endOfMonth() else lot.expiryDate,
                location = lot.location?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }
}

class DeleteLotUseCase @Inject constructor(
    private val stockRepository: StockRepository,
) {
    suspend operator fun invoke(lotId: String) = stockRepository.deleteLot(lotId)
}

// ---------- Categoria ----------

class CategoryInUseException(val productCount: Int) :
    IllegalStateException("Categoria tem $productCount produto(s). Mova-os para outra categoria antes de excluir.")

class SaveCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    /** [id] null = nova categoria. */
    suspend operator fun invoke(id: String?, name: String, color: Long) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Nome da categoria é obrigatório" }
        val all = categoryRepository.getAll()
        require(all.none { it.id != id && it.name.normalizeForSearch() == trimmed.normalizeForSearch() }) {
            "Já existe uma categoria \"$trimmed\""
        }
        val existing = id?.let { cid -> all.firstOrNull { it.id == cid } }
        categoryRepository.save(
            existing?.copy(name = trimmed, color = color)
                ?: Category(
                    id = "cat-${UUID.randomUUID()}",
                    name = trimmed,
                    icon = "other",
                    color = color,
                    sortOrder = (all.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                ),
        )
    }
}

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: String) {
        val count = categoryRepository.productCount(categoryId)
        if (count > 0) throw CategoryInUseException(count)
        categoryRepository.delete(categoryId)
    }
}
