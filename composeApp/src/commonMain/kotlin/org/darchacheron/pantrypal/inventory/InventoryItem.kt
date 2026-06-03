package org.darchacheron.pantrypal.inventory

import org.darchacheron.pantrypal.camera.Image
import org.darchacheron.pantrypal.common.Product
import org.darchacheron.pantrypal.common.RemoteProduct
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class InventoryItem(
    override val id: Uuid = Uuid.generateV7(),
    override val serverId: Uuid? = null,
    override val serverUrl: String? = null,
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
) : Product(
    id,
    serverId,
    serverUrl,
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
)
