package com.mystorage.ui.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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
    val items by viewModel.items.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Escanear") },
            )
        },
    ) { padding ->
        val list = items
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Seu estoque está vazio.\nToque em Escanear para adicionar o primeiro produto.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 96.dp),
            ) {
                items(list, key = { it.productId }) { item ->
                    StockRow(item, onClick = { onProductClick(item.productId) })
                    HorizontalDivider()
                }
            }
        }
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
