package org.darthacheron.pantrypal.server.inventory

import kotlin.time.Clock
import org.darthacheron.pantrypal.core.inventory.InventoryItemDto
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryItemRepository {
    fun getInventoryItemById(id: Uuid): InventoryItemDto? = transaction {
        InventoryItemDAO.findById(id)?.toDto()
    }

    fun getAllInventoryItemsByProfileId(profileId: Uuid): List<InventoryItemDto> = transaction {
        InventoryItemDAO.find { (InventoryItemsTable.profileId eq profileId) and (InventoryItemsTable.deletedAt.isNull()) }.map { it.toDto() }
    }

    fun createInventoryItem(inventoryItemDto: InventoryItemDto, profileId: Uuid): InventoryItemDto = transaction {
        InventoryItemDAO.new {
            this.profileId = profileId
            this.clientId = inventoryItemDto.clientId
            this.name = inventoryItemDto.name
            this.kiloCalories = inventoryItemDto.kiloCalories
            this.kiloJoule = inventoryItemDto.kiloJoule
            this.fatInGrams = inventoryItemDto.fatInGrams
            this.saturatedFattyAcidsInGrams = inventoryItemDto.saturatedFattyAcidsInGrams
            this.carbsInGrams = inventoryItemDto.carbsInGrams
            this.sugarInGrams = inventoryItemDto.sugarInGrams
            this.dietaryFiberInGrams = inventoryItemDto.dietaryFiberInGrams
            this.proteinInGrams = inventoryItemDto.proteinInGrams
            this.saltInGrams = inventoryItemDto.saltInGrams
            this.fillingQuantity = inventoryItemDto.fillingQuantity
            this.isLiquid = inventoryItemDto.isLiquid
            this.createdAt = inventoryItemDto.createdAt
            this.lastModifiedAt = inventoryItemDto.lastModifiedAt
        }.toDto()
    }

    fun updateInventoryItem(inventoryItemDto: InventoryItemDto, profileId: Uuid): InventoryItemDto? = transaction {
        val id = inventoryItemDto.serverId ?: return@transaction null
        val existing = InventoryItemDAO.findById(id) ?: return@transaction null
        
        if (existing.profileId != profileId || existing.deletedAt != null) return@transaction null

        existing.apply {
            this.name = inventoryItemDto.name
            this.kiloCalories = inventoryItemDto.kiloCalories
            this.kiloJoule = inventoryItemDto.kiloJoule
            this.fatInGrams = inventoryItemDto.fatInGrams
            this.saturatedFattyAcidsInGrams = inventoryItemDto.saturatedFattyAcidsInGrams
            this.carbsInGrams = inventoryItemDto.carbsInGrams
            this.sugarInGrams = inventoryItemDto.sugarInGrams
            this.dietaryFiberInGrams = inventoryItemDto.dietaryFiberInGrams
            this.proteinInGrams = inventoryItemDto.proteinInGrams
            this.saltInGrams = inventoryItemDto.saltInGrams
            this.fillingQuantity = inventoryItemDto.fillingQuantity
            this.isLiquid = inventoryItemDto.isLiquid
            this.lastModifiedAt = inventoryItemDto.lastModifiedAt
        }.toDto()
    }

    fun deleteInventoryItem(id: Uuid, profileId: Uuid): Boolean = transaction {
        val existing = InventoryItemDAO.findById(id) ?: return@transaction false
        if (existing.profileId != profileId || existing.deletedAt != null) return@transaction false
        existing.deletedAt = Clock.System.now()
        true
    }

    fun deleteAllInventoryItemsForProfile(profileId: Uuid) = transaction {
        InventoryItemDAO.find { (InventoryItemsTable.profileId eq profileId) and (InventoryItemsTable.deletedAt.isNull()) }.forEach {
            it.deletedAt = Clock.System.now()
        }
    }
}
