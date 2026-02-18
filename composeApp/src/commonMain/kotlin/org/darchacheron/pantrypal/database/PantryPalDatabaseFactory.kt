package org.darchacheron.pantrypal.database

import androidx.room.RoomDatabase

expect class PantryPalDatabaseFactory {
    fun create(): RoomDatabase.Builder<PantryPalDatabase>
}