package org.darchacheron.pantrypal.inventory

import androidx.room.Embedded
import androidx.room.Relation
import org.darchacheron.pantrypal.camera.ImageEntity
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class InventoryItemWithImages(
    @Embedded val inventoryItem: InventoryItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "inventoryItemId"
    )
    val images: List<ImageEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "localInventoryItemId"
    )
    val remoteInventoryItems: List<RemoteInventoryItemEntity>
) {
    fun toInventoryItem(): InventoryItem {
        val primaryImage = images.find { it.isPrimary }?.toImage(emptyList())
        val additionalImages = images.filter { !it.isPrimary }.map { it.toImage(emptyList()) }
        val remoteProducts = remoteInventoryItems.map { it.toRemoteProduct() }
        return inventoryItem.toInventoryItem(primaryImage, additionalImages, remoteProducts)
    }
}
