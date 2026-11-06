package com.mystorage.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object StockRoute
@Serializable data object SettingsRoute
@Serializable data object ScannerRoute
@Serializable data class ProductRoute(val productId: String)
