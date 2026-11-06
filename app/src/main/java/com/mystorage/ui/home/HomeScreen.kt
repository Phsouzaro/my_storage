package com.mystorage.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.compositeOver
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mystorage.domain.model.ExpiryStatus
import com.mystorage.domain.model.LotWithProduct
import com.mystorage.ui.components.ExpiryBadge
import com.mystorage.ui.components.formatDate
import com.mystorage.ui.components.formatQuantity
import com.mystorage.ui.components.label
import com.mystorage.ui.theme.color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onProductClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Meu Estoque") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Escanear") },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { NotificationPermissionBanner() }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryCard("Vencidos", state.expired.size, ExpiryStatus.EXPIRED, Modifier.weight(1f))
                    SummaryCard("Até 7 dias", state.critical.size, ExpiryStatus.CRITICAL, Modifier.weight(1f))
                    SummaryCard("Até ${state.warningDays} dias", state.warning.size, ExpiryStatus.WARNING, Modifier.weight(1f))
                }
            }

            if (!state.hasAlerts) {
                item {
                    Text(
                        if (state.totalLots == 0) {
                            "Seu estoque está vazio.\nToque em Escanear para começar."
                        } else {
                            "Tudo em dia! Nada vence nos próximos ${state.warningDays} dias."
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }

            section("Vencidos", state.expired, onProductClick)
            section("Vencem em até 7 dias", state.critical, onProductClick)
            section("Vencem em até ${state.warningDays} dias", state.warning, onProductClick)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    lots: List<LotWithProduct>,
    onProductClick: (String) -> Unit,
) {
    if (lots.isEmpty()) return
    item(key = "header-$title") {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
    items(lots, key = { it.lotId }) { lot -> LotRow(lot, onClick = { onProductClick(lot.productId) }) }
}

@Composable
private fun SummaryCard(label: String, count: Int, status: ExpiryStatus, modifier: Modifier = Modifier) {
    val accent = status.color()
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            // Cor sólida (composta sobre a superfície) para a sombra do card não aparecer por baixo.
            containerColor = if (count > 0) {
                accent.copy(alpha = 0.18f).compositeOver(MaterialTheme.colorScheme.surface)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) accent.darken() else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Escurece a cor de status para o número ficar legível sobre o fundo claro do card. */
private fun Color.darken(): Color = Color(red * 0.7f, green * 0.7f, blue * 0.7f, alpha)

@Composable
private fun LotRow(lot: LotWithProduct, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            if (lot.imageUrl != null) {
                AsyncImage(
                    model = lot.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text(lot.productName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium) }
            }
        },
        headlineContent = { Text(lot.productName) },
        supportingContent = { ExpiryBadge(lot.expiryDate) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text("${lot.quantity.formatQuantity()} ${lot.unit.label()}", style = MaterialTheme.typography.titleSmall)
                Text(lot.expiryDate.formatDate(lot.expiryMonthOnly), style = MaterialTheme.typography.labelSmall)
            }
        },
    )
}

/** Android 13+: pede a permissão de notificação; se negada, mostra um aviso com botão. */
@Composable
private fun NotificationPermissionBanner() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (granted || !asked) return

    OutlinedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Notificações desativadas: você não será avisado sobre validades.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Permitir") }
        }
    }
}
