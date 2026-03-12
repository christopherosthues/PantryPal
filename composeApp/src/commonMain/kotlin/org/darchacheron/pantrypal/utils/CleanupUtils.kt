package org.darchacheron.pantrypal.utils

import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

object CleanupUtils {
    private const val LOGGER_TAG = "CleanupUtils"

    fun cleanupOcrDirectory(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                val ocrDir = FileKit.filesDir.path.toPath() / "PantryPal" / "OCR"
                if (FileSystem.SYSTEM.exists(ocrDir)) {
                    val files = FileSystem.SYSTEM.list(ocrDir)
                    files.forEach { file ->
                        FileSystem.SYSTEM.delete(file)
                        Logger.withTag(LOGGER_TAG).i { "Deleted old OCR file: $file" }
                    }
                }
            } catch (e: Exception) {
                Logger.withTag(LOGGER_TAG).e(e) { "Failed to cleanup OCR directory" }
            }
        }
    }
}
