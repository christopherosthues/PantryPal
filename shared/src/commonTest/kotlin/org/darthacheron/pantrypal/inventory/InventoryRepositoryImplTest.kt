package org.darthacheron.pantrypal.inventory

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.SYSTEM
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.camera.RemoteImageDao
import org.darthacheron.pantrypal.networking.ImageNetworkService
import org.darthacheron.pantrypal.profile.ProfileDao
import org.darthacheron.pantrypal.profile.ProfileEntity
import org.darthacheron.pantrypal.settings.DataSynchronization
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryRepositoryImplTest {

    private lateinit var repository: InventoryRepositoryImpl
    private lateinit var itemDao: InventoryItemDao
    private lateinit var remoteItemDao: RemoteInventoryItemDao
    private lateinit var profileDao: ProfileDao
    private lateinit var networkService: InventoryNetworkService
    private lateinit var imageNetworkService: ImageNetworkService
    private lateinit var remoteImageDao: RemoteImageDao
    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var fileSystem: FileSystem

    @BeforeTest
    fun setup() {
        itemDao = mock<InventoryItemDao>()
        remoteItemDao = mock<RemoteInventoryItemDao>()
        profileDao = mock<ProfileDao>()
        networkService = mock<InventoryNetworkService>()
        imageNetworkService = mock<ImageNetworkService>()
        remoteImageDao = mock<RemoteImageDao>()
        authRepository = mock<AuthenticationPreferencesRepository>()
        fileSystem = FileSystem.SYSTEM

        repository = InventoryRepositoryImpl(
            itemDao,
            remoteItemDao,
            profileDao,
            networkService,
            imageNetworkService,
            remoteImageDao,
            authRepository,
            fileSystem
        )
    }

    @Test
    fun testUpsertLocally() = runTest {
        val item = createTestItem()
        everySuspend { itemDao.upsert(any()) } returns Unit
        every { authRepository.authenticationPreferencesFlow } returns flowOf(AuthenticationPreferences("", "", 0, 0))

        repository.upsert(item)

        verifySuspend { itemDao.upsert(any()) }
    }

    @Test
    fun testUpsertWithSync() = runTest {
        val profileId = Uuid.random()
        val itemId = Uuid.random()
        val item = createTestItem(itemId, profileId)
        val serverUrl = "http://localhost"
        
        everySuspend { itemDao.upsert(any()) } returns Unit
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token", "refresh", 3600, 7200, localProfileId = profileId.toString(), isLoggedInRemotely = true, serverUrl = serverUrl)
        )
        
        val profileEntity = ProfileEntity(
            id = profileId,
            username = "test",
            email = "test@test.com",
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now(),
            dataSynchronization = DataSynchronization.UPLOAD_AND_DOWNLOAD
        )
        every { profileDao.getProfileById(profileId) } returns flowOf(profileEntity)
        
        every { remoteItemDao.getRemoteInventoryItem(itemId, serverUrl) } returns flowOf(null)
        everySuspend { networkService.createInventoryItem(any(), any()) } returns createTestItem().toDto(serverUrl).copy(serverId = Uuid.random())
        everySuspend { remoteItemDao.upsert(any()) } returns Unit

        repository.upsert(item)

        verifySuspend { itemDao.upsert(any()) }
        verifySuspend { networkService.createInventoryItem(any(), any()) }
        verifySuspend { remoteItemDao.upsert(any()) }
    }

    private fun createTestItem(id: Uuid = Uuid.random(), profileId: Uuid = Uuid.random()) = InventoryItem(
        id = id,
        profileId = profileId,
        name = "Sugar",
        kiloCalories = null,
        kiloJoule = null,
        fatInGrams = null,
        saturatedFattyAcidsInGrams = null,
        carbsInGrams = null,
        sugarInGrams = null,
        dietaryFiberInGrams = null,
        proteinInGrams = null,
        saltInGrams = null,
        fillingQuantity = null,
        isLiquid = false,
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now()
    )
}
