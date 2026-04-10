package org.darchacheron.pantrypal.profile

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.darchacheron.pantrypal.settings.DataSynchronization
import org.darchacheron.pantrypal.settings.SettingsRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileRepository(
    private val profileDao: ProfileDao,
    private val profileNetworkService: ProfileNetworkService,
    private val settingsRepository: SettingsRepository,
) {
    private val loggerTag = "ProfileRepository"

    fun getProfileById(id: Uuid): Flow<Profile?> =
        profileDao.getProfileById(id).map { it?.toProfile() }

    fun getProfileByServerId(serverId: Uuid): Flow<Profile?> =
        profileDao.getProfileByServerId(serverId).map { it?.toProfile() }

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
        if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
            settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD
        ) {
            try {
                // TODO: what if the remote profile does not exist yet?
                profileNetworkService.updateProfile(profile, settings.serverUrl)?.let { synced ->
                    profileDao.upsert(synced.toProfileEntity())
                }
            } catch (e: ProfileNetworkService.RemoteAccountDeletedException) {
                _remoteAccountDeleted.emit(true)
            } catch (e: Exception) {
                Logger.withTag(loggerTag).w { "Failed immediate profile sync: ${e.message}" }
            }
        }
    }

    suspend fun delete(remote: Boolean) = withContext(Dispatchers.IO) {
        if (remote) {
            val settings = settingsRepository.getSettings()
            try {
                profileNetworkService.deleteProfile(settings.serverUrl, remote = true)
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Remote profile deletion failed: ${e.message}" }
                // Optionally rethrow or handle if local deletion should be blocked
            }
        }
        profileDao.delete()
    }

    private val _remoteAccountDeleted = kotlinx.coroutines.flow.MutableSharedFlow<Boolean>()
    val remoteAccountDeleted: Flow<Boolean> = _remoteAccountDeleted

    suspend fun syncWithServer() = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) return@withContext

        try {
            val localProfile = profileDao.getProfile().firstOrNull()?.toProfile() ?: return@withContext

            // 1. Push changes if needed
            if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD
            ) {
                if (localProfile.serverId == null || localProfile.isLocalOnly) {
                    // Skip sync for local-only profiles
                } else if (localProfile.lastSyncedAt == null || localProfile.lastModifiedAt > localProfile.lastSyncedAt) {
                    profileNetworkService.updateProfile(localProfile, settings.serverUrl)?.let { synced ->
                        profileDao.upsert(synced.toProfileEntity())
                    }
                }
            }

            // 2. Pull changes
            if (!localProfile.isLocalOnly && (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                settings.dataSynchronization == DataSynchronization.ONLY_DOWNLOAD)
            ) {
                profileNetworkService.fetchProfile(settings.serverUrl)?.let { remoteProfile ->
                    profileDao.upsert(remoteProfile.toProfileEntity())
                }
            }
        } catch (e: ProfileNetworkService.RemoteAccountDeletedException) {
            _remoteAccountDeleted.emit(true)
        } catch (e: Exception) {
            Logger.withTag(loggerTag).e { "Profile sync failed: ${e.message}" }
        }
    }
}
