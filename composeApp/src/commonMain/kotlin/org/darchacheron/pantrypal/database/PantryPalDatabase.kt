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

@Database(
    entities = [
        FoodEntity::class
    ],
    version = 2
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

    companion object {
        const val DB_NAME = "pantrypal.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE food RENAME COLUMN amount TO fillingQuantity")
                connection.execSQL("ALTER TABLE food ADD COLUMN amount INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
