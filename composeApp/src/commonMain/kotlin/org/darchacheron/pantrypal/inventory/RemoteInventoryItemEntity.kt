package org.darchacheron.pantrypal.inventory

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.darchacheron.pantrypal.common.RemoteProduct
import org.darchacheron.pantrypal.profile.RemoteProfileEntity
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "remote_inventory_item",
    primaryKeys = ["localInventoryItemId", "serverUrl"],
    foreignKeys = [
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["localInventoryItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RemoteProfileEntity::class,
            parentColumns = ["localProfileId", "serverUrl"],
            childColumns = ["localProfileId", "serverUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["localInventoryItemId"]),
        Index(value = ["localProfileId", "serverUrl"])
    ]
)
data class RemoteInventoryItemEntity(
    val localInventoryItemId: Uuid,
    val localProfileId: Uuid,
    val serverUrl: String,
    val serverId: Uuid,
    val lastSyncedAt: Instant? = null
) {
    fun toRemoteProduct(): RemoteProduct = RemoteProduct(
        localProductId = localInventoryItemId,
        localProfileId = localProfileId,
        serverUrl = serverUrl,
        serverId = serverId,
        lastSyncedAt = lastSyncedAt
    )
}

@OptIn(ExperimentalUuidApi::class)
fun RemoteProduct.toRemoteInventoryItemEntity(): RemoteInventoryItemEntity = RemoteInventoryItemEntity(
    localInventoryItemId = localProductId,
    localProfileId = localProfileId,
    serverUrl = serverUrl,
    serverId = serverId,
    lastSyncedAt = lastSyncedAt
)
