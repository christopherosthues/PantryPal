package org.darthacheron.pantrypal.profile

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.settings.DataSynchronization
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileRepositoryImplTest {

    private lateinit var repository: ProfileRepositoryImpl
    private lateinit var profileDao: ProfileDao
    private lateinit var remoteProfileDao: RemoteProfileDao
    private lateinit var networkService: ProfileNetworkService
    private lateinit var authRepository: AuthenticationPreferencesRepository

    @BeforeTest
    fun setup() {
        profileDao = mock<ProfileDao>()
        remoteProfileDao = mock<RemoteProfileDao>()
        networkService = mock<ProfileNetworkService>()
        authRepository = mock<AuthenticationPreferencesRepository>()
        repository = ProfileRepositoryImpl(profileDao, remoteProfileDao, networkService, authRepository)
    }

    @Test
    fun testUpsertLocally() = runTest {
        val profile = createTestProfile()
        everySuspend { profileDao.upsert(any()) } returns Unit
        every { authRepository.authenticationPreferencesFlow } returns flowOf(AuthenticationPreferences("", "", 0, 0))

        repository.upsert(profile)

        verifySuspend { profileDao.upsert(any()) }
    }

    @Test
    fun testUpsertWithSync() = runTest {
        val profileId = Uuid.random()
        val profile = createTestProfile(profileId).copy(dataSynchronization = DataSynchronization.UPLOAD_AND_DOWNLOAD)
        val serverUrl = "http://localhost"
        
        everySuspend { profileDao.upsert(any()) } returns Unit
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token", "refresh", 3600, 7200, isLoggedInRemotely = true, serverUrl = serverUrl)
        )
        every { remoteProfileDao.getRemoteProfile(profileId, serverUrl) } returns flowOf(null)
        everySuspend { networkService.updateProfile(any(), any(), any(), any(), any()) } returns Result.success(null)

        repository.upsert(profile)

        verifySuspend { profileDao.upsert(any()) }
        verifySuspend { networkService.updateProfile(profile, serverUrl, null, null, null) }
    }

    @Test
    fun testDeleteRemote() = runTest {
        val serverUrl = "http://localhost"
        val profileId = Uuid.random()
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token", "refresh", 3600, 7200, localProfileId = profileId.toString(), isLoggedInRemotely = true, serverUrl = serverUrl)
        )
        everySuspend { networkService.deleteProfile(serverUrl, true) } returns Result.success(true)
        everySuspend { remoteProfileDao.delete(profileId, serverUrl) } returns Unit

        repository.deleteRemote()

        verifySuspend { networkService.deleteProfile(serverUrl, true) }
        verifySuspend { remoteProfileDao.delete(profileId, serverUrl) }
    }

    private fun createTestProfile(id: Uuid = Uuid.random()) = Profile(
        id = id,
        username = "testuser",
        email = "test@example.com",
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now()
    )
}
