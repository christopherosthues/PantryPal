package org.darchacheron.pantrypal.camera

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.darchacheron.pantrypal.food.FoodEntity
import org.darchacheron.pantrypal.inventory.InventoryItemEntity
import org.darchacheron.pantrypal.profile.ProfileEntity
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["foodId"]),
        Index(value = ["inventoryItemId"])
    ]
)
data class ImageEntity(
    @PrimaryKey val id: Uuid = Uuid.generateV7(),
    val serverId: Uuid?,
    val profileId: Uuid,
    val localPath: String?,
    val foodId: Uuid? = null,
    val inventoryItemId: Uuid? = null,
    val isPrimary: Boolean = false,
    val createdAt: Instant,
    val lastModifiedAt: Instant
) {
    fun toImage(): Image = Image(
        id = id,
        serverId = serverId,
        profileId = profileId,
        localPath = localPath,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt
    )
}

@OptIn(ExperimentalUuidApi::class)
fun Image.toImageEntity(
    foodId: Uuid? = null,
    inventoryItemId: Uuid? = null,
    isPrimary: Boolean = false
): ImageEntity = ImageEntity(
    id = id,
    serverId = serverId,
    profileId = profileId,
    localPath = localPath,
    foodId = foodId,
    inventoryItemId = inventoryItemId,
    isPrimary = isPrimary,
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt
)
