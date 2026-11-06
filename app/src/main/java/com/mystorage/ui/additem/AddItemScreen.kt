package com.mystorage.ui.additem

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mystorage.domain.model.MeasureUnit
import com.mystorage.ui.components.ExpiryBadge
import com.mystorage.ui.components.formatDate
import com.mystorage.ui.components.label
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddItemViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { name ->
            Toast.makeText(context, "Adicionado: $name", Toast.LENGTH_SHORT).show()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.existingProduct != null) "Adicionar ao estoque" else "Novo produto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(),
            ) {
                Text(if (state.isSaving) "Salvando..." else "Salvar")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text("Buscando ${state.barcode}...")
            }
        } else {
            AddItemForm(state, viewModel, Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemForm(state: AddItemUiState, vm: AddItemViewModel, modifier: Modifier = Modifier) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.lookupMessage?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        // ---- Produto ----
        if (state.existingProduct != null) {
            ProductHeader(state)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.imageUrl != null) ProductImage(state.imageUrl)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = vm::onNameChange,
                        label = { Text("Nome *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.brand,
                        onValueChange = vm::onBrandChange,
                        label = { Text("Marca") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            state.barcode?.let { Text("Código: $it", style = MaterialTheme.typography.labelMedium) }
            CategoryPicker(state, vm::onCategoryChange)
            UnitPicker(state.unit, vm::onUnitChange)
        }

        // ---- Lote ----
        Text("Quantidade", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(onClick = { vm.stepQuantity(-1) }) { Text("−", style = MaterialTheme.typography.titleLarge) }
            OutlinedTextField(
                value = state.quantity,
                onValueChange = vm::onQuantityChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(100.dp),
            )
            FilledTonalIconButton(onClick = { vm.stepQuantity(1) }) { Icon(Icons.Filled.Add, contentDescription = "Mais um") }
            Text(state.unit.label())
        }

        Text("Validade *", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Text(
                "  " + (state.expiryDate?.formatDate(state.expiryMonthOnly) ?: "Escolher data"),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = state.expiryMonthOnly, role = Role.Switch, onValueChange = vm::onMonthOnlyChange),
        ) {
            Switch(checked = state.expiryMonthOnly, onCheckedChange = null)
            Text("  Embalagem só tem mês/ano", style = MaterialTheme.typography.bodyMedium)
        }
        state.expiryDate?.let { date ->
            val effective = if (state.expiryMonthOnly) date.withDayOfMonth(date.lengthOfMonth()) else date
            ExpiryBadge(effective)
        }

        OutlinedTextField(
            value = state.location,
            onValueChange = vm::onLocationChange,
            label = { Text("Local (opcional)") },
            placeholder = { Text("Despensa, freezer...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Box(Modifier.size(8.dp))
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.expiryDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            vm.onExpiryChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showDatePicker = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ProductHeader(state: AddItemUiState) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.imageUrl?.let { ProductImage(it) }
            Column {
                Text(state.name, style = MaterialTheme.typography.titleMedium)
                if (state.brand.isNotBlank()) Text(state.brand, style = MaterialTheme.typography.bodyMedium)
                val category = state.categories.firstOrNull { it.id == state.categoryId }?.name
                Text(
                    listOfNotNull(category, state.barcode).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun ProductImage(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
private fun CategoryPicker(state: AddItemUiState, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = state.categories.firstOrNull { it.id == state.categoryId }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.name ?: "Escolher categoria *", modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun UnitPicker(selected: MeasureUnit, onSelect: (MeasureUnit) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MeasureUnit.entries.forEach { unit ->
            FilterChip(selected = unit == selected, onClick = { onSelect(unit) }, label = { Text(unit.label()) })
        }
    }
}
