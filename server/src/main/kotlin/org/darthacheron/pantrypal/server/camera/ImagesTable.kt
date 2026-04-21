package org.darthacheron.pantrypal.server.camera

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object ImagesTable : UuidTable("images") {
    val profileId = uuid("profile_id")
    val foodId = uuid("food_id").nullable()
    val inventoryItemId = uuid("inventory_item_id").nullable()
    val isPrimary = bool("is_primary").default(false)
    val createdAt = timestamp("created_at")
    val lastModifiedAt = timestamp("last_modified_at")
    val deletedAt = timestamp("deleted_at").nullable()
}
