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
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Database(
    entities = [
        FoodEntity::class,
        ProfileEntity::class
    ],
    version = 3
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

        @OptIn(ExperimentalUuidApi::class)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                // 1. Create profile table with all columns
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile` (`id` TEXT NOT NULL, `serverId` TEXT, `username` TEXT NOT NULL, `email` TEXT NOT NULL, `passwordHash` TEXT, `createdAt` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )

                // 2. Ensure a default profile exists to associate legacy data with
                val defaultProfileId = Uuid.generateV7()
                val createdAt = Clock.System.now()
                connection.execSQL(
                    "INSERT OR IGNORE INTO `profile` (id, username, email, passwordHash, createdAt) VALUES ('$defaultProfileId', 'Local User', '', NULL, '$createdAt')"
                )

                // 3. Add profileId column to food table
                connection.execSQL("ALTER TABLE food ADD COLUMN profileId TEXT NOT NULL DEFAULT '$defaultProfileId'")

                // 4. Create index for profileId
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_food_profileId` ON `food` (`profileId`)")
            }
        }
    }
}
