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
    val images: List<ImageEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "localFoodId"
    )
    val remoteFoods: List<RemoteFoodEntity>
) {
    fun toFood(): Food {
        // Here we would also need to fetch remote images if we want full detail, 
        // but for now let's focus on food remote mapping.
        // Actually, to fully map Image, each ImageEntity needs its RemoteImageEntities.
        // Room doesn't easily support nested relations in a single data class without another wrapper.
        // For now, let's just pass empty list for remoteImages in toImage() or handle it elsewhere.
        
        val primaryImage = images.find { it.isPrimary }?.toImage(emptyList())
        val additionalImages = images.filter { !it.isPrimary }.map { it.toImage(emptyList()) }
        val remoteProducts = remoteFoods.map { it.toRemoteProduct() }
        return food.toFood(primaryImage, additionalImages, remoteProducts)
    }
}
