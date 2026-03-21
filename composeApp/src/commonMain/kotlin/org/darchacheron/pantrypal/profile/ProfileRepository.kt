package org.darchacheron.pantrypal.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileRepository(
    private val profileDao: ProfileDao
) {
    fun getProfileById(id: Uuid): Flow<Profile?> =
        profileDao.getProfileById(id).map { it?.toProfile() }

    fun getProfileByServerId(serverId: Uuid): Flow<Profile?> =
        profileDao.getProfileByServerId(serverId).map { it?.toProfile() }

    fun getProfileByUsername(username: String): Flow<Profile?> =
        profileDao.getProfileByUsername(username).map { it?.toProfile() }

    suspend fun upsert(profile: Profile) =
        profileDao.upsert(profile.toProfileEntity())

    suspend fun delete() =
        profileDao.delete()
}
