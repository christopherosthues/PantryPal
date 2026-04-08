package org.darthacheron.pantrypal.server.profile

import org.darthacheron.pantrypal.shared.profile.ProfileDto
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileService {
    fun createProfile(profileDto: ProfileDto): ProfileDto {
        return transaction {
            val profile = ProfileEntity.new {
                clientId = profileDto.clientId
                username = profileDto.username
                email = profileDto.email
                passwordHash = profileDto.passwordHash
                createdAt = profileDto.createdAt
                lastModifiedAt = profileDto.lastModifiedAt
            }

            profile.toDto()
        }
    }

    fun updateProfile(profileDto: ProfileDto): ProfileDto {
        if (profileDto.serverId == null) {
            throw IllegalArgumentException("Profile ID cannot be null")
        }

        return transaction {
            val profile =
                ProfileEntity.findById(profileDto.serverId!!) ?: throw IllegalArgumentException("Profile not found")
            profile.createdAt = profileDto.createdAt
            profile.lastModifiedAt = profileDto.lastModifiedAt
            profile.clientId = profileDto.clientId
            profile.username = profileDto.username
            profile.email = profileDto.email
            profile.passwordHash = profileDto.passwordHash
            profile.toDto()
        }
    }

    fun getProfile(id: Uuid): ProfileDto? {
        return ProfileEntity.findById(id)?.toDto()
    }

    fun deleteProfile(id: Uuid) {
        transaction {
            ProfileEntity.findById(id)?.delete()
        }
    }
}
