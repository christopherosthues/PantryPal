package org.darthacheron.pantrypal.server.camera

import kotlin.time.Clock
import org.darthacheron.pantrypal.shared.camera.ImageDto
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ImageRepository {
    fun getImageById(id: Uuid): ImageDto? = transaction {
        ImageDAO.findById(id)?.toDto()
    }

    fun getImagesForFood(foodId: Uuid, profileId: Uuid): List<ImageDto> = transaction {
        ImageDAO.find {
            (ImagesTable.foodId eq foodId) and (ImagesTable.profileId eq profileId) and (ImagesTable.deletedAt.isNull())
        }.map { it.toDto() }
    }

    fun getImagesForInventoryItem(inventoryItemId: Uuid, profileId: Uuid): List<ImageDto> = transaction {
        ImageDAO.find {
            (ImagesTable.inventoryItemId eq inventoryItemId) and (ImagesTable.profileId eq profileId) and (ImagesTable.deletedAt.isNull())
        }.map { it.toDto() }
    }

    fun createImage(
        profileId: Uuid,
        foodId: Uuid?,
        inventoryItemId: Uuid?,
        isPrimary: Boolean
    ): ImageDto = transaction {
        if (isPrimary) {
            if (foodId != null) {
                ImageDAO.find {
                    (ImagesTable.foodId eq foodId) and (ImagesTable.isPrimary eq true) and (ImagesTable.deletedAt.isNull())
                }.forEach { it.isPrimary = false }
            } else if (inventoryItemId != null) {
                ImageDAO.find {
                    (ImagesTable.inventoryItemId eq inventoryItemId) and (ImagesTable.isPrimary eq true) and (ImagesTable.deletedAt.isNull())
                }.forEach { it.isPrimary = false }
            }
        }

        ImageDAO.new {
            this.profileId = profileId
            this.foodId = foodId
            this.inventoryItemId = inventoryItemId
            this.isPrimary = isPrimary
            this.createdAt = Clock.System.now()
            this.lastModifiedAt = Clock.System.now()
        }.toDto()
    }

    fun deleteImage(id: Uuid, profileId: Uuid): Boolean = transaction {
        val existing = ImageDAO.findById(id) ?: return@transaction false
        if (existing.profileId != profileId) return@transaction false
        if (existing.deletedAt != null) return@transaction false
        
        existing.deletedAt = Clock.System.now()
        existing.lastModifiedAt = Clock.System.now()
        true
    }
}
