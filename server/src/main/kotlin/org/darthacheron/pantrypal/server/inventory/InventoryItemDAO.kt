package org.darthacheron.pantrypal.server.inventory

import org.darthacheron.pantrypal.server.camera.ImageDAO
import org.darthacheron.pantrypal.server.camera.ImagesTable
import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryItemDAO(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<InventoryItemDAO>(InventoryItemsTable)

    var clientId by InventoryItemsTable.clientId
    var name by InventoryItemsTable.name
    var kiloCalories by InventoryItemsTable.kiloCalories
    var kiloJoule by InventoryItemsTable.kiloJoule
    var fatInGrams by InventoryItemsTable.fatInGrams
    var saturatedFattyAcidsInGrams by InventoryItemsTable.saturatedFattyAcidsInGrams
    var carbsInGrams by InventoryItemsTable.carbsInGrams
    var sugarInGrams by InventoryItemsTable.sugarInGrams
    var dietaryFiberInGrams by InventoryItemsTable.dietaryFiberInGrams
    var proteinInGrams by InventoryItemsTable.proteinInGrams
    var saltInGrams by InventoryItemsTable.saltInGrams
    var fillingQuantity by InventoryItemsTable.fillingQuantity
    var isLiquid by InventoryItemsTable.isLiquid
    var profileId by InventoryItemsTable.profileId
    var createdAt by InventoryItemsTable.createdAt
    var lastModifiedAt by InventoryItemsTable.lastModifiedAt
    var deletedAt by InventoryItemsTable.deletedAt

    // TODO: Use toDto in the Extensions!
    fun toDto(): InventoryItemDto {
        val images = ImageDAO.find { (ImagesTable.inventoryItemId eq id.value) and (ImagesTable.profileId eq profileId) }.map { it.toDto() }
        val primaryImage = images.find { it.isPrimary }
        val additionalImages = images.filter { !it.isPrimary }

        return InventoryItemDto(
            serverId = id.value,
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
            profileId = profileId,
            primaryImage = primaryImage,
            additionalImages = additionalImages,
            createdAt = createdAt,
            lastModifiedAt = lastModifiedAt,
            deletedAt = deletedAt
        )
    }
}

