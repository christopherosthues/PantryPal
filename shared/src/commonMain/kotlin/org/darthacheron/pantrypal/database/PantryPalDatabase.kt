package org.darthacheron.pantrypal.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.darthacheron.pantrypal.camera.ImageEntity
import org.darthacheron.pantrypal.camera.RemoteImageDao
import org.darthacheron.pantrypal.camera.RemoteImageEntity
import org.darthacheron.pantrypal.database.converters.DataSynchronizationConverter
import org.darthacheron.pantrypal.database.converters.InstantConverter
import org.darthacheron.pantrypal.database.converters.LocalDateConverter
import org.darthacheron.pantrypal.database.converters.StringListConverter
import org.darthacheron.pantrypal.database.converters.UuidConverter
import org.darthacheron.pantrypal.food.FoodDao
import org.darthacheron.pantrypal.food.FoodEntity
import org.darthacheron.pantrypal.food.RemoteFoodDao
import org.darthacheron.pantrypal.food.RemoteFoodEntity
import org.darthacheron.pantrypal.inventory.InventoryItemDao
import org.darthacheron.pantrypal.inventory.InventoryItemEntity
import org.darthacheron.pantrypal.inventory.RemoteInventoryItemDao
import org.darthacheron.pantrypal.inventory.RemoteInventoryItemEntity
import org.darthacheron.pantrypal.profile.ProfileDao
import org.darthacheron.pantrypal.profile.ProfileEntity
import org.darthacheron.pantrypal.profile.RemoteProfileDao
import org.darthacheron.pantrypal.profile.RemoteProfileEntity

@Database(
    entities = [
        FoodEntity::class,
        ProfileEntity::class,
        InventoryItemEntity::class,
        ImageEntity::class,
        RemoteProfileEntity::class,
        RemoteFoodEntity::class,
        RemoteInventoryItemEntity::class,
        RemoteImageEntity::class
    ],
    version = 4
)
@TypeConverters(
    InstantConverter::class,
    LocalDateConverter::class,
    UuidConverter::class,
    StringListConverter::class,
    DataSynchronizationConverter::class
)
@ConstructedBy(PantryPalDatabaseConstructor::class)
abstract class PantryPalDatabase : RoomDatabase() {
    abstract val foodDao: FoodDao
    abstract val remoteFoodDao: RemoteFoodDao
    abstract val profileDao: ProfileDao
    abstract val remoteProfileDao: RemoteProfileDao
    abstract val inventoryItemDao: InventoryItemDao
    abstract val remoteInventoryItemDao: RemoteInventoryItemDao
    abstract val remoteImageDao: RemoteImageDao

    companion object {
        const val DB_NAME = "pantrypal.db"
    }
}
