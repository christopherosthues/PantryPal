package org.darchacheron.pantrypal.profile

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.settings.DataSynchronization
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProfileRepository(
    private val profileDao: ProfileDao,
    private val remoteProfileDao: RemoteProfileDao,
    private val profileNetworkService: ProfileNetworkService,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
) {
    private val loggerTag = "ProfileRepository"

    fun getProfileById(id: Uuid): Flow<Profile?> =
        profileDao.getProfileById(id).flatMapLatest { entity ->
            if (entity == null) flowOf(null)
            else remoteProfileDao.getRemoteProfilesByLocalProfileId(id).map { remotes ->
                entity.toProfile().copy(remoteProfiles = remotes.map { it.toRemoteProfile() })
            }
        }

    fun getProfileByUsername(username: String): Flow<Profile?> =
        profileDao.getProfileByUsername(username).flatMapLatest { entity ->
            if (entity == null) flowOf(null)
            else remoteProfileDao.getRemoteProfilesByLocalProfileId(entity.id).map { remotes ->
                entity.toProfile().copy(remoteProfiles = remotes.map { it.toRemoteProfile() })
            }
        }

    fun getProfileByEmail(email: String): Flow<Profile?> =
        profileDao.getProfileByEmail(email).flatMapLatest { entity ->
            if (entity == null) flowOf(null)
            else remoteProfileDao.getRemoteProfilesByLocalProfileId(entity.id).map { remotes ->
                entity.toProfile().copy(remoteProfiles = remotes.map { it.toRemoteProfile() })
            }
        }

    fun getProfileByIdentifier(identifier: String): Flow<Profile?> =
        profileDao.getProfileByIdentifier(identifier).flatMapLatest { entity ->
            if (entity == null) flowOf(null)
            else remoteProfileDao.getRemoteProfilesByLocalProfileId(entity.id).map { remotes ->
                entity.toProfile().copy(remoteProfiles = remotes.map { it.toRemoteProfile() })
            }
        }

    suspend fun upsert(profile: Profile) = withContext(Dispatchers.IO) {
        // Phase 1: Save locally
        profileDao.upsert(profile.toProfileEntity())
        profile.remoteProfiles.forEach {
            remoteProfileDao.upsert(it.toRemoteProfileEntity())
        }

        // Phase 1: Try push immediately
        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val canSync = prefs?.isLoggedInRemotely == true

        if (canSync && (profile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
            profile.dataSynchronization == DataSynchronization.ONLY_UPLOAD
        )) {
            val serverUrl = prefs!!.serverUrl
            val remoteProfile = remoteProfileDao.getRemoteProfile(profile.id, serverUrl).firstOrNull()
            profileNetworkService.updateProfile(profile, serverUrl, remoteProfile?.username, remoteProfile?.email)
                .onSuccess { syncedDto ->
                    syncedDto?.let {
                        remoteProfileDao.upsert(it.toRemoteProfileEntity(profile.id, serverUrl))
                    }
                }
                .onFailure { e ->
                    if (e is ProfileNetworkService.RemoteAccountDeletedException) {
                        _remoteAccountDeleted.emit(true)
                    } else {
                        Logger.withTag(loggerTag).w { "Failed immediate profile sync: ${e.message}" }
                    }
                }
        }
    }

    suspend fun upsertRemoteProfile(remoteProfile: RemoteProfile) = withContext(Dispatchers.IO) {
        remoteProfileDao.upsert(remoteProfile.toRemoteProfileEntity())
    }

    suspend fun deleteLocal() = withContext(Dispatchers.IO) {
        profileDao.delete()
    }

    suspend fun deleteRemote() = withContext(Dispatchers.IO) {
        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val canSync = prefs?.isLoggedInRemotely == true

        if (canSync) {
            profileNetworkService.deleteProfile(prefs.serverUrl, remote = true)
                .onSuccess {
                    val profileId = prefs.localProfileId.let { if (it.isNotBlank()) Uuid.parse(it) else null }
                    val localProfile = profileId?.let { profileDao.getProfileById(it).firstOrNull() }?.toProfile()
                    if (localProfile != null) {
                        profileDao.upsert(localProfile.copy(serverId = null, lastSyncedAt = null).toProfileEntity())
                    }
                }
                .onFailure { e ->
                    Logger.withTag(loggerTag).e { "Remote profile deletion failed: ${e.message}" }
                    throw e
                }
        } else {
            throw Exception("Not logged in to remote server")
        }
    }

    suspend fun delete(remote: Boolean) = withContext(Dispatchers.IO) {
        if (remote) {
            deleteRemote()
        }
        deleteLocal()
    }

    private val _remoteAccountDeleted = MutableSharedFlow<Boolean>()
    val remoteAccountDeleted: Flow<Boolean> = _remoteAccountDeleted

    suspend fun syncWithServer() = withContext(Dispatchers.IO) {
        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val profileId = prefs?.localProfileId?.let { if (it.isNotBlank()) Uuid.parse(it) else null }
        val localProfile = profileId?.let { profileDao.getProfileById(it).firstOrNull() }?.toProfile() ?: return@withContext

        if (localProfile.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) {
            return@withContext
        }

        if (!prefs.isLoggedInRemotely) {
            Logger.withTag(loggerTag).d { "Skipping profile sync: Not logged in to remote" }
            return@withContext
        }

        val serverUrl = prefs.serverUrl
        val remoteProfile = remoteProfileDao.getRemoteProfile(localProfile.id, serverUrl).firstOrNull()

        // 1. Push changes if needed
        if (localProfile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
            localProfile.dataSynchronization == DataSynchronization.ONLY_UPLOAD
        ) {
            if (remoteProfile != null && !localProfile.isLocalOnly) {
                if (localProfile.lastSyncedAt == null || localProfile.lastModifiedAt > localProfile.lastSyncedAt) {
                    profileNetworkService.updateProfile(localProfile, serverUrl, remoteProfile.username, remoteProfile.email)
                        .onSuccess { syncedDto ->
                            syncedDto?.let {
                                remoteProfileDao.upsert(it.toRemoteProfileEntity(localProfile.id, serverUrl))
                            }
                        }
                        .onFailure { e ->
                            handleSyncError(e)
                        }
                }
            }
        }

        // 2. Pull changes
        if (!localProfile.isLocalOnly && (localProfile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                    localProfile.dataSynchronization == DataSynchronization.ONLY_DOWNLOAD)
        ) {
            profileNetworkService.fetchProfile(serverUrl)
                .onSuccess { remoteProfileDto ->
                    remoteProfileDto?.let {
                        remoteProfileDao.upsert(it.toRemoteProfileEntity(localProfile.id, serverUrl))
                    }
                }
                .onFailure { e ->
                    handleSyncError(e)
                }
        }
    }

    private suspend fun handleSyncError(e: Throwable) {
        if (e is ProfileNetworkService.RemoteAccountDeletedException) {
            _remoteAccountDeleted.emit(true)
        } else {
            Logger.withTag(loggerTag).e { "Profile sync failed: ${e.message}" }
        }
    }
}
