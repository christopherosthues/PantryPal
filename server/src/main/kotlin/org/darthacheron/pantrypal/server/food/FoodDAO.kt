package org.darthacheron.pantrypal.server.food

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodDAO(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<FoodDAO>(FoodsTable)

    val clientId by FoodsTable.clientId
    val name by FoodsTable.name
    val kiloCalories by FoodsTable.kiloCalories
    val kiloJoule by FoodsTable.kiloJoule
    val fatInGrams by FoodsTable.fatInGrams
    val saturatedFattyAcidsInGrams by FoodsTable.saturatedFattyAcidsInGrams
    val carbsInGrams by FoodsTable.carbsInGrams
    val sugarInGrams by FoodsTable.sugarInGrams
    val dietaryFiberInGrams by FoodsTable.dietaryFiberInGrams
    val proteinInGrams by FoodsTable.proteinInGrams
    val saltInGrams by FoodsTable.saltInGrams
    val fillingQuantity by FoodsTable.fillingQuantity
    val isLiquid by FoodsTable.isLiquid
    val bestBeforeUsedByDate by FoodsTable.bestBeforeUsedByDate
    val isUseBy by FoodsTable.isUseBy
    val openedAt by FoodsTable.openedAt
    val createdAt by FoodsTable.createdAt
    val lastModifiedAt by FoodsTable.lastModifiedAt
}

