package org.darchacheron.pantrypal.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider

actual fun inMemoryDatabaseFactory(): RoomDatabase.Builder<PantryPalDatabase> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(
        context = context,
        klass = PantryPalDatabase::class.java
    ).setDriver(BundledSQLiteDriver())
}
