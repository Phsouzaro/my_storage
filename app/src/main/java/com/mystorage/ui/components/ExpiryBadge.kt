package com.mystorage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mystorage.domain.model.daysUntilExpiry
import com.mystorage.domain.model.expiryStatusOf
import com.mystorage.ui.theme.color
import java.time.LocalDate

fun expiryText(expiryDate: LocalDate, today: LocalDate): String {
    val days = daysUntilExpiry(expiryDate, today)
    return when {
        days < -1 -> "Vencido há ${-days} dias"
        days == -1L -> "Venceu ontem"
        days == 0L -> "Vence hoje"
        days == 1L -> "Vence amanhã"
        else -> "Vence em $days dias"
    }
}

@Composable
fun ExpiryBadge(expiryDate: LocalDate, modifier: Modifier = Modifier, today: LocalDate = LocalDate.now()) {
    val status = expiryStatusOf(expiryDate, today)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(status.color()))
        Text(expiryText(expiryDate, today), style = MaterialTheme.typography.labelMedium)
    }
}
