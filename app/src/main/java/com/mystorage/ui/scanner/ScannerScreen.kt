package com.mystorage.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Câmera em tela cheia. Ao ler um código chama [onBarcode] uma única vez;
 * ao voltar para esta tela (modo contínuo) a leitura é reativada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBarcode: (String) -> Unit,
    onWithoutBarcode: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var torch by rememberSaveable { mutableStateOf(false) }
    var showManualEntry by rememberSaveable { mutableStateOf(false) }
    // Trava para não disparar várias navegações com o mesmo código.
    var handled by remember { mutableStateOf(false) }

    val handleCode: (String) -> Unit = { code ->
        if (!handled) {
            handled = true
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onBarcode(code)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear produto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (hasPermission) {
                        TextButton(onClick = { torch = !torch }) { Text(if (torch) "Lanterna: ON" else "Lanterna") }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (hasPermission) {
                    CameraPreview(torchEnabled = torch, onBarcode = handleCode, modifier = Modifier.fillMaxSize())
                    // Mira
                    Box(
                        Modifier
                            .fillMaxWidth(0.8f)
                            .height(160.dp)
                            .border(BorderStroke(3.dp, Color.White.copy(alpha = 0.85f)), RoundedCornerShape(16.dp)),
                    )
                    Text(
                        "Aponte para o código de barras",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                    )
                } else {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Preciso da câmera para ler os códigos de barras.",
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Permitir câmera")
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(onClick = { showManualEntry = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text("  Digitar código")
                }
                FilledTonalButton(onClick = onWithoutBarcode, modifier = Modifier.weight(1f)) {
                    Text("Sem código")
                }
            }
        }
    }

    if (showManualEntry) {
        ManualBarcodeDialog(
            onDismiss = { showManualEntry = false },
            onConfirm = { code ->
                showManualEntry = false
                handleCode(code)
            },
        )
    }
}

@Composable
private fun ManualBarcodeDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val valid = code.length in 8..14
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Digitar código") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { new -> code = new.filter(Char::isDigit).take(14) },
                label = { Text("Código de barras") },
                supportingText = { Text("8 a 14 dígitos") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(code) }, enabled = valid) { Text("Buscar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
