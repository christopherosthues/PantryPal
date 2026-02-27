package org.darchacheron.pantrypal.food

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(tableName = "food")
data class FoodEntity(
    @PrimaryKey(autoGenerate = false) val id: Uuid = Uuid.generateV7(),
    val name: String,
    val kiloCalories: Int?,
    val kiloJoule: Int?,
    val fatInGrams: Float?,
    val saturatedFattyAcidsInGrams: String?,
    val carbsInGrams: Int?,
    val sugarInGrams: Float?,
    val dietaryFiberInGrams: Float?,
    val proteinInGrams: Float?,
    val saltInGrams: Float?,
    val weightInGrams: Float?,
    val bestBeforeUsedByDate: LocalDate?,
    val isUseBy: Boolean,
    val openedAt: LocalDate?,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val imagePath: String? = null,
) {
    fun toFood(): Food = Food(
        id = id,
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
        weightInGrams = weightInGrams,
        bestBeforeUsedByDate = bestBeforeUsedByDate,
        isUseBy = isUseBy,
        openedAt = openedAt,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        imagePath = imagePath
    )
}

@OptIn(ExperimentalUuidApi::class)
fun Food.toFoodEntity(): FoodEntity = FoodEntity(
    id = id,
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
    weightInGrams = weightInGrams,
    bestBeforeUsedByDate = bestBeforeUsedByDate,
    isUseBy = isUseBy,
    openedAt = openedAt,
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt,
    imagePath = imagePath
)
