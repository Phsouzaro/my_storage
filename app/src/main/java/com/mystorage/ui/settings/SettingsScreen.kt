package com.mystorage.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mystorage.domain.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirmImport by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFrom)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajustes") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val s = settings ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Alertas de validade", style = MaterialTheme.typography.titleMedium)

            Text("Avisar quando faltar", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppSettings.WARNING_DAY_OPTIONS.forEach { days ->
                    FilterChip(
                        selected = s.warningDays == days,
                        onClick = { viewModel.setWarningDays(days) },
                        label = { Text("$days dias") },
                    )
                }
            }
            Text(
                "Você também recebe avisos aos 30 e aos 7 dias, e quando o produto vence.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Horário da verificação diária", style = MaterialTheme.typography.bodyMedium)
            HourPicker(selected = s.notifyHour, onSelect = viewModel::setNotifyHour)

            FilledTonalButton(
                onClick = {
                    viewModel.checkNow()
                    Toast.makeText(context, "Verificando validades...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null)
                Text("  Verificar agora")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("Organização", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onOpenCategories, modifier = Modifier.fillMaxWidth()) {
                Text("Gerenciar categorias")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("Backup", style = MaterialTheme.typography.titleMedium)
            Text(
                "O backup automático do Android já guarda seus dados no Google Drive da conta do aparelho. " +
                    "Para ter uma cópia sob seu controle, exporte um arquivo — no seletor, escolha \"Drive\" " +
                    "para salvar direto no Google Drive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { exportLauncher.launch(viewModel.suggestedBackupName()) },
                    enabled = !backupBusy,
                    modifier = Modifier.weight(1f),
                ) { Text("Exportar backup") }
                OutlinedButton(
                    onClick = { confirmImport = true },
                    enabled = !backupBusy,
                    modifier = Modifier.weight(1f),
                ) { Text("Importar backup") }
            }
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Importar backup?") },
            text = {
                Text(
                    "Os dados atuais (produtos, lotes e histórico) serão substituídos pelos do arquivo. " +
                        "Se quiser guardar o estado atual, exporte um backup antes.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    // Drive/Downloads às vezes não marcam .json como application/json.
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }) { Text("Escolher arquivo") }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun HourPicker(selected: Int, onSelect: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("%02d:00".format(selected))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (6..22).forEach { hour ->
                DropdownMenuItem(
                    text = { Text("%02d:00".format(hour)) },
                    onClick = {
                        onSelect(hour)
                        expanded = false
                    },
                )
            }
        }
    }
}
