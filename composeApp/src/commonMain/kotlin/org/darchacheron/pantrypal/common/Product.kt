package org.darchacheron.pantrypal.common

import org.darchacheron.pantrypal.camera.Image
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
open class Product(
    open val id: Uuid = Uuid.generateV7(),
    open val serverId: Uuid? = null,
    open val profileId: Uuid,
    open val name: String,
    open val kiloCalories: Int?,
    open val kiloJoule: Int?,
    open val fatInGrams: Float?,
    open val saturatedFattyAcidsInGrams: Float?,
    open val carbsInGrams: Float?,
    open val sugarInGrams: Float?,
    open val dietaryFiberInGrams: Float?,
    open val proteinInGrams: Float?,
    open val saltInGrams: Float?,
    open val fillingQuantity: Float?,
    open val isLiquid: Boolean = false,
    open val createdAt: Instant,
    open val lastModifiedAt: Instant,
    open val image: Image? = null,
    open val additionalImages: List<Image> = emptyList(),
)
