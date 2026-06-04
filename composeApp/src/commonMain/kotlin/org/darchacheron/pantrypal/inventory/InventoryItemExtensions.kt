package org.darchacheron.pantrypal.inventory

import org.darchacheron.pantrypal.common.RemoteProduct
import org.darchacheron.pantrypal.camera.toDto as toImageDto
import org.darchacheron.pantrypal.camera.toImage as toImageBO
import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun InventoryItem.toDto(serverUrl: String): InventoryItemDto = InventoryItemDto(
    serverId = getServerId(serverUrl),
    profileId = Uuid.NIL, // Server will fill this from session, but DTO expects non-null
    clientId = id,
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
    fillingQuantity = fillingQuantity,
    isLiquid = isLiquid,
    primaryImage = image?.toImageDto(serverUrl),
    additionalImages = additionalImages.map { it.toImageDto(serverUrl) },
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt,
)

@OptIn(ExperimentalUuidApi::class)
fun InventoryItemDto.toInventoryItem(serverUrl: String): InventoryItem = InventoryItem(
    id = clientId,
    profileId = profileId,
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
    fillingQuantity = fillingQuantity,
    isLiquid = isLiquid,
    image = primaryImage?.toImageBO(serverUrl),
    additionalImages = additionalImages.map { it.toImageBO(serverUrl) },
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt,
    remoteProducts = listOfNotNull(
        serverId?.let {
            RemoteProduct(
                localProductId = clientId,
                localProfileId = profileId,
                serverUrl = serverUrl,
                serverId = it,
                lastSyncedAt = lastModifiedAt
            )
        }
    )
)
