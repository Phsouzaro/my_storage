package com.mystorage.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mystorage.domain.model.Category
import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.MovementType
import com.mystorage.domain.model.Product
import com.mystorage.domain.repository.CategoryRepository
import com.mystorage.domain.repository.ProductRepository
import com.mystorage.domain.repository.StockRepository
import com.mystorage.domain.usecase.ConsumeProductUseCase
import com.mystorage.domain.usecase.DeleteLotUseCase
import com.mystorage.domain.usecase.DeleteProductUseCase
import com.mystorage.domain.usecase.RemoveFromLotUseCase
import com.mystorage.domain.usecase.UpdateLotUseCase
import com.mystorage.domain.usecase.UpdateProductUseCase
import com.mystorage.ui.components.formatQuantity
import com.mystorage.ui.components.label
import com.mystorage.ui.navigation.ProductRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val categories: List<Category> = emptyList(),
    val lots: List<Lot> = emptyList(),
) {
    val categoryName: String? get() = categories.firstOrNull { it.id == product?.categoryId }?.name
    val totalQuantity: Double get() = lots.sumOf { it.quantity }
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    productRepository: ProductRepository,
    stockRepository: StockRepository,
    categoryRepository: CategoryRepository,
    private val consumeProduct: ConsumeProductUseCase,
    private val removeFromLot: RemoveFromLotUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val updateLotUseCase: UpdateLotUseCase,
    private val deleteLotUseCase: DeleteLotUseCase,
) : ViewModel() {

    private val productId = savedStateHandle.toRoute<ProductRoute>().productId

    val state: StateFlow<ProductUiState> = combine(
        productRepository.observeById(productId),
        stockRepository.observeLotsOfProduct(productId),
        categoryRepository.observeAll(),
    ) { product, lots, categories ->
        ProductUiState(isLoading = false, product = product, categories = categories, lots = lots)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductUiState())

    /** Mensagens curtas para o snackbar. */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /** Disparado quando o produto foi excluído (a tela fecha). */
    private val _closed = Channel<String>(Channel.BUFFERED)
    val closed = _closed.receiveAsFlow()

    private val unitLabel get() = state.value.product?.unit?.label().orEmpty()

    fun consume(quantity: Double) = run { consumeProduct(productId, quantity); "Consumido: ${quantity.formatQuantity()} $unitLabel" }

    fun consumeFromLot(lot: Lot, quantity: Double) =
        run { removeFromLot(lot, quantity, MovementType.CONSUME); "Consumido: ${quantity.formatQuantity()} $unitLabel" }

    fun discardLot(lot: Lot, quantity: Double) =
        run { removeFromLot(lot, quantity, MovementType.DISCARD); "Descartado: ${quantity.formatQuantity()} $unitLabel" }

    fun updateProduct(name: String, brand: String, categoryId: String, unit: MeasureUnit) =
        run { updateProductUseCase(productId, name, brand, categoryId, unit); "Produto atualizado" }

    fun updateLot(lot: Lot) = run { updateLotUseCase(lot); "Lote atualizado" }

    fun deleteLot(lot: Lot) = run { deleteLotUseCase(lot.id); "Lote excluído" }

    fun deleteProduct() {
        val name = state.value.product?.name.orEmpty()
        viewModelScope.launch {
            runCatching { deleteProductUseCase(productId) }
                .onSuccess { _closed.send("Produto excluído: $name") }
                .onFailure { e -> _messages.send(e.message ?: "Não foi possível excluir") }
        }
    }

    private fun run(action: suspend () -> String) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { _messages.send(it) }
                .onFailure { e -> _messages.send(e.message ?: "Não foi possível concluir") }
        }
    }
}
