package org.darchacheron.pantrypal.food

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.darchacheron.pantrypal.camera.Image
import org.darchacheron.pantrypal.common.Product
import org.darchacheron.pantrypal.common.RemoteProduct
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Food(
    override val id: Uuid = Uuid.generateV7(),
    override val profileId: Uuid,
    override val name: String,
    override val kiloCalories: Int?,
    override val kiloJoule: Int?,
    override val fatInGrams: Float?,
    override val saturatedFattyAcidsInGrams: Float?,
    override val carbsInGrams: Float?,
    override val sugarInGrams: Float?,
    override val dietaryFiberInGrams: Float?,
    override val proteinInGrams: Float?,
    override val saltInGrams: Float?,
    override val fillingQuantity: Float?,
    override val isLiquid: Boolean = false,
    override val createdAt: Instant,
    override val lastModifiedAt: Instant,
    override val image: Image? = null,
    override val additionalImages: List<Image> = emptyList(),
    override val remoteProducts: List<RemoteProduct> = emptyList(),
    val bestBeforeUsedByDate: LocalDate?,
    val isUseBy: Boolean,
    val openedAt: LocalDate?,
) : Product(
    id,
    profileId,
    name,
    kiloCalories,
    kiloJoule,
    fatInGrams,
    saturatedFattyAcidsInGrams,
    carbsInGrams,
    sugarInGrams,
    dietaryFiberInGrams,
    proteinInGrams,
    saltInGrams,
    fillingQuantity,
    isLiquid,
    createdAt,
    lastModifiedAt,
    image,
    additionalImages,
    remoteProducts,
) {
    val isOverdue: Boolean
        get() = bestBeforeUsedByDate != null && bestBeforeUsedByDate < Clock.System.todayIn(TimeZone.currentSystemDefault())

    val isOpened: Boolean
        get() = openedAt != null
}
