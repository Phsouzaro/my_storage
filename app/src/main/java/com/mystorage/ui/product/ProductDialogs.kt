package com.mystorage.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mystorage.domain.model.Category
import com.mystorage.domain.model.Lot
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.domain.model.Product
import com.mystorage.ui.components.CategoryPicker
import com.mystorage.ui.components.ExpiryDateField
import com.mystorage.ui.components.UnitPicker
import com.mystorage.ui.components.formatQuantity
import com.mystorage.ui.components.label
import java.time.LocalDate

@Composable
fun EditProductDialog(
    product: Product,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (name: String, brand: String, categoryId: String, unit: MeasureUnit) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(product.name) }
    var brand by rememberSaveable { mutableStateOf(product.brand.orEmpty()) }
    var categoryId by rememberSaveable { mutableStateOf(product.categoryId) }
    var unit by rememberSaveable { mutableStateOf(product.unit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar produto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Nome *") },
                    singleLine = true, isError = name.isBlank(), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = brand, onValueChange = { brand = it }, label = { Text("Marca") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                CategoryPicker(categories, categoryId, onSelect = { categoryId = it })
                UnitPicker(unit, onSelect = { unit = it })
                product.barcode?.let {
                    Text("Código: $it", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, brand, categoryId, unit) }, enabled = name.isNotBlank()) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun EditLotDialog(
    lot: Lot,
    unit: MeasureUnit,
    onDismiss: () -> Unit,
    onSave: (Lot) -> Unit,
) {
    var quantityText by rememberSaveable { mutableStateOf(lot.quantity.formatQuantity()) }
    var expiry by rememberSaveable { mutableStateOf(lot.expiryDate.toEpochDay()) }
    var monthOnly by rememberSaveable { mutableStateOf(lot.expiryMonthOnly) }
    var location by rememberSaveable { mutableStateOf(lot.location.orEmpty()) }

    val quantity = quantityText.replace(',', '.').toDoubleOrNull()
    val valid = quantity != null && quantity > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar lote") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quantidade", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = {
                        quantityText = ((quantity ?: 1.0) - 1).coerceAtLeast(1.0).formatQuantity()
                    }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { v -> quantityText = v.filter { it.isDigit() || it == ',' || it == '.' }.take(8) },
                        singleLine = true,
                        isError = !valid,
                        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(96.dp),
                    )
                    FilledTonalIconButton(onClick = { quantityText = ((quantity ?: 0.0) + 1).formatQuantity() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Mais um")
                    }
                    Text(unit.label())
                }
                Text(
                    "Corrigir a quantidade fica registrado como ajuste no histórico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Validade", style = MaterialTheme.typography.titleSmall)
                ExpiryDateField(
                    date = LocalDate.ofEpochDay(expiry),
                    monthOnly = monthOnly,
                    onDateChange = { expiry = it.toEpochDay() },
                    onMonthOnlyChange = { monthOnly = it },
                )
                OutlinedTextField(
                    value = location, onValueChange = { location = it }, label = { Text("Local (opcional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        lot.copy(
                            quantity = requireNotNull(quantity),
                            expiryDate = LocalDate.ofEpochDay(expiry),
                            expiryMonthOnly = monthOnly,
                            location = location,
                        ),
                    )
                },
                enabled = valid,
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
