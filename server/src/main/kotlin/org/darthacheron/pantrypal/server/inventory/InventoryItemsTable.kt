package org.darthacheron.pantrypal.server.inventory

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object InventoryItemsTable : UuidTable("inventory_items") {
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
    val profileId = uuid("profile_id")
    val createdAt = timestamp("created_at")
    val lastModifiedAt = timestamp("last_modified_at")
    val deletedAt = timestamp("deleted_at").nullable()
}