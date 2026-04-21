package org.darthacheron.pantrypal.server.camera

import org.darthacheron.pantrypal.shared.camera.ImageDto
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ImageDAO(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<ImageDAO>(ImagesTable)

    var profileId by ImagesTable.profileId
    var foodId by ImagesTable.foodId
    var inventoryItemId by ImagesTable.inventoryItemId
    var isPrimary by ImagesTable.isPrimary
    var createdAt by ImagesTable.createdAt
    var lastModifiedAt by ImagesTable.lastModifiedAt
    var deletedAt by ImagesTable.deletedAt

    fun toDto(): ImageDto = ImageDto(
        serverId = id.value,
        profileId = profileId,
        foodId = foodId,
        inventoryItemId = inventoryItemId,
        isPrimary = isPrimary,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        deletedAt = deletedAt
    )
}
