package com.mystorage.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystorage.domain.model.Category
import com.mystorage.domain.repository.CategoryRepository
import com.mystorage.domain.usecase.DeleteCategoryUseCase
import com.mystorage.domain.usecase.SaveCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryItem(val category: Category, val productCount: Int)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
    private val saveCategory: SaveCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
) : ViewModel() {

    /** null = carregando. */
    val items: StateFlow<List<CategoryItem>?> = combine(
        categoryRepository.observeAll(),
        categoryRepository.observeProductCounts(),
    ) { categories, counts ->
        categories.map { CategoryItem(it, counts[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun save(id: String?, name: String, color: Long) = run(if (id == null) "Categoria criada" else "Categoria atualizada") {
        saveCategory(id, name, color)
    }

    fun delete(id: String) = run("Categoria excluída") { deleteCategory(id) }

    private fun run(success: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { _messages.send(success) }
                .onFailure { e -> _messages.send(e.message ?: "Não foi possível concluir") }
        }
    }

    companion object {
        /** Paleta para novas categorias (mesmos tons das categorias padrão + extras). */
        val PALETTE: List<Long> = listOf(
            0xFFC8A165, 0xFFF2C14E, 0xFFFBC02D, 0xFFD84315, 0xFFE53935, 0xFFEC407A,
            0xFF8E24AA, 0xFF3949AB, 0xFF1E88E5, 0xFF4FC3F7, 0xFF26A69A, 0xFF43A047,
            0xFF9E9E9E, 0xFF8D6E63,
        )
    }
}
