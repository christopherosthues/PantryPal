package org.darthacheron.pantrypal.server.profile

import org.darthacheron.pantrypal.shared.profile.ProfileDto
import org.jetbrains.exposed.v1.core.eq
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
            }

            // Return DTO with the original clientId for mapping
            profile.toDto().copy(clientId = profileDto.clientId)
        }
    }

    fun updateProfile(profileDto: ProfileDto): ProfileDto {
        if (profileDto.serverId == null) {
            throw IllegalArgumentException("Profile ID cannot be null")
        }

        return transaction {
            val profile =
                ProfileDAO.findById(profileDto.serverId!!) ?: throw IllegalArgumentException("Profile not found")
            profile.createdAt = profileDto.createdAt
            profile.lastModifiedAt = profileDto.lastModifiedAt
            profile.username = profileDto.username
            profile.email = profileDto.email
            profile.toDto().copy(clientId = profileDto.clientId)
        }
    }

    fun getProfile(id: Uuid): ProfileDto? {
        return transaction {
            ProfileDAO.findById(id)?.toDto()
        }
    }

    fun getProfileByUsernameOrEmail(identifier: String): ProfileDto? {
        return transaction {
            ProfileDAO.find { (ProfilesTable.username eq identifier) or (ProfilesTable.email eq identifier) }
                .firstOrNull()
                ?.toDto()
        }
    }

    fun deleteProfile(id: Uuid) {
        transaction {
            ProfileDAO.findById(id)?.delete()
        }
    }
}
