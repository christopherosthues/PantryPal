package org.darchacheron.pantrypal.inventory

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class InventoryItem(
    val id: Uuid = Uuid.generateV7(),
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
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val imagePath: String? = null,
    val additionalImagePaths: List<String> = emptyList(),
)
