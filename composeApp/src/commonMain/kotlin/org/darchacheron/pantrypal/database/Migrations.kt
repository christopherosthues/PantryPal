package org.darchacheron.pantrypal.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE profile ADD COLUMN dataSynchronization TEXT NOT NULL DEFAULT 'NO_SYNCHRONIZATION'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `remote_profile` (
                `localProfileId` TEXT NOT NULL, 
                `serverUrl` TEXT NOT NULL, 
                `serverId` TEXT NOT NULL, 
                `username` TEXT NOT NULL, 
                `email` TEXT NOT NULL, 
                `lastSyncedAt` TEXT, 
                PRIMARY KEY(`localProfileId`, `serverUrl`), 
                FOREIGN KEY(`localProfileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_remote_profile_localProfileId` ON `remote_profile` (`localProfileId`)")
    }
}
