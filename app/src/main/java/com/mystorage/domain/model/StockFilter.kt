package com.mystorage.domain.model

import java.text.Normalizer
import java.time.LocalDate

/** Filtro por situação de validade (pela validade mais próxima do produto). */
enum class StatusFilter { ALL, EXPIRED, CRITICAL, WARNING }

data class StockFilter(
    val query: String = "",
    val categoryId: String? = null,
    val status: StatusFilter = StatusFilter.ALL,
) {
    val isActive: Boolean get() = query.isNotBlank() || categoryId != null || status != StatusFilter.ALL

    fun apply(items: List<StockSummary>, today: LocalDate, thresholds: ExpiryThresholds): List<StockSummary> {
        val terms = query.normalizeForSearch().split(' ').filter { it.isNotEmpty() }
        return items.filter { item ->
            (categoryId == null || item.categoryId == categoryId) &&
                matchesStatus(expiryStatusOf(item.nextExpiry, today, thresholds)) &&
                terms.all { term -> "${item.name} ${item.brand.orEmpty()}".normalizeForSearch().contains(term) }
        }
    }

    private fun matchesStatus(s: ExpiryStatus) = when (status) {
        StatusFilter.ALL -> true
        StatusFilter.EXPIRED -> s == ExpiryStatus.EXPIRED
        StatusFilter.CRITICAL -> s == ExpiryStatus.CRITICAL
        StatusFilter.WARNING -> s == ExpiryStatus.WARNING
    }
}

/** "Feijão  CARIOCA" → "feijao carioca" — busca sem acento e sem diferenciar maiúsculas. */
fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")
