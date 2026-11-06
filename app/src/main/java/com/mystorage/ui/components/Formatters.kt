package com.mystorage.ui.components

import com.mystorage.domain.model.MeasureUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val fullDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val monthYear = DateTimeFormatter.ofPattern("MM/yyyy")

fun LocalDate.formatDate(monthOnly: Boolean = false): String =
    format(if (monthOnly) monthYear else fullDate)

/** 2.0 → "2", 1.5 → "1,5" */
fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) toLong().toString()
    else "%.3f".format(this).trimEnd('0').trimEnd('.', ',').replace('.', ',')

fun MeasureUnit.label(): String = when (this) {
    MeasureUnit.UN -> "un"
    MeasureUnit.KG -> "kg"
    MeasureUnit.G -> "g"
    MeasureUnit.L -> "L"
    MeasureUnit.ML -> "ml"
}
