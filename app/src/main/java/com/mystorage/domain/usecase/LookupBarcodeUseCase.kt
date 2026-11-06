package com.mystorage.domain.usecase

import com.mystorage.domain.model.LookupResult
import com.mystorage.domain.repository.ProductInfoSource
import com.mystorage.domain.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/**
 * Catálogo local primeiro; depois cada fonte externa na ordem recebida.
 * Falha de rede numa fonte não interrompe: tenta a próxima.
 */
class LookupBarcodeUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val sources: List<@JvmSuppressWildcards ProductInfoSource>,
) {
    suspend operator fun invoke(barcode: String): LookupResult {
        val code = barcode.trim()
        productRepository.findByBarcode(code)?.let { return LookupResult.Local(it) }

        var offline = false
        for (source in sources) {
            try {
                source.lookup(code)?.let { return LookupResult.Remote(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                offline = true
            }
        }
        return LookupResult.NotFound(code, offline)
    }
}
