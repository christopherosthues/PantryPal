package org.darthacheron.pantrypal.server.profile

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object ProfilesTable : UuidTable("profiles") {
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val createdAt = timestamp("created_at")
    val lastModifiedAt = timestamp("last_modified_at").nullable()
}