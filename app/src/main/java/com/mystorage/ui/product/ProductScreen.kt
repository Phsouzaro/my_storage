package com.mystorage.ui.product

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mystorage.ui.components.PlaceholderScreen

@Composable
fun ProductScreen(productId: String, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Produto",
        description = "Detalhes e lotes do produto $productId.",
    ) {
        OutlinedButton(onClick = onBack) { Text("Voltar") }
    }
}
