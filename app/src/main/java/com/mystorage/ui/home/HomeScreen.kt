package com.mystorage.ui.home

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mystorage.ui.components.PlaceholderScreen

@Composable
fun HomeScreen(onScanClick: () -> Unit) {
    PlaceholderScreen(
        title = "Início",
        description = "Aqui vão os alertas: vencidos, vencendo em 7 e em 60 dias.",
    ) {
        Button(onClick = onScanClick) { Text("Escanear produto") }
    }
}
