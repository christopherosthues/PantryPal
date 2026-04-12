package org.darchacheron.pantrypal.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.darchacheron.pantrypal.camera.ImageEntity
import org.darchacheron.pantrypal.database.converters.InstantConverter
import org.darchacheron.pantrypal.database.converters.LocalDateConverter
import org.darchacheron.pantrypal.database.converters.StringListConverter
import org.darchacheron.pantrypal.database.converters.UuidConverter
import org.darchacheron.pantrypal.food.FoodDao
import org.darchacheron.pantrypal.food.FoodEntity
import org.darchacheron.pantrypal.inventory.InventoryItemDao
import org.darchacheron.pantrypal.inventory.InventoryItemEntity
import org.darchacheron.pantrypal.profile.ProfileDao
import org.darchacheron.pantrypal.profile.ProfileEntity

@Database(
    entities = [
        FoodEntity::class,
        ProfileEntity::class,
        InventoryItemEntity::class,
        ImageEntity::class
    ],
    version = 1
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
    abstract val inventoryItemDao: InventoryItemDao

    companion object {
        const val DB_NAME = "pantrypal.db"
    }
}
