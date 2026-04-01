package org.darthacheron.pantrypal.server.profile

import org.darthacheron.pantrypal.shared.profile.ProfileDto
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object Profiles : UuidTable("profiles") {
    val clientId = uuid("client_id")
    val username = varchar("username", 255)
    val email = varchar("email", 255)
    val passwordHash = varchar("password_hash", 255).nullable()
    val createdAt = timestamp("created_at")
    val lastModifiedAt = timestamp("last_modified_at").nullable()
}

@OptIn(ExperimentalUuidApi::class)
class ProfileEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<ProfileEntity>(Profiles)

    var clientId by Profiles.clientId
    var username by Profiles.username
    var email by Profiles.email
    var passwordHash by Profiles.passwordHash
    var createdAt by Profiles.createdAt
    var lastModifiedAt by Profiles.lastModifiedAt
}

@OptIn(ExperimentalUuidApi::class)
fun ProfileEntity.toDto() : ProfileDto {
    return ProfileDto(
        this.id.value,
        this.clientId,
        this.username,
        this.email,
        this.passwordHash,
        this.createdAt,
        this.lastModifiedAt
    )
}