package org.darthacheron.pantrypal.profile

import org.darthacheron.pantrypal.core.profile.ProfileDto
import kotlin.uuid.Uuid

interface ProfileNetworkService {
    suspend fun fetchProfile(serverUrl: String): Result<ProfileDto?>
    suspend fun updateProfile(
        profile: Profile,
        serverUrl: String,
        serverId: Uuid?,
        usernameOverride: String? = null,
        emailOverride: String? = null
    ): Result<ProfileDto?>
    suspend fun deleteProfile(serverUrl: String, remote: Boolean): Result<Boolean>
}
