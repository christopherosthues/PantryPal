package org.darthacheron.pantrypal.server.inventory

import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun InventoryItemDAO.toDto() : InventoryItemDto {
    return InventoryItemDto(
        serverId = this.id.value,
        profileId = this.profileId,
        clientId = this.clientId,
        name = this.name,
        kiloCalories = this.kiloCalories,
        kiloJoule = this.kiloJoule,
        fatInGrams = this.fatInGrams,
        saturatedFattyAcidsInGrams = this.saturatedFattyAcidsInGrams,
        carbsInGrams = this.carbsInGrams,
        sugarInGrams = this.sugarInGrams,
        dietaryFiberInGrams = this.dietaryFiberInGrams,
        proteinInGrams = this.proteinInGrams,
        saltInGrams = this.saltInGrams,
        fillingQuantity = this.fillingQuantity,
        isLiquid = this.isLiquid,
        createdAt = this.createdAt,
        lastModifiedAt = this.lastModifiedAt,
        deletedAt = this.deletedAt
    )
}