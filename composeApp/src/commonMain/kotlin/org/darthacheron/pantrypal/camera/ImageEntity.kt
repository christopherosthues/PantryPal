package org.darthacheron.pantrypal.camera

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.darthacheron.pantrypal.food.FoodEntity
import org.darthacheron.pantrypal.inventory.InventoryItemEntity
import org.darthacheron.pantrypal.profile.ProfileEntity
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
    val profileId: Uuid,
    val localPath: String?,
    val foodId: Uuid? = null,
    val inventoryItemId: Uuid? = null,
    val isPrimary: Boolean = false,
    val createdAt: Instant,
    val lastModifiedAt: Instant
) {
    fun toImage(remoteImages: List<RemoteImage>): Image = Image(
        id = id,
        profileId = profileId,
        localPath = localPath,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        remoteImages = remoteImages
    )
}

@OptIn(ExperimentalUuidApi::class)
fun Image.toImageEntity(
    foodId: Uuid? = null,
    inventoryItemId: Uuid? = null,
    isPrimary: Boolean = false
): ImageEntity = ImageEntity(
    id = id,
    profileId = profileId,
    localPath = localPath,
    foodId = foodId,
    inventoryItemId = inventoryItemId,
    isPrimary = isPrimary,
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt
)
