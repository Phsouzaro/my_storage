package com.mystorage.ui.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Lê códigos de produto de varejo (EAN-13/EAN-8/UPC) com ML Kit on-device.
 * Exige a mesma leitura em [requiredHits] frames seguidos para evitar leituras parciais erradas.
 */
class BarcodeAnalyzer(
    private val requiredHits: Int = 2,
    private val onBarcode: (String) -> Unit,
) : ImageAnalysis.Analyzer, AutoCloseable {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build(),
    )

    private var lastValue: String? = null
    private var hits = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                if (value == null) {
                    hits = 0
                    lastValue = null
                } else {
                    hits = if (value == lastValue) hits + 1 else 1
                    lastValue = value
                    if (hits >= requiredHits) {
                        hits = 0
                        onBarcode(value)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun close() = scanner.close()
}
