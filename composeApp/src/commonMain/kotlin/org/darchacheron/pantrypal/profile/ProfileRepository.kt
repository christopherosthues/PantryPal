package org.darchacheron.pantrypal.profile

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.settings.DataSynchronization
import org.darchacheron.pantrypal.settings.SettingsRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileRepository(
    private val profileDao: ProfileDao,
    private val profileNetworkService: ProfileNetworkService,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val loggerTag = "ProfileRepository"

    fun getProfileById(id: Uuid): Flow<Profile?> =
        profileDao.getProfileById(id).map { it?.toProfile() }

    fun getProfileByServerId(serverId: Uuid): Flow<Profile?> =
        profileDao.getProfileByServerId(serverId).map { it?.toProfile() }

    fun getProfileByUsername(username: String): Flow<Profile?> =
        profileDao.getProfileByUsername(username).map { it?.toProfile() }

    suspend fun upsert(profile: Profile) {
        // Phase 1: Save locally
        profileDao.upsert(profile.toProfileEntity())

        // Phase 1: Try push immediately
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
            settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD
        ) {
            scope.launch {
                try {
                    profileNetworkService.updateProfile(profile, settings.serverUrl)?.let { synced ->
                        profileDao.upsert(synced.toProfileEntity())
                    }
                } catch (e: Exception) {
                    Logger.withTag(loggerTag).w { "Failed immediate profile sync: ${e.message}" }
                }
            }
        }
    }

    suspend fun delete() =
        profileDao.delete()

    suspend fun syncWithServer() {
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) return

        try {
            // Since there is only one profile, we just fetch it from the server
            // The server knows which profile to return based on the access token
            if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                settings.dataSynchronization == DataSynchronization.ONLY_DOWNLOAD
            ) {
                profileNetworkService.fetchProfile(settings.serverUrl)?.let { remoteProfile ->
                    profileDao.upsert(remoteProfile.toProfileEntity())
                }
            }
        } catch (e: Exception) {
            Logger.withTag(loggerTag).e { "Profile sync failed: ${e.message}" }
        }
    }
}
