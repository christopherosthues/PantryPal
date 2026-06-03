package org.darchacheron.pantrypal.food

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import org.darchacheron.pantrypal.camera.Image
import org.darchacheron.pantrypal.common.RemoteProduct
import org.darchacheron.pantrypal.profile.ProfileEntity
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "food",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = false) val id: Uuid = Uuid.generateV7(),
    val serverId: Uuid?,
    val serverUrl: String?,
    val profileId: Uuid,
    val name: String,
    val kiloCalories: Int?,
    val kiloJoule: Int?,
    val fatInGrams: Float?,
    val saturatedFattyAcidsInGrams: Float?,
    val carbsInGrams: Float?,
    val sugarInGrams: Float?,
    val dietaryFiberInGrams: Float?,
    val proteinInGrams: Float?,
    val saltInGrams: Float?,
    val fillingQuantity: Float?,
    val isLiquid: Boolean = false,
    val bestBeforeUsedByDate: LocalDate?,
    val isUseBy: Boolean,
    val openedAt: LocalDate?,
    val createdAt: Instant,
    val lastModifiedAt: Instant
) {
    fun toFood(image: Image?, additionalImages: List<Image>, remoteProducts: List<RemoteProduct>): Food = Food(
        id = id,
        serverId = serverId,
        serverUrl = serverUrl,
        profileId = profileId,
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
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        image = image,
        additionalImages = additionalImages,
        remoteProducts = remoteProducts
    )
}

@OptIn(ExperimentalUuidApi::class)
fun Food.toFoodEntity(): FoodEntity = FoodEntity(
    id = id,
    serverId = serverId,
    serverUrl = serverUrl,
    profileId = profileId,
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
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt
)
