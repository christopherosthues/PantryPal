package org.darchacheron.pantrypal.food

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Food(
    val id: Uuid = Uuid.generateV7(),
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
    val isOverdue: Boolean
        get() = bestBeforeUsedByDate != null && bestBeforeUsedByDate < Clock.System.todayIn(TimeZone.currentSystemDefault())

    val isOpened: Boolean
        get() = openedAt != null
}
