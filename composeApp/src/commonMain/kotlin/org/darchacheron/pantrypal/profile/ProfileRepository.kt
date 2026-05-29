package org.darchacheron.pantrypal.profile

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.settings.DataSynchronization
import org.darchacheron.pantrypal.settings.SettingsRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileRepository(
    private val profileDao: ProfileDao,
    private val profileNetworkService: ProfileNetworkService,
    private val settingsRepository: SettingsRepository,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
) {
    private val loggerTag = "ProfileRepository"

    fun getProfileById(id: Uuid): Flow<Profile?> =
        profileDao.getProfileById(id).map { it?.toProfile() }

    fun getProfileByUsername(username: String): Flow<Profile?> =
        profileDao.getProfileByUsername(username).map { it?.toProfile() }

    fun getProfileByEmail(email: String): Flow<Profile?> =
        profileDao.getProfileByEmail(email).map { it?.toProfile() }

    fun getProfileByIdentifier(identifier: String): Flow<Profile?> =
        profileDao.getProfileByIdentifier(identifier).map { it?.toProfile() }

    suspend fun upsert(profile: Profile) = withContext(Dispatchers.IO) {
        // Phase 1: Save locally
        profileDao.upsert(profile.toProfileEntity())

        // Phase 1: Try push immediately
        val settings = settingsRepository.getSettings()
        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val canSync = prefs?.isLoggedInRemotely == true

        if (canSync && (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
            settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD
        )) {
            // TODO: what if the remote profile does not exist yet?
            profileNetworkService.updateProfile(profile, prefs.serverUrl)
                .onSuccess { syncedDto ->
                    syncedDto?.let { profileDao.upsert(it.toProfileEntity(profile)) }
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

    suspend fun deleteLocal() = withContext(Dispatchers.IO) {
        profileDao.delete()
    }

    suspend fun deleteRemote() = withContext(Dispatchers.IO) {
        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val canSync = prefs?.isLoggedInRemotely == true

        if (canSync) {
            profileNetworkService.deleteProfile(prefs.serverUrl, remote = true)
                .onSuccess {
                    val localProfile = profileDao.getProfile().firstOrNull()?.toProfile()
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
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) {
            return@withContext
        }

        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        if (prefs?.isLoggedInRemotely != true) {
            Logger.withTag(loggerTag).d { "Skipping profile sync: Not logged in to remote" }
            return@withContext
        }

        val localProfile = profileDao.getProfile().firstOrNull()?.toProfile() ?: return@withContext

        // 1. Push changes if needed
        if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
            settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD
        ) {
            if (localProfile.serverId != null && !localProfile.isLocalOnly) {
                if (localProfile.lastSyncedAt == null || localProfile.lastModifiedAt > localProfile.lastSyncedAt) {
                    profileNetworkService.updateProfile(localProfile, prefs.serverUrl)
                        .onSuccess { syncedDto ->
                            syncedDto?.let { profileDao.upsert(it.toProfileEntity(localProfile)) }
                        }
                        .onFailure { e ->
                            handleSyncError(e)
                        }
                }
            }
        }

        // 2. Pull changes
        if (!localProfile.isLocalOnly && (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                    settings.dataSynchronization == DataSynchronization.ONLY_DOWNLOAD)
        ) {
            profileNetworkService.fetchProfile(prefs.serverUrl)
                .onSuccess { remoteProfileDto ->
                    remoteProfileDto?.let { profileDao.upsert(it.toProfileEntity(localProfile)) }
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
