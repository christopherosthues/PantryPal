package org.darthacheron.pantrypal.server.food

import org.darthacheron.pantrypal.shared.food.FoodDto
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodRepository {
    fun getFoodById(id: Uuid): FoodDto? = transaction {
        FoodDAO.findById(id)?.toDto()
    }

    fun getAllFoodByProfileId(profileId: Uuid): List<FoodDto> = transaction {
        FoodDAO.find { FoodsTable.profileId eq profileId }.map { it.toDto() }
    }

    fun createFood(foodDto: FoodDto, profileId: Uuid): FoodDto = transaction {
        FoodDAO.new {
            this.profileId = profileId
            this.clientId = foodDto.clientId
            this.name = foodDto.name
            this.kiloCalories = foodDto.kiloCalories
            this.kiloJoule = foodDto.kiloJoule
            this.fatInGrams = foodDto.fatInGrams
            this.saturatedFattyAcidsInGrams = foodDto.saturatedFattyAcidsInGrams
            this.carbsInGrams = foodDto.carbsInGrams
            this.sugarInGrams = foodDto.sugarInGrams
            this.dietaryFiberInGrams = foodDto.dietaryFiberInGrams
            this.proteinInGrams = foodDto.proteinInGrams
            this.saltInGrams = foodDto.saltInGrams
            this.fillingQuantity = foodDto.fillingQuantity
            this.isLiquid = foodDto.isLiquid
            this.bestBeforeUsedByDate = foodDto.bestBeforeUsedByDate
            this.isUseBy = foodDto.isUseBy
            this.openedAt = foodDto.openedAt
            this.createdAt = foodDto.createdAt
            this.lastModifiedAt = foodDto.lastModifiedAt
        }.toDto()
    }

    fun updateFood(foodDto: FoodDto, profileId: Uuid): FoodDto? = transaction {
        val id = foodDto.serverId ?: return@transaction null
        val existing = FoodDAO.findById(id) ?: return@transaction null
        
        if (existing.profileId != profileId) return@transaction null

        existing.apply {
            this.name = foodDto.name
            this.kiloCalories = foodDto.kiloCalories
            this.kiloJoule = foodDto.kiloJoule
            this.fatInGrams = foodDto.fatInGrams
            this.saturatedFattyAcidsInGrams = foodDto.saturatedFattyAcidsInGrams
            this.carbsInGrams = foodDto.carbsInGrams
            this.sugarInGrams = foodDto.sugarInGrams
            this.dietaryFiberInGrams = foodDto.dietaryFiberInGrams
            this.proteinInGrams = foodDto.proteinInGrams
            this.saltInGrams = foodDto.saltInGrams
            this.fillingQuantity = foodDto.fillingQuantity
            this.isLiquid = foodDto.isLiquid
            this.bestBeforeUsedByDate = foodDto.bestBeforeUsedByDate
            this.isUseBy = foodDto.isUseBy
            this.openedAt = foodDto.openedAt
            this.lastModifiedAt = foodDto.lastModifiedAt
        }.toDto()
    }

    fun deleteFood(id: Uuid, profileId: Uuid): Boolean = transaction {
        val existing = FoodDAO.findById(id) ?: return@transaction false
        if (existing.profileId != profileId) return@transaction false
        existing.delete()
        true
    }
}
