package com.mystorage.ui.additem

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mystorage.domain.model.Category
import com.mystorage.domain.model.LookupResult
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.NewProduct
import com.mystorage.domain.model.NewStockItem
import com.mystorage.domain.model.Product
import com.mystorage.domain.model.ProductSource
import com.mystorage.domain.repository.CategoryRepository
import com.mystorage.domain.usecase.AddStockItemUseCase
import com.mystorage.domain.usecase.LookupBarcodeUseCase
import com.mystorage.ui.navigation.AddItemRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddItemUiState(
    val isLoading: Boolean = true,
    val barcode: String? = null,
    /** Produto já existente no catálogo: só quantidade/validade são editáveis. */
    val existingProduct: Product? = null,
    val lookupMessage: String? = null,
    val name: String = "",
    val brand: String = "",
    val imageUrl: String? = null,
    val source: ProductSource = ProductSource.MANUAL,
    val categoryId: String? = null,
    val unit: MeasureUnit = MeasureUnit.UN,
    val quantity: String = "1",
    val expiryDate: LocalDate? = null,
    val expiryMonthOnly: Boolean = false,
    val location: String = "",
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val parsedQuantity: Double? get() = quantity.replace(',', '.').toDoubleOrNull()

    val canSave: Boolean
        get() = !isLoading && !isSaving &&
            (parsedQuantity ?: 0.0) > 0 &&
            expiryDate != null &&
            (existingProduct != null || (name.isNotBlank() && categoryId != null))
}

@HiltViewModel
class AddItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lookupBarcode: LookupBarcodeUseCase,
    private val addStockItem: AddStockItemUseCase,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AddItemRoute>()

    private val _state = MutableStateFlow(AddItemUiState(barcode = route.barcode, isLoading = route.barcode != null))
    val state: StateFlow<AddItemUiState> = _state

    /** Emite o nome do produto salvo; a tela volta para o scanner. */
    private val _saved = Channel<String>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeAll().collect { list -> _state.update { it.copy(categories = list) } }
        }
        route.barcode?.let { code -> viewModelScope.launch { lookup(code) } }
    }

    private suspend fun lookup(code: String) {
        val result = runCatching { lookupBarcode(code) }
            .getOrElse { LookupResult.NotFound(code, offline = true) }
        _state.update { s ->
            when (result) {
                is LookupResult.Local -> s.copy(
                    isLoading = false,
                    existingProduct = result.product,
                    name = result.product.name,
                    brand = result.product.brand.orEmpty(),
                    imageUrl = result.product.imageUrl,
                    categoryId = result.product.categoryId,
                    unit = result.product.unit,
                    lookupMessage = "Já está no seu catálogo",
                )
                is LookupResult.Remote -> s.copy(
                    isLoading = false,
                    name = result.info.name,
                    brand = result.info.brand.orEmpty(),
                    imageUrl = result.info.imageUrl,
                    source = result.info.source,
                    lookupMessage = when (result.info.source) {
                        ProductSource.OPEN_FOOD_FACTS -> "Encontrado no Open Food Facts — confira os dados"
                        ProductSource.COSMOS -> "Encontrado no Cosmos — confira os dados"
                        ProductSource.MANUAL -> null
                    },
                )
                is LookupResult.NotFound -> s.copy(
                    isLoading = false,
                    lookupMessage = if (result.offline) {
                        "Sem conexão — preencha manualmente"
                    } else {
                        "Produto não encontrado — preencha manualmente"
                    },
                )
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onBrandChange(v: String) = _state.update { it.copy(brand = v) }
    fun onCategoryChange(id: String) = _state.update { it.copy(categoryId = id) }
    fun onUnitChange(u: MeasureUnit) = _state.update { it.copy(unit = u) }
    fun onLocationChange(v: String) = _state.update { it.copy(location = v) }
    fun onExpiryChange(date: LocalDate) = _state.update { it.copy(expiryDate = date) }
    fun onMonthOnlyChange(v: Boolean) = _state.update { it.copy(expiryMonthOnly = v) }

    fun onQuantityChange(v: String) {
        val cleaned = v.filter { it.isDigit() || it == ',' || it == '.' }.take(8)
        _state.update { it.copy(quantity = cleaned) }
    }

    fun stepQuantity(delta: Int) = _state.update { s ->
        val next = ((s.parsedQuantity ?: 0.0) + delta).coerceAtLeast(1.0)
        s.copy(quantity = if (next % 1.0 == 0.0) next.toLong().toString() else next.toString())
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                addStockItem(
                    NewStockItem(
                        productId = s.existingProduct?.id,
                        newProduct = if (s.existingProduct == null) {
                            NewProduct(
                                barcode = s.barcode,
                                name = s.name,
                                brand = s.brand,
                                imageUrl = s.imageUrl,
                                categoryId = requireNotNull(s.categoryId),
                                unit = s.unit,
                                source = s.source,
                            )
                        } else null,
                        quantity = requireNotNull(s.parsedQuantity),
                        expiryDate = requireNotNull(s.expiryDate),
                        expiryMonthOnly = s.expiryMonthOnly,
                        location = s.location,
                    ),
                )
            }.onSuccess {
                _saved.send(s.name.trim())
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false, error = e.message ?: "Erro ao salvar") }
            }
        }
    }
}
