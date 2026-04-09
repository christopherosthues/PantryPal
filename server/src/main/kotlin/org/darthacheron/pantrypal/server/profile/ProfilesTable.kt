package org.darthacheron.pantrypal.server.profile

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object ProfilesTable : UuidTable("profiles") {
    val clientId = uuid("client_id")
    val username = varchar("username", 255)
    val email = varchar("email", 255)
    val createdAt = timestamp("created_at")
    val lastModifiedAt = timestamp("last_modified_at").nullable()
}