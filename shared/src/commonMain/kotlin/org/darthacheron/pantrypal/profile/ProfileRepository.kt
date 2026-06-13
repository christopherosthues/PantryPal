package org.darthacheron.pantrypal.profile

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface ProfileRepository {
    val remoteAccountDeleted: Flow<Boolean>
    fun getProfileById(id: Uuid): Flow<Profile?>
    fun getProfileByUsername(username: String): Flow<Profile?>
    fun getProfileByEmail(email: String): Flow<Profile?>
    fun getProfileByIdentifier(identifier: String): Flow<Profile?>

    suspend fun upsert(profile: Profile): Result<Unit>

    suspend fun upsertRemoteProfile(remoteProfile: RemoteProfile): Result<Unit>

    suspend fun deleteLocal(): Result<Unit>

    suspend fun deleteRemote(): Result<Boolean>

    suspend fun delete(remote: Boolean): Result<Unit>

    suspend fun unlinkRemote(localProfileId: Uuid, serverUrl: String): Result<Unit>

    suspend fun syncWithServer(): Result<Unit>
}