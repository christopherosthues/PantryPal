package org.darthacheron.pantrypal.server.food

import org.darthacheron.pantrypal.server.camera.ImageDAO
import org.darthacheron.pantrypal.server.camera.ImagesTable
import org.darthacheron.pantrypal.shared.food.FoodDto
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodDAO(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<FoodDAO>(FoodsTable)

    var profileId by FoodsTable.profileId
    var clientId by FoodsTable.clientId
    var name by FoodsTable.name
    var kiloCalories by FoodsTable.kiloCalories
    var kiloJoule by FoodsTable.kiloJoule
    var fatInGrams by FoodsTable.fatInGrams
    var saturatedFattyAcidsInGrams by FoodsTable.saturatedFattyAcidsInGrams
    var carbsInGrams by FoodsTable.carbsInGrams
    var sugarInGrams by FoodsTable.sugarInGrams
    var dietaryFiberInGrams by FoodsTable.dietaryFiberInGrams
    var proteinInGrams by FoodsTable.proteinInGrams
    var saltInGrams by FoodsTable.saltInGrams
    var fillingQuantity by FoodsTable.fillingQuantity
    var isLiquid by FoodsTable.isLiquid
    var bestBeforeUsedByDate by FoodsTable.bestBeforeUsedByDate
    var isUseBy by FoodsTable.isUseBy
    var openedAt by FoodsTable.openedAt
    var createdAt by FoodsTable.createdAt
    var lastModifiedAt by FoodsTable.lastModifiedAt
    var deletedAt by FoodsTable.deletedAt

    // TODO: Move to Extensions!
    fun toDto(): FoodDto {
        val images = ImageDAO.find { (ImagesTable.foodId eq id.value) and (ImagesTable.profileId eq profileId) }.map { it.toDto() }
        val primaryImage = images.find { it.isPrimary }
        val additionalImages = images.filter { !it.isPrimary }

        return FoodDto(
            serverId = id.value,
            profileId = profileId,
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
            bestBeforeUsedByDate = bestBeforeUsedByDate,
            isUseBy = isUseBy,
            openedAt = openedAt,
            primaryImage = primaryImage,
            additionalImages = additionalImages,
            createdAt = createdAt,
            lastModifiedAt = lastModifiedAt,
            deletedAt = deletedAt
        )
    }
}

