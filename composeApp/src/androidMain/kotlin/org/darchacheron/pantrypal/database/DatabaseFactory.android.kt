package org.darchacheron.pantrypal.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class PantryPalDatabaseFactory(
    private val context: Context
) {
    actual fun create(): RoomDatabase.Builder<PantryPalDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(PantryPalDatabase.DB_NAME)

        return Room.databaseBuilder(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
}