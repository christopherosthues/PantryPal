package org.darthacheron.pantrypal.server.inventory

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryItemDAO(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<InventoryItemDAO>(InventoryItemsTable)

    val clientId by InventoryItemsTable.clientId
    val name by InventoryItemsTable.name
    val kiloCalories by InventoryItemsTable.kiloCalories
    val kiloJoule by InventoryItemsTable.kiloJoule
    val fatInGrams by InventoryItemsTable.fatInGrams
    val saturatedFattyAcidsInGrams by InventoryItemsTable.saturatedFattyAcidsInGrams
    val carbsInGrams by InventoryItemsTable.carbsInGrams
    val sugarInGrams by InventoryItemsTable.sugarInGrams
    val dietaryFiberInGrams by InventoryItemsTable.dietaryFiberInGrams
    val proteinInGrams by InventoryItemsTable.proteinInGrams
    val saltInGrams by InventoryItemsTable.saltInGrams
    val fillingQuantity by InventoryItemsTable.fillingQuantity
    val isLiquid by InventoryItemsTable.isLiquid
    val createdAt by InventoryItemsTable.createdAt
    val lastModifiedAt by InventoryItemsTable.lastModifiedAt
}

