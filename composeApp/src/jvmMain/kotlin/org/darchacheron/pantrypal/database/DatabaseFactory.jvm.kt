package org.darchacheron.pantrypal.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class PantryPalDatabaseFactory {
    actual fun create(): androidx.room.RoomDatabase.Builder<PantryPalDatabase> {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val appDataDir =
            when {
                os.contains("win") -> File(System.getenv("APPDATA"), "PantryPal")
                os.contains("mac") -> File(userHome, "Library/Application Support/PantryPal")
                else -> File(userHome, ".local/share/PantryPal")
            }

        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }

        val dbFile = File(appDataDir, PantryPalDatabase.DB_NAME)
        return Room.databaseBuilder(dbFile.absolutePath)
    }
}