package com.mystorage.ui.product

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.Product
import com.mystorage.ui.components.ExpiryBadge
import com.mystorage.ui.components.QuantityDialog
import com.mystorage.ui.components.formatDate
import com.mystorage.ui.components.formatQuantity
import com.mystorage.ui.components.label

/** Diálogo aberto no momento (sobrevive à rotação via ids). */
private sealed interface PendingAction {
    data object ConsumeProduct : PendingAction
    data object EditProduct : PendingAction
    data object DeleteProduct : PendingAction
    data class ConsumeLot(val lotId: String) : PendingAction
    data class DiscardLot(val lotId: String) : PendingAction
    data class EditLot(val lotId: String) : PendingAction
    data class DeleteLot(val lotId: String) : PendingAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    onBack: () -> Unit,
    onAddLot: (Product) -> Unit,
    viewModel: ProductViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pending by remember { mutableStateOf<PendingAction?>(null) }

    val context = LocalContext.current
    var topMenu by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.closed.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.product?.name ?: "Produto", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    if (state.product != null) {
                        IconButton(onClick = { pending = PendingAction.EditProduct }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar produto")
                        }
                        Box {
                            IconButton(onClick = { topMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
                            }
                            DropdownMenu(expanded = topMenu, onDismissRequest = { topMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Excluir produto") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                    onClick = { topMenu = false; pending = PendingAction.DeleteProduct },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val product = state.product
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            product == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Produto não encontrado")
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ProductHeader(product, state) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { pending = PendingAction.ConsumeProduct },
                            enabled = state.totalQuantity > 0,
                            modifier = Modifier.weight(1f),
                        ) { Text("Consumir") }
                        OutlinedButton(onClick = { onAddLot(product) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(" Adicionar lote")
                        }
                    }
                }
                item {
                    Text(
                        if (state.lots.isEmpty()) "Lotes" else "Lotes (do que vence primeiro)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (state.lots.isEmpty()) {
                    item {
                        Text(
                            "Sem estoque deste produto.\nUse \"Adicionar lote\" quando comprar de novo.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                        )
                    }
                }
                itemsIndexed(state.lots, key = { _, lot -> lot.id }) { index, lot ->
                    LotCard(
                        lot = lot,
                        product = product,
                        isNext = index == 0 && state.lots.size > 1,
                        onConsume = { pending = PendingAction.ConsumeLot(lot.id) },
                        onDiscard = { pending = PendingAction.DiscardLot(lot.id) },
                        onEdit = { pending = PendingAction.EditLot(lot.id) },
                        onDelete = { pending = PendingAction.DeleteLot(lot.id) },
                    )
                }
            }
        }
    }

    val product = state.product ?: return
    when (val action = pending) {
        null -> Unit
        PendingAction.ConsumeProduct -> QuantityDialog(
            title = "Consumir",
            confirmLabel = "Consumir",
            max = state.totalQuantity,
            unit = product.unit,
            message = "Retira primeiro dos lotes que vencem antes.",
            onDismiss = { pending = null },
            onConfirm = { viewModel.consume(it); pending = null },
        )
        is PendingAction.ConsumeLot -> state.lots.firstOrNull { it.id == action.lotId }?.let { lot ->
            QuantityDialog(
                title = "Consumir deste lote",
                confirmLabel = "Consumir",
                max = lot.quantity,
                unit = product.unit,
                message = "Validade ${lot.expiryDate.formatDate(lot.expiryMonthOnly)}",
                onDismiss = { pending = null },
                onConfirm = { viewModel.consumeFromLot(lot, it); pending = null },
            )
        }
        is PendingAction.DiscardLot -> state.lots.firstOrNull { it.id == action.lotId }?.let { lot ->
            QuantityDialog(
                title = "Descartar",
                confirmLabel = "Descartar",
                max = lot.quantity,
                unit = product.unit,
                initial = lot.quantity,
                message = "Registra a perda (vencido, estragado...). Validade ${lot.expiryDate.formatDate(lot.expiryMonthOnly)}.",
                onDismiss = { pending = null },
                onConfirm = { viewModel.discardLot(lot, it); pending = null },
            )
        }
        PendingAction.EditProduct -> EditProductDialog(
            product = product,
            categories = state.categories,
            onDismiss = { pending = null },
            onSave = { name, brand, categoryId, unit ->
                viewModel.updateProduct(name, brand, categoryId, unit)
                pending = null
            },
        )
        PendingAction.DeleteProduct -> ConfirmDialog(
            title = "Excluir produto?",
            message = "\"${product.name}\" e todos os seus lotes saem do estoque. " +
                "Se escanear o código de novo, ele será cadastrado outra vez.",
            confirmLabel = "Excluir",
            onDismiss = { pending = null },
            onConfirm = { viewModel.deleteProduct(); pending = null },
        )
        is PendingAction.EditLot -> state.lots.firstOrNull { it.id == action.lotId }?.let { lot ->
            EditLotDialog(
                lot = lot,
                unit = product.unit,
                onDismiss = { pending = null },
                onSave = { viewModel.updateLot(it); pending = null },
            )
        }
        is PendingAction.DeleteLot -> state.lots.firstOrNull { it.id == action.lotId }?.let { lot ->
            ConfirmDialog(
                title = "Excluir lote?",
                message = "Use para corrigir um lote lançado por engano. " +
                    "Se o produto estragou ou venceu, prefira \"Descartar\" — assim a perda fica no histórico.",
                confirmLabel = "Excluir",
                onDismiss = { pending = null },
                onConfirm = { viewModel.deleteLot(lot); pending = null },
            )
        }
    }
}

@Composable
private fun ProductHeader(product: Product, state: ProductUiState) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text(product.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                product.brand?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    listOfNotNull(state.categoryName, product.barcode).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val qty = "${state.totalQuantity.formatQuantity()} ${product.unit.label()}"
                Text(
                    when (state.lots.size) {
                        0 -> "Sem estoque"
                        1 -> "$qty em 1 lote"
                        else -> "$qty em ${state.lots.size} lotes"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LotCard(
    lot: Lot,
    product: Product,
    isNext: Boolean,
    onConsume: () -> Unit,
    onDiscard: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${lot.quantity.formatQuantity()} ${product.unit.label()}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (isNext) AssistChip(onClick = onConsume, label = { Text("Usar primeiro") })
                }
                ExpiryBadge(lot.expiryDate)
                Text(
                    listOfNotNull(
                        "Validade ${lot.expiryDate.formatDate(lot.expiryMonthOnly)}",
                        lot.location,
                        "entrada ${lot.addedAt.formatDate()}",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Ações do lote") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Consumir deste lote") }, onClick = { menu = false; onConsume() })
                    DropdownMenuItem(text = { Text("Descartar") }, onClick = { menu = false; onDiscard() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Editar lote") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Excluir lote") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
