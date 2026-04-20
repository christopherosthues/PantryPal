package org.darthacheron.pantrypal.shared.food

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class FoodDto(
    val serverId: Uuid?,
    val profileId: Uuid?,
    val clientId: Uuid,
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
    val lastModifiedAt: Instant,
    val deletedAt: Instant? = null
)