package org.darthacheron.pantrypal.core.inventory

import kotlinx.serialization.Serializable
import org.darthacheron.pantrypal.core.camera.ImageDto
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class InventoryItemDto(
    val serverId: Uuid?,
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
    val profileId: Uuid,
    val primaryImage: ImageDto? = null,
    val additionalImages: List<ImageDto> = emptyList(),
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val deletedAt: Instant? = null
)
