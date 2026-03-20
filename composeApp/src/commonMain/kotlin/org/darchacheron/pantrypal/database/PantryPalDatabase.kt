package org.darchacheron.pantrypal.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import org.darchacheron.pantrypal.database.converters.InstantConverter
import org.darchacheron.pantrypal.database.converters.LocalDateConverter
import org.darchacheron.pantrypal.database.converters.StringListConverter
import org.darchacheron.pantrypal.database.converters.UuidConverter
import org.darchacheron.pantrypal.food.FoodDao
import org.darchacheron.pantrypal.food.FoodEntity
import org.darchacheron.pantrypal.profile.ProfileDao
import org.darchacheron.pantrypal.profile.ProfileEntity

@Database(
    entities = [
        FoodEntity::class,
        ProfileEntity::class
    ],
    version = 4
)
@TypeConverters(
    InstantConverter::class,
    LocalDateConverter::class,
    UuidConverter::class,
    StringListConverter::class
)
@ConstructedBy(PantryPalDatabaseConstructor::class)
abstract class PantryPalDatabase : RoomDatabase() {
    abstract val foodDao: FoodDao
    abstract val profileDao: ProfileDao

    companion object {
        const val DB_NAME = "pantrypal.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE food RENAME COLUMN amount TO fillingQuantity")
                connection.execSQL("ALTER TABLE food ADD COLUMN amount INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile` (`id` TEXT NOT NULL, `serverId` TEXT, `username` TEXT NOT NULL, `email` TEXT NOT NULL, `createdAt` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                // Add profileId column to food table
                connection.execSQL("ALTER TABLE food ADD COLUMN profileId TEXT NOT NULL DEFAULT ''")
                // Create index for profileId
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_food_profileId` ON `food` (`profileId`)")
                // Note: Foreign key constraints cannot be added via ALTER TABLE in SQLite. 
                // Full table recreation would be needed for a strict FK constraint if required by Room validation at runtime.
                // However, Room often accepts the schema if the column and index exist.
            }
        }
    }
}
