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
    @PrimaryKey(autoGenerate = true) val id: Uuid = Uuid.generateV7(),
    val name: String,
    val calories: Int,
    val carbs: Int,
    val fat: Int,
    val protein: Int,
    val weightInGrams: Int,
    val bestBeforeDate: LocalDate?,
    val useByDate: LocalDate?,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
)