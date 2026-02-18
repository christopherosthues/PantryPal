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
    val calories: Int,
    val carbs: Int,
    val fat: Int,
    val protein: Int,
    val weightInGrams: Int,
    val bestBeforeDate: LocalDate?,
    val useByDate: LocalDate?,
    val openedAt: LocalDate?,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
) {
    fun toFood(): Food = Food(
        id = id,
        name = name,
        calories = calories,
        carbs = carbs,
        fat = fat,
        protein = protein,
        weightInGrams = weightInGrams,
        bestBeforeDate = bestBeforeDate,
        useByDate = useByDate,
        openedAt = openedAt,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt
    )
}

@OptIn(ExperimentalUuidApi::class)
fun Food.toFoodEntity(): FoodEntity = FoodEntity(
    id = id,
    name = name,
    calories = calories,
    carbs = carbs,
    fat = fat,
    protein = protein,
    weightInGrams = weightInGrams,
    bestBeforeDate = bestBeforeDate,
    useByDate = useByDate,
    openedAt = openedAt,
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt
)
