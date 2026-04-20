package org.darthacheron.pantrypal.server.food

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object FoodsTable : UuidTable("foods") {
    val profileId = uuid("profile_id")
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
    val bestBeforeUsedByDate = date("best_before_used_by_date").nullable()
    val isUseBy = bool("is_use_by")
    val openedAt = date("opened_at").nullable()
    val createdAt = timestamp("created_at")
    val lastModifiedAt = timestamp("last_modified_at")
}