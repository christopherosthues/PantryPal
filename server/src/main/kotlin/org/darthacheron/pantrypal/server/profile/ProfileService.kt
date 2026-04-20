package org.darthacheron.pantrypal.server.profile

import org.darthacheron.pantrypal.shared.profile.ProfileDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ProfileService(private val profileRepository: ProfileRepository) {
    fun getProfileByUsernameOrEmail(username: String): Result<ProfileDto?> {
        try {
            val profile = profileRepository.getProfileByUsernameOrEmail(username)
            return Result.success(profile)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun getProfileById(id: Uuid): Result<ProfileDto?> {
        try {
            val profile = profileRepository.getProfile(id)
            return Result.success(profile)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    fun createProfile(profileDto: ProfileDto): Result<ProfileDto> {
        try {
            val profile = profileRepository.createProfile(profileDto)
            return Result.success(profile)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    fun updateProfile(profileDto: ProfileDto): Result<ProfileDto> {
        try {
            val profile = profileRepository.updateProfile(profileDto)
            return Result.success(profile)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun deleteProfile(id: Uuid): Result<Boolean> {
        try {
            profileRepository.deleteProfile(id)
            return Result.success(true)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }
}