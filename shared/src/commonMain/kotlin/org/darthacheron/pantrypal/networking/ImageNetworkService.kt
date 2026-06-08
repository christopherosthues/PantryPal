package org.darthacheron.pantrypal.networking

import org.darthacheron.pantrypal.core.camera.ImageDto
import kotlin.uuid.Uuid

interface ImageNetworkService {
    suspend fun uploadFoodImage(
        foodId: Uuid,
        imageData: ByteArray,
        isPrimary: Boolean,
        serverUrl: String
    ): Result<ImageDto>
    suspend fun uploadInventoryImage(
        inventoryItemId: Uuid,
        imageData: ByteArray,
        isPrimary: Boolean,
        serverUrl: String
    ): Result<ImageDto>
    suspend fun deleteFoodImage(foodId: Uuid, imageId: Uuid, serverUrl: String): Result<Boolean>
    suspend fun deleteInventoryImage(inventoryItemId: Uuid, imageId: Uuid, serverUrl: String): Result<Boolean>
}
