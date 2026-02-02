package org.darchacheron.pantrypal.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.darchacheron.pantrypal.food.FoodEntity

@Database(
    entities = [
        FoodEntity::class
    ],
    version = 1
)
@ConstructedBy(PantryPalDatabaseConstructor::class)
abstract class PantryPalDatabase : RoomDatabase() {

    companion object {
        const val DB_NAME = "pantrypal.db"
    }
}