package org.darthacheron.pantrypal.inventory

import androidx.room.Embedded
import androidx.room.Relation
import org.darthacheron.pantrypal.camera.ImageEntity
import org.darthacheron.pantrypal.camera.ImageWithRemotes
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class InventoryItemWithImages(
    @Embedded val inventoryItem: InventoryItemEntity,
    @Relation(
        entity = ImageEntity::class,
        parentColumn = "id",
        entityColumn = "inventoryItemId"
    )
    val images: List<ImageWithRemotes>,
    @Relation(
        parentColumn = "id",
        entityColumn = "localInventoryItemId"
    )
    val remoteInventoryItems: List<RemoteInventoryItemEntity>
) {
    fun toInventoryItem(): InventoryItem {
        val primaryImage = images.find { it.image.isPrimary }?.toImage()
        val additionalImages = images.filter { !it.image.isPrimary }.map { it.toImage() }
        val remoteProducts = remoteInventoryItems.map { it.toRemoteProduct() }
        return inventoryItem.toInventoryItem(primaryImage, additionalImages, remoteProducts)
    }
}
