package org.darthacheron.pantrypal.database

import androidx.room.RoomDatabase

expect class PantryPalDatabaseFactory {
    fun create(): RoomDatabase.Builder<PantryPalDatabase>
}