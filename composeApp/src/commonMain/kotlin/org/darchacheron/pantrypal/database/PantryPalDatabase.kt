package org.darchacheron.pantrypal.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Database(
    entities = [
        FoodEntity::class,
        ProfileEntity::class,
        InventoryItemEntity::class,
        ImageEntity::class
    ],
    version = 6
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
                val defaultProfileId = Uuid.generateV7().toString()
                val createdAtNow = Clock.System.now().toString()
                connection.execSQL(
                    "INSERT OR IGNORE INTO `profile` (id, username, email, passwordHash, createdAt) VALUES ('$defaultProfileId', 'Local User', '', NULL, '$createdAtNow')"
                )

                // 3. Add profileId column to food table
                connection.execSQL("ALTER TABLE food ADD COLUMN profileId TEXT NOT NULL DEFAULT '$defaultProfileId'")

                // 4. Duplicate food items based on amount and remove openedAt for copies
                val selectSql = "SELECT id, name, amount, kiloCalories, kiloJoule, fatInGrams, saturatedFattyAcidsInGrams, carbsInGrams, sugarInGrams, dietaryFiberInGrams, proteinInGrams, saltInGrams, fillingQuantity, isLiquid, bestBeforeUsedByDate, isUseBy, createdAt, lastModifiedAt, imagePath, additionalImagePaths, profileId FROM food WHERE amount > 1"
                connection.prepare(selectSql).use { statement ->
                    while (statement.step()) {
                        val id = statement.getText(0)
                        val name = statement.getText(1)
                        val amount = statement.getLong(2)
                        val kiloCalories = if (statement.isNull(3)) "NULL" else statement.getLong(3)
                        val kiloJoule = if (statement.isNull(4)) "NULL" else statement.getLong(4)
                        val fatInGrams = if (statement.isNull(5)) "NULL" else statement.getDouble(5)
                        val saturatedFattyAcidsInGrams = if (statement.isNull(6)) "NULL" else statement.getDouble(6)
                        val carbsInGrams = if (statement.isNull(7)) "NULL" else statement.getDouble(7)
                        val sugarInGrams = if (statement.isNull(8)) "NULL" else statement.getDouble(8)
                        val dietaryFiberInGrams = if (statement.isNull(9)) "NULL" else statement.getDouble(9)
                        val proteinInGrams = if (statement.isNull(10)) "NULL" else statement.getDouble(10)
                        val saltInGrams = if (statement.isNull(11)) "NULL" else statement.getDouble(11)
                        val fillingQuantity = if (statement.isNull(12)) "NULL" else statement.getDouble(12)
                        val isLiquid = statement.getLong(13)
                        val bestBeforeUsedByDate = if (statement.isNull(14)) "NULL" else "'${statement.getText(14)}'"
                        val isUseBy = statement.getLong(15)
                        val createdAtStr = statement.getText(16)
                        val lastModifiedAtStr = statement.getText(17)
                        val imagePath = if (statement.isNull(18)) "NULL" else "'${statement.getText(18)}'"
                        val additionalImagePaths = statement.getText(19)
                        val profileId = statement.getText(20)

                        for (i in 1 until amount) {
                            val newId = Uuid.generateV7().toString()
                            connection.execSQL(
                                """
                                INSERT INTO food (id, name, amount, kiloCalories, kiloJoule, fatInGrams, saturatedFattyAcidsInGrams, carbsInGrams, sugarInGrams, dietaryFiberInGrams, proteinInGrams, saltInGrams, fillingQuantity, isLiquid, bestBeforeUsedByDate, isUseBy, openedAt, createdAt, lastModifiedAt, imagePath, additionalImagePaths, profileId)
                                VALUES ('$newId', '$name', 1, $kiloCalories, $kiloJoule, $fatInGrams, $saturatedFattyAcidsInGrams, $carbsInGrams, $sugarInGrams, $dietaryFiberInGrams, $proteinInGrams, $saltInGrams, $fillingQuantity, $isLiquid, $bestBeforeUsedByDate, $isUseBy, NULL, '$createdAtStr', '$lastModifiedAtStr', $imagePath, '$additionalImagePaths', '$profileId')
                                """.trimIndent()
                            )
                        }
                        // Set the amount of the original row to 1
                        connection.execSQL("UPDATE food SET amount = 1 WHERE id = '$id'")
                    }
                }

                // 5. Create new food table without amount
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `food_new` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `name` TEXT NOT NULL, `kiloCalories` INTEGER, `kiloJoule` INTEGER, `fatInGrams` REAL, `saturatedFattyAcidsInGrams` REAL, `carbsInGrams` REAL, `sugarInGrams` REAL, `dietaryFiberInGrams` REAL, `proteinInGrams` REAL, `saltInGrams` REAL, `fillingQuantity` REAL, `isLiquid` INTEGER NOT NULL, `bestBeforeUsedByDate` TEXT, `isUseBy` INTEGER NOT NULL, `openedAt` TEXT, `createdAt` TEXT NOT NULL, `lastModifiedAt` TEXT NOT NULL, `imagePath` TEXT, `additionalImagePaths` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )

                // 6. Copy data to new table (excluding amount)
                connection.execSQL(
                    """
                    INSERT INTO food_new (id, profileId, name, kiloCalories, kiloJoule, fatInGrams, saturatedFattyAcidsInGrams, carbsInGrams, sugarInGrams, dietaryFiberInGrams, proteinInGrams, saltInGrams, fillingQuantity, isLiquid, bestBeforeUsedByDate, isUseBy, openedAt, createdAt, lastModifiedAt, imagePath, additionalImagePaths)
                    SELECT id, profileId, name, kiloCalories, kiloJoule, fatInGrams, saturatedFattyAcidsInGrams, carbsInGrams, sugarInGrams, dietaryFiberInGrams, proteinInGrams, saltInGrams, fillingQuantity, isLiquid, bestBeforeUsedByDate, isUseBy, openedAt, createdAt, lastModifiedAt, imagePath, additionalImagePaths FROM food
                    """.trimIndent()
                )

                // 7. Drop old table and rename new one
                connection.execSQL("DROP TABLE food")
                connection.execSQL("ALTER TABLE food_new RENAME TO food")

                // 8. Create index for profileId
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_food_profileId` ON `food` (`profileId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("UPDATE profile SET passwordHash = '' WHERE passwordHash IS NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE food ADD COLUMN serverId TEXT")
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inventory_item` (
                        `id` TEXT NOT NULL, 
                        `serverId` TEXT,
                        `profileId` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `kiloCalories` INTEGER, 
                        `kiloJoule` INTEGER, 
                        `fatInGrams` REAL, 
                        `saturatedFattyAcidsInGrams` REAL, 
                        `carbsInGrams` REAL, 
                        `sugarInGrams` REAL, 
                        `dietaryFiberInGrams` REAL, 
                        `proteinInGrams` REAL, 
                        `saltInGrams` REAL, 
                        `fillingQuantity` REAL, 
                        `isLiquid` INTEGER NOT NULL, 
                        `createdAt` TEXT NOT NULL, 
                        `lastModifiedAt` TEXT NOT NULL, 
                        `imagePath` TEXT, 
                        `additionalImagePaths` TEXT NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                    """.trimIndent()
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_item_profileId` ON `inventory_item` (`profileId`)")
            }
        }

        @OptIn(ExperimentalUuidApi::class)
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                val now = Clock.System.now().toString()
                
                // 1. Create images table with profileId and audit timestamps
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `images` (
                        `id` TEXT NOT NULL, 
                        `serverId` TEXT, 
                        `profileId` TEXT NOT NULL,
                        `localPath` TEXT, 
                        `foodId` TEXT, 
                        `inventoryItemId` TEXT, 
                        `isPrimary` INTEGER NOT NULL, 
                        `createdAt` TEXT NOT NULL,
                        `lastModifiedAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`foodId`) REFERENCES `food`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`inventoryItemId`) REFERENCES `inventory_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_images_profileId` ON `images` (`profileId`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_images_foodId` ON `images` (`foodId`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_images_inventoryItemId` ON `images` (`inventoryItemId`)")

                // 2. Migrate existing images
                migrateImages(connection, "food", "foodId", now)
                migrateImages(connection, "inventory_item", "inventoryItemId", now)

                // 3. Create new food table without image columns
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `food_new` (`id` TEXT NOT NULL, `serverId` TEXT, `profileId` TEXT NOT NULL, `name` TEXT NOT NULL, `kiloCalories` INTEGER, `kiloJoule` INTEGER, `fatInGrams` REAL, `saturatedFattyAcidsInGrams` REAL, `carbsInGrams` REAL, `sugarInGrams` REAL, `dietaryFiberInGrams` REAL, `proteinInGrams` REAL, `saltInGrams` REAL, `fillingQuantity` REAL, `isLiquid` INTEGER NOT NULL, `bestBeforeUsedByDate` TEXT, `isUseBy` INTEGER NOT NULL, `openedAt` TEXT, `createdAt` TEXT NOT NULL, `lastModifiedAt` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                connection.execSQL(
                    """
                    INSERT INTO food_new (id, serverId, profileId, name, kiloCalories, kiloJoule, fatInGrams, saturatedFattyAcidsInGrams, carbsInGrams, sugarInGrams, dietaryFiberInGrams, proteinInGrams, saltInGrams, fillingQuantity, isLiquid, bestBeforeUsedByDate, isUseBy, openedAt, createdAt, lastModifiedAt)
                    SELECT id, serverId, profileId, name, kiloCalories, kiloJoule, fatInGrams, saturatedFattyAcidsInGrams, carbsInGrams, sugarInGrams, dietaryFiberInGrams, proteinInGrams, saltInGrams, fillingQuantity, isLiquid, bestBeforeUsedByDate, isUseBy, openedAt, createdAt, lastModifiedAt FROM food
                    """.trimIndent()
                )
                connection.execSQL("DROP TABLE food")
                connection.execSQL("ALTER TABLE food_new RENAME TO food")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_food_profileId` ON `food` (`profileId`)")

                // 4. Create new inventory_item table without image columns
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `inventory_item_new` (`id` TEXT NOT NULL, `serverId` TEXT, `profileId` TEXT NOT NULL, `name` TEXT NOT NULL, `kiloCalories` INTEGER, `kiloJoule` INTEGER, `fatInGrams` REAL, `saturatedFattyAcidsInGrams` REAL, `carbsInGrams` REAL, `sugarInGrams` REAL, `dietaryFiberInGrams` REAL, `proteinInGrams` REAL, `saltInGrams` REAL, `fillingQuantity` REAL, `isLiquid` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `lastModifiedAt` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                connection.execSQL(
                    """
                    INSERT INTO inventory_item_new (id, serverId, profileId, name, kiloCalories, kiloJoule, fatInGrams, saturatedFattyAcidsInGrams, carbsInGrams, sugarInGrams, dietaryFiberInGrams, proteinInGrams, saltInGrams, fillingQuantity, isLiquid, createdAt, lastModifiedAt)
                    SELECT id, serverId, profileId, name, kiloCalories, kiloJoule, fatInGrams, saturatedFattyAcidsInGrams, carbsInGrams, sugarInGrams, dietaryFiberInGrams, proteinInGrams, saltInGrams, fillingQuantity, isLiquid, createdAt, lastModifiedAt FROM inventory_item
                    """.trimIndent()
                )
                connection.execSQL("DROP TABLE inventory_item")
                connection.execSQL("ALTER TABLE inventory_item_new RENAME TO inventory_item")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_item_profileId` ON `inventory_item` (`profileId`)")
            }

            private fun migrateImages(connection: SQLiteConnection, sourceTable: String, foreignKeyColumn: String, now: String) {
                val selectSql = "SELECT id, profileId, imagePath, additionalImagePaths FROM $sourceTable"
                connection.prepare(selectSql).use { statement ->
                    while (statement.step()) {
                        val parentId = statement.getText(0)
                        val profileId = statement.getText(1)
                        val primaryPath = if (statement.isNull(2)) null else statement.getText(2)
                        val additionalPathsStr = statement.getText(3)

                        primaryPath?.let { path ->
                            val imageId = Uuid.generateV7().toString()
                            connection.execSQL(
                                "INSERT INTO images (id, profileId, localPath, $foreignKeyColumn, isPrimary, createdAt, lastModifiedAt) VALUES ('$imageId', '$profileId', '$path', '$parentId', 1, '$now', '$now')"
                            )
                        }

                        try {
                            val cleaned = additionalPathsStr.trim('[', ']', ' ')
                            if (cleaned.isNotEmpty()) {
                                cleaned.split(',').forEach {
                                    val path = it.trim('"', ' ')
                                    if (path.isNotEmpty()) {
                                        val imageId = Uuid.generateV7().toString()
                                        connection.execSQL(
                                            "INSERT INTO images (id, profileId, localPath, $foreignKeyColumn, isPrimary, createdAt, lastModifiedAt) VALUES ('$imageId', '$profileId', '$path', '$parentId', 0, '$now', '$now')"
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Skip if malformed
                        }
                    }
                }
            }
        }
    }
}
