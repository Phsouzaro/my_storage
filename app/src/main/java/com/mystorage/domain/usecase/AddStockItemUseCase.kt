package com.mystorage.domain.usecase

import com.mystorage.domain.model.NewStockItem
import com.mystorage.domain.repository.StockRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class AddStockItemUseCase @Inject constructor(
    private val stockRepository: StockRepository,
) {
    /** Retorna o id do lote criado. */
    suspend operator fun invoke(item: NewStockItem): String {
        require(item.quantity > 0) { "Quantidade deve ser maior que zero" }
        item.newProduct?.let { require(it.name.isNotBlank()) { "Nome do produto é obrigatório" } }

        val normalized = item.copy(
            expiryDate = if (item.expiryMonthOnly) item.expiryDate.endOfMonth() else item.expiryDate,
            location = item.location?.trim()?.takeIf { it.isNotEmpty() },
            newProduct = item.newProduct?.let { p ->
                p.copy(
                    name = p.name.trim(),
                    brand = p.brand?.trim()?.takeIf { it.isNotEmpty() },
                    barcode = p.barcode?.trim()?.takeIf { it.isNotEmpty() },
                )
            },
        )
        return stockRepository.addStockItem(normalized)
    }
}

fun LocalDate.endOfMonth(): LocalDate = YearMonth.from(this).atEndOfMonth()
