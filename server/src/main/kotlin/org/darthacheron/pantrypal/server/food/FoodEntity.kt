package org.darthacheron.pantrypal.server.food

import org.darthacheron.pantrypal.shared.food.FoodDto
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object Foods : UuidTable("foods") {
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

@OptIn(ExperimentalUuidApi::class)
class FoodEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<FoodEntity>(Foods)

    val clientId by Foods.clientId
    val name by Foods.name
    val kiloCalories by Foods.kiloCalories
    val kiloJoule by Foods.kiloJoule
    val fatInGrams by Foods.fatInGrams
    val saturatedFattyAcidsInGrams by Foods.saturatedFattyAcidsInGrams
    val carbsInGrams by Foods.carbsInGrams
    val sugarInGrams by Foods.sugarInGrams
    val dietaryFiberInGrams by Foods.dietaryFiberInGrams
    val proteinInGrams by Foods.proteinInGrams
    val saltInGrams by Foods.saltInGrams
    val fillingQuantity by Foods.fillingQuantity
    val isLiquid by Foods.isLiquid
    val bestBeforeUsedByDate by Foods.bestBeforeUsedByDate
    val isUseBy by Foods.isUseBy
    val openedAt by Foods.openedAt
    val createdAt by Foods.createdAt
    val lastModifiedAt by Foods.lastModifiedAt
}

@OptIn(ExperimentalUuidApi::class)
fun FoodEntity.toDto() : FoodDto {
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