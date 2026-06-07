package org.darthacheron.pantrypal.food

import androidx.room.Embedded
import androidx.room.Relation
import org.darthacheron.pantrypal.camera.ImageEntity
import org.darthacheron.pantrypal.camera.ImageWithRemotes
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class FoodWithImages(
    @Embedded val food: FoodEntity,
    @Relation(
        entity = ImageEntity::class,
        parentColumn = "id",
        entityColumn = "foodId"
    )
    val images: List<ImageWithRemotes>,
    @Relation(
        parentColumn = "id",
        entityColumn = "localFoodId"
    )
    val remoteFoods: List<RemoteFoodEntity>
) {
    fun toFood(): Food {
        val primaryImage = images.find { it.image.isPrimary }?.toImage()
        val additionalImages = images.filter { !it.image.isPrimary }.map { it.toImage() }
        val remoteProducts = remoteFoods.map { it.toRemoteProduct() }
        return food.toFood(primaryImage, additionalImages, remoteProducts)
    }
}
