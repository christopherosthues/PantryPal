package org.darchacheron.pantrypal.database

import androidx.room.RoomDatabase

expect fun inMemoryDatabaseFactory(): RoomDatabase.Builder<PantryPalDatabase>

fun createInMemoryRoomDatabase(): PantryPalDatabase = inMemoryDatabaseFactory().build()