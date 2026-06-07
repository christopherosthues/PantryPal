package org.darthacheron.pantrypal.database

import androidx.room.RoomDatabaseConstructor

expect object PantryPalDatabaseConstructor : RoomDatabaseConstructor<PantryPalDatabase> {
    override fun initialize(): PantryPalDatabase
}