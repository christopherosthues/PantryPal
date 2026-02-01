package org.darchacheron.pantrypal.database

import androidx.room.RoomDatabaseConstructor

expect object PantryPalDatabaseConstructor : RoomDatabaseConstructor<PantryPalDatabase> {
    override fun initialize(): PantryPalDatabase
}