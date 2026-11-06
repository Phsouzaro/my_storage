package com.mystorage.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lê/grava o arquivo de backup num Uri escolhido pelo usuário no seletor do sistema (SAF).
 * O seletor já oferece Google Drive, Downloads etc. — sem OAuth nem API do Drive.
 */
@Singleton
class BackupFileManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val MAX_BYTES = 20L * 1024 * 1024 // 20 MB — muito acima de qualquer estoque doméstico

        fun suggestedFileName(today: LocalDate = LocalDate.now()) = "my_storage_backup_$today.json"
    }

    suspend fun write(uri: Uri, content: String) = withContext(Dispatchers.IO) {
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Não foi possível abrir o arquivo")
        out.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    }

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Não foi possível abrir o arquivo")
        // Leitura com limite (readNBytes só existe a partir do Android 13; minSdk é 26).
        input.use { stream ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_BYTES) throw IOException("Arquivo grande demais para ser um backup")
            }
            out.toString(Charsets.UTF_8.name())
        }
    }
}
