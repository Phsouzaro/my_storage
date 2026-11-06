package com.mystorage.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import com.mystorage.domain.model.MeasureUnit

/** Diálogo de quantidade com +/− e limite máximo (saldo disponível). */
@Composable
fun QuantityDialog(
    title: String,
    confirmLabel: String,
    max: Double,
    unit: MeasureUnit,
    initial: Double = 1.0,
    message: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(minOf(initial, max).formatQuantity()) }
    val value = text.replace(',', '.').toDoubleOrNull()
    val valid = value != null && value > 0 && value <= max + 1e-9

    fun step(delta: Int) {
        val next = ((value ?: 0.0) + delta).coerceIn(minOf(1.0, max), max)
        text = next.formatQuantity()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = { step(-1) }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { it.isDigit() || it == ',' || it == '.' }.take(8) },
                        singleLine = true,
                        isError = !valid,
                        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(96.dp),
                    )
                    FilledTonalIconButton(onClick = { step(1) }) { Icon(Icons.Filled.Add, contentDescription = "Mais um") }
                }
                Text("Disponível: ${max.formatQuantity()} ${unit.label()}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
