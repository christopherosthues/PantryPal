package org.darthacheron.pantrypal.server.inventory

import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object InventoryItems : UuidTable("inventory_items") {
    val clientId = uuid("client_id")
    val name = varchar("name", 255)
    val kiloCalories = integer("kilo_calories").nullable()
    val kiloJoule = integer("kilo_joule").nullable()
    val fatInGrams = float("fat_in_grams").nullable()
    val saturatedFattyAcidsInGrams = float("saturated_fatty_acids_in_grams").nullable()
    val carbsInGrams = float("carbs_in_grams").nullable()
    val sugarInGrams = float("sugar_in_grams").nullable()
    val dietaryFiberInGrams = float("dietary_fiber_in_grams").nullable()
    val proteinInGrams = float("protein_in_grams").nullable()
    val saltInGrams = float("salt_in_grams").nullable()
    val fillingQuantity = float("filling_quantity").nullable()
    val isLiquid = bool("is_liquid").default(false)
    val createdAt = timestamp("created_at")
    val lastModifiedAt = timestamp("last_modified_at")
}

@OptIn(ExperimentalUuidApi::class)
class InventoryItemEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<InventoryItemEntity>(InventoryItems)

    val clientId by InventoryItems.clientId
    val name by InventoryItems.name
    val kiloCalories by InventoryItems.kiloCalories
    val kiloJoule by InventoryItems.kiloJoule
    val fatInGrams by InventoryItems.fatInGrams
    val saturatedFattyAcidsInGrams by InventoryItems.saturatedFattyAcidsInGrams
    val carbsInGrams by InventoryItems.carbsInGrams
    val sugarInGrams by InventoryItems.sugarInGrams
    val dietaryFiberInGrams by InventoryItems.dietaryFiberInGrams
    val proteinInGrams by InventoryItems.proteinInGrams
    val saltInGrams by InventoryItems.saltInGrams
    val fillingQuantity by InventoryItems.fillingQuantity
    val isLiquid by InventoryItems.isLiquid
    val createdAt by InventoryItems.createdAt
    val lastModifiedAt by InventoryItems.lastModifiedAt
}

@OptIn(ExperimentalUuidApi::class)
fun InventoryItemEntity.toDto() : InventoryItemDto {
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