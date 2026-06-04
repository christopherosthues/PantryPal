package org.darchacheron.pantrypal.food

import org.darchacheron.pantrypal.common.RemoteProduct
import org.darchacheron.pantrypal.camera.toDto as toImageDto
import org.darchacheron.pantrypal.camera.toImage as toImageBO
import org.darthacheron.pantrypal.shared.food.FoodDto
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Food.toDto(serverUrl: String): FoodDto = FoodDto(
    serverId = getServerId(serverUrl),
    profileId = null, // Server will fill this from session
    clientId = id,
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
    primaryImage = image?.toImageDto(serverUrl),
    additionalImages = additionalImages.map { it.toImageDto(serverUrl) },
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt,
)

@OptIn(ExperimentalUuidApi::class)
fun FoodDto.toFood(serverUrl: String): Food = Food(
    id = clientId,
    profileId = profileId ?: throw IllegalArgumentException("profileId must not be null when converting from DTO"),
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
    image = primaryImage?.toImageBO(serverUrl),
    additionalImages = additionalImages.map { it.toImageBO(serverUrl) },
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt,
    remoteProducts = listOfNotNull(
        serverId?.let {
            RemoteProduct(
                localProductId = clientId,
                localProfileId = profileId ?: throw IllegalArgumentException("profileId must not be null when converting from DTO"),
                serverUrl = serverUrl,
                serverId = it,
                lastSyncedAt = lastModifiedAt
            )
        }
    )
)
