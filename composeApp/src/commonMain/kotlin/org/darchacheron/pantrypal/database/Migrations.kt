package org.darchacheron.pantrypal.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE profile ADD COLUMN dataSynchronization TEXT NOT NULL DEFAULT 'NO_SYNCHRONIZATION'")
    }
}
