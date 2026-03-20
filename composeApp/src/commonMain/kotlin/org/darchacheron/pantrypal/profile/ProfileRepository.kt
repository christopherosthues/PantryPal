package org.darchacheron.pantrypal.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ProfileRepository(
    private val profileDao: ProfileDao
) {
    fun getProfile(): Flow<Profile?> =
        profileDao.getProfile().map { it?.toProfile() }

    suspend fun upsert(profile: Profile) =
        profileDao.upsert(profile.toProfileEntity())

    suspend fun delete() =
        profileDao.delete()

    fun getCurrentInstant() = Clock.System.now()
}
