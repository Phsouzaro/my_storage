package com.mystorage.ui.scanner

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mystorage.ui.components.PlaceholderScreen

@Composable
fun ScannerScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Scanner",
        description = "CameraX + ML Kit: leitura do código, lookup e formulário rápido.",
    ) {
        OutlinedButton(onClick = onBack) { Text("Voltar") }
    }
}
