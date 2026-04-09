package org.darthacheron.pantrypal.server.inventory

import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun InventoryItemDAO.toDto() : InventoryItemDto {
    return InventoryItemDto(
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
        this.createdAt,
        this.lastModifiedAt
    )
}