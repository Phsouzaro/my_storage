package com.mystorage.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.MovementType
import com.mystorage.domain.model.Product
import com.mystorage.domain.repository.CategoryRepository
import com.mystorage.domain.repository.ProductRepository
import com.mystorage.domain.repository.StockRepository
import com.mystorage.domain.usecase.ConsumeProductUseCase
import com.mystorage.domain.usecase.RemoveFromLotUseCase
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
    val categoryName: String? = null,
    val lots: List<Lot> = emptyList(),
) {
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
) : ViewModel() {

    private val productId = savedStateHandle.toRoute<ProductRoute>().productId

    val state: StateFlow<ProductUiState> = combine(
        productRepository.observeById(productId),
        stockRepository.observeLotsOfProduct(productId),
        categoryRepository.observeAll(),
    ) { product, lots, categories ->
        ProductUiState(
            isLoading = false,
            product = product,
            categoryName = categories.firstOrNull { it.id == product?.categoryId }?.name,
            lots = lots,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductUiState())

    /** Mensagens curtas para o snackbar. */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun consume(quantity: Double) = run("Consumido") { consumeProduct(productId, quantity); quantity }

    fun consumeFromLot(lot: Lot, quantity: Double) =
        run("Consumido") { removeFromLot(lot, quantity, MovementType.CONSUME); quantity }

    fun discardLot(lot: Lot, quantity: Double) =
        run("Descartado") { removeFromLot(lot, quantity, MovementType.DISCARD); quantity }

    private fun run(verb: String, action: suspend () -> Double) {
        viewModelScope.launch {
            val unit = state.value.product?.unit?.label().orEmpty()
            runCatching { action() }
                .onSuccess { qty -> _messages.send("$verb: ${qty.formatQuantity()} $unit") }
                .onFailure { e -> _messages.send(e.message ?: "Não foi possível concluir") }
        }
    }
}
