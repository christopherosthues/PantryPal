package org.darthacheron.pantrypal.server.profile

import org.darthacheron.pantrypal.shared.profile.ProfileDto
import java.time.Clock
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileRepository {
    fun createProfile(profileDto: ProfileDto): ProfileDto {
        return transaction {
            val profile = ProfileDAO.new {
                username = profileDto.username
                email = profileDto.email
                createdAt = profileDto.createdAt
                lastModifiedAt = profileDto.lastModifiedAt
                lastSyncedAt = profileDto.lastSyncedAt
                deletedAt = profileDto.deletedAt
            }

            // Return DTO with the original clientId for mapping
            profile.toDto(clientId = profileDto.clientId)
        }
    }

    fun updateProfile(profileDto: ProfileDto): ProfileDto {
        val serverId = profileDto.serverId ?: throw IllegalArgumentException("Profile ID cannot be null")

        return transaction {
            val profile =
                ProfileDAO.findById(serverId) ?: throw IllegalArgumentException("Profile not found")
            profile.createdAt = profileDto.createdAt
            profile.lastModifiedAt = profileDto.lastModifiedAt
            profile.lastSyncedAt = profileDto.lastSyncedAt
            profile.deletedAt = profileDto.deletedAt
            profile.username = profileDto.username
            profile.email = profileDto.email
            profile.toDto(clientId = profileDto.clientId)
        }
    }

    fun getProfile(id: Uuid): ProfileDto? {
        return transaction {
            ProfileDAO.findById(id)?.toDto()
        }
    }

    fun getProfileByUsernameOrEmail(identifier: String): ProfileDto? {
        return transaction {
            ProfileDAO.find {
                ((ProfilesTable.username eq identifier) or (ProfilesTable.email eq identifier)) and
                (ProfilesTable.deletedAt.isNull())
            }
                .firstOrNull()
                ?.toDto()
        }
    }

    fun deleteProfile(id: Uuid) {
        transaction {
            ProfileDAO.findById(id)?.apply {
                deletedAt = kotlin.time.Instant.fromEpochMilliseconds(Clock.systemUTC().millis())
            }
        }
    }
}
