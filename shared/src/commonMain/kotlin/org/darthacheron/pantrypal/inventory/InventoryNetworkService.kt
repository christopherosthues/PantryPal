package org.darthacheron.pantrypal.inventory

import org.darthacheron.pantrypal.core.inventory.InventoryItemDto
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface InventoryNetworkService {
    suspend fun fetchAllInventoryItems(serverUrl: String): Result<List<InventoryItemDto>>

    suspend fun fetchInventoryItemById(serverId: Uuid, serverUrl: String): Result<InventoryItemDto?>

    suspend fun createInventoryItem(item: InventoryItemDto, serverUrl: String): Result<InventoryItemDto?>

    suspend fun updateInventoryItem(item: InventoryItemDto, serverUrl: String): Result<InventoryItemDto?>

    suspend fun pushInventoryItems(items: List<InventoryItemDto>, serverUrl: String): Result<List<InventoryItemDto>>

    suspend fun fetchChanges(lastSync: Instant, serverUrl: String): Result<List<InventoryItemDto>>

    suspend fun deleteInventoryItem(serverId: Uuid, serverUrl: String): Result<Boolean>
}

