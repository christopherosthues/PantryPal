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
    val images: List<ImageEntity>
) {
    fun toInventoryItem(): InventoryItem {
        val primaryImage = images.find { it.isPrimary }?.toImage()
        val additionalImages = images.filter { !it.isPrimary }.map { it.toImage() }
        return inventoryItem.toInventoryItem(primaryImage, additionalImages)
    }
}
