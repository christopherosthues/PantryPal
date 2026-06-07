package org.darthacheron.pantrypal.server.food

import org.darthacheron.pantrypal.server.camera.ImageDAO
import org.darthacheron.pantrypal.server.camera.ImagesTable
import org.darthacheron.pantrypal.core.food.FoodDto
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun FoodDAO.toDto(): FoodDto {
    val images = ImageDAO.find {
        (ImagesTable.foodId eq id.value) and (ImagesTable.profileId eq profileId) and (ImagesTable.deletedAt.isNull())
    }.map { it.toDto() }
    val primaryImage = images.find { it.isPrimary }
    val additionalImages = images.filter { !it.isPrimary }

    return FoodDto(
        serverId = id.value,
        profileId = profileId,
        clientId = clientId,
        name = name,
        kiloCalories = kiloCalories,
        kiloJoule = kiloJoule,
        fatInGrams = fatInGrams,
        saturatedFattyAcidsInGrams = saturatedFattyAcidsInGrams,
        carbsInGrams = carbsInGrams,
        sugarInGrams = sugarInGrams,
        dietaryFiberInGrams = dietaryFiberInGrams,
        proteinInGrams = proteinInGrams,
        saltInGrams = saltInGrams,
        fillingQuantity = fillingQuantity,
        isLiquid = isLiquid,
        bestBeforeUsedByDate = bestBeforeUsedByDate,
        isUseBy = isUseBy,
        openedAt = openedAt,
        primaryImage = primaryImage,
        additionalImages = additionalImages,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        deletedAt = deletedAt
    )
}
