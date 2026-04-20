package org.darthacheron.pantrypal.server.food

import org.darthacheron.pantrypal.shared.food.FoodDto
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun FoodDAO.toDto() = FoodDto(
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
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt,
    deletedAt = deletedAt
)
