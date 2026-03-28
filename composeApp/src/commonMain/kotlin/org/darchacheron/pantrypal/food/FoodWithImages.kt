package org.darchacheron.pantrypal.food

import androidx.room.Embedded
import androidx.room.Relation
import org.darchacheron.pantrypal.camera.ImageEntity
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class FoodWithImages(
    @Embedded val food: FoodEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "foodId"
    )
    val images: List<ImageEntity>
) {
    fun toFood(): Food {
        val primaryImage = images.find { it.isPrimary }?.toImage()
        val additionalImages = images.filter { !it.isPrimary }.map { it.toImage() }
        return food.toFood(primaryImage, additionalImages)
    }
}
