package org.darthacheron.pantrypal.profile

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.first
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
    fun testGetProfileById() = runTest {
        val id = Uuid.random()
        val entity = ProfileEntity(id, "user", "email", "", Clock.System.now(), Clock.System.now())
        val remoteEntity = RemoteProfileEntity(id, "http://srv", Uuid.random(), "remUser", "remEmail", Clock.System.now())
        
        every { profileDao.getProfileById(id) } returns flowOf(entity)
        every { remoteProfileDao.getRemoteProfilesByLocalProfileId(id) } returns flowOf(listOf(remoteEntity))

        val profile = repository.getProfileById(id).first()
        
        assertNotNull(profile)
        assertEquals("user", profile.username)
        assertEquals(1, profile.remoteProfiles.size)
        assertEquals("remUser", profile.remoteProfiles[0].username)
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
    fun testDeleteRemote_NotLoggedIn() = runTest {
        every { authRepository.authenticationPreferencesFlow } returns flowOf(AuthenticationPreferences("", "", 0, 0, isLoggedInRemotely = false))
        
        assertFailsWith<Exception> {
            repository.deleteRemote()
        }
    }

    @Test
    fun testSyncWithServer_NoSync() = runTest {
        val id = Uuid.random()
        val profile = createTestProfile(id).copy(dataSynchronization = DataSynchronization.NO_SYNCHRONIZATION)
        
        every { authRepository.authenticationPreferencesFlow } returns flowOf(AuthenticationPreferences("", "", 0, 0, localProfileId = id.toString()))
        every { profileDao.getProfileById(id) } returns flowOf(profile.toProfileEntity())
        
        repository.syncWithServer()
        
        // Should not reach the network service
        // verifySuspend { networkService.fetchProfile(any()) } // implicitly verified by not failing if we didn't mock it
    }

    @Test
    fun testSyncWithServer_Success() = runTest {
        val id = Uuid.random()
        val profile = createTestProfile(id).copy(
            dataSynchronization = DataSynchronization.UPLOAD_AND_DOWNLOAD,
            isLocalOnly = false,
            lastSyncedAt = Clock.System.now().minus(kotlin.time.Duration.parse("1h")),
            lastModifiedAt = Clock.System.now()
        )
        val serverUrl = "http://localhost"
        val remoteProfile = RemoteProfile(id, serverUrl, Uuid.random(), "rem", "rem@ex.com", Clock.System.now())

        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("t", "r", 3600, 7200, localProfileId = id.toString(), isLoggedInRemotely = true, serverUrl = serverUrl)
        )
        every { profileDao.getProfileById(id) } returns flowOf(profile.toProfileEntity())
        every { remoteProfileDao.getRemoteProfile(id, serverUrl) } returns flowOf(remoteProfile.toRemoteProfileEntity())
        everySuspend { networkService.updateProfile(any(), any(), any(), any(), any()) } returns Result.success(null)
        everySuspend { networkService.fetchProfile(serverUrl) } returns Result.success(null)

        repository.syncWithServer()

        verifySuspend { networkService.updateProfile(any(), serverUrl, any(), any(), any()) }
        verifySuspend { networkService.fetchProfile(serverUrl) }
    }

    private fun createTestProfile(id: Uuid = Uuid.random()) = Profile(
        id = id,
        username = "testuser",
        email = "test@example.com",
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now()
    )
}
