package org.darthacheron.pantrypal.inventory

import org.darthacheron.pantrypal.core.inventory.InventoryItemDto
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface InventoryNetworkService {
    suspend fun pushInventoryItems(items: List<InventoryItemDto>, serverUrl: String): List<InventoryItemDto>

    suspend fun fetchChanges(lastSync: Instant, serverUrl: String): List<InventoryItemDto>

    suspend fun deleteInventoryItem(serverId: Uuid, serverUrl: String)
}

