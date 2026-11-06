package com.mystorage.ui.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mystorage.domain.model.StatusFilter
import com.mystorage.domain.model.StockSummary
import com.mystorage.ui.components.ExpiryBadge
import com.mystorage.ui.components.formatDate
import com.mystorage.ui.components.formatQuantity
import com.mystorage.ui.components.label

@Composable
fun StockScreen(
    onProductClick: (String) -> Unit,
    onScanClick: () -> Unit,
    viewModel: StockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Escanear") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            if (state.totalCount > 0) {
                FilterBar(state, viewModel)
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.totalCount == 0 -> CenterMessage(
                    "Seu estoque está vazio.\nToque em Escanear para adicionar o primeiro produto.",
                )
                state.items.isEmpty() -> CenterMessage("Nenhum produto encontrado com esses filtros.") {
                    TextButton(onClick = viewModel::clearFilters) { Text("Limpar filtros") }
                }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (state.filter.isActive) {
                        item {
                            Text(
                                "${state.items.size} de ${state.totalCount} produtos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                    items(state.items, key = { it.productId }) { item ->
                        StockRow(item, onClick = { onProductClick(item.productId) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(state: StockUiState, vm: StockViewModel) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedTextField(
            value = state.filter.query,
            onValueChange = vm::onQueryChange,
            placeholder = { Text("Buscar por nome ou marca") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.filter.query.isNotEmpty()) {
                    IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "Limpar busca") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip(state, vm::onCategoryChange)
            listOf(
                StatusFilter.EXPIRED to "Vencidos",
                StatusFilter.CRITICAL to "Até 7 dias",
                StatusFilter.WARNING to "Até ${state.warningDays} dias",
            ).forEach { (status, label) ->
                val selected = state.filter.status == status
                FilterChip(
                    selected = selected,
                    onClick = { vm.onStatusChange(if (selected) StatusFilter.ALL else status) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(state: StockUiState, onSelect: (String?) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = state.categories.firstOrNull { it.id == state.filter.categoryId }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected?.name ?: "Categoria") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todas") }, onClick = { onSelect(null); expanded = false })
            state.categories.forEach { category ->
                DropdownMenuItem(text = { Text(category.name) }, onClick = { onSelect(category.id); expanded = false })
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String, action: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        action()
    }
}

@Composable
private fun StockRow(item: StockSummary, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text(item.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium) }
            }
        },
        headlineContent = { Text(item.name) },
        supportingContent = {
            Column {
                Text(
                    listOfNotNull(item.brand, item.categoryName).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
                ExpiryBadge(item.nextExpiry)
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${item.totalQuantity.formatQuantity()} ${item.unit.label()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (item.lotCount > 1) "${item.lotCount} lotes" else item.nextExpiry.formatDate(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
    )
}
