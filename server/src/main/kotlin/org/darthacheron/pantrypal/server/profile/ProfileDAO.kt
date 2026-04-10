package org.darthacheron.pantrypal.server.profile

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileDAO(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<ProfileDAO>(ProfilesTable)

    var username by ProfilesTable.username
    var email by ProfilesTable.email
    var createdAt by ProfilesTable.createdAt
    var lastModifiedAt by ProfilesTable.lastModifiedAt
    var lastSyncedAt by ProfilesTable.lastSyncedAt
    var deletedAt by ProfilesTable.deletedAt
}
