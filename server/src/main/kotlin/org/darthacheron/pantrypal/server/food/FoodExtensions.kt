package org.darthacheron.pantrypal.server.food

import org.darthacheron.pantrypal.shared.food.FoodDto
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun FoodDAO.toDto() : FoodDto {
    return FoodDto(
        this.id.value,
        this.clientId,
        this.name,
        this.kiloCalories,
        this.kiloJoule,
        this.fatInGrams,
        this.saturatedFattyAcidsInGrams,
        this.carbsInGrams,
        this.sugarInGrams,
        this.dietaryFiberInGrams,
        this.proteinInGrams,
        this.saltInGrams,
        this.fillingQuantity,
        this.isLiquid,
        this.bestBeforeUsedByDate,
        this.isUseBy,
        this.openedAt,
        this.createdAt,
        this.lastModifiedAt
    )
}