package org.darthacheron.pantrypal.food

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
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodRepositoryTest {

    private lateinit var repository: FoodRepositoryImpl
    private lateinit var foodDao: FoodDao
    private lateinit var remoteFoodDao: RemoteFoodDao
    private lateinit var profileDao: ProfileDao
    private lateinit var foodNetworkService: FoodNetworkService
    private lateinit var imageNetworkService: ImageNetworkService
    private lateinit var remoteImageDao: RemoteImageDao
    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var fileSystem: FileSystem

    @BeforeTest
    fun setup() {
        foodDao = mock<FoodDao>()
        remoteFoodDao = mock<RemoteFoodDao>()
        profileDao = mock<ProfileDao>()
        foodNetworkService = mock<FoodNetworkService>()
        imageNetworkService = mock<ImageNetworkService>()
        remoteImageDao = mock<RemoteImageDao>()
        authRepository = mock<AuthenticationPreferencesRepository>()
        fileSystem = FileSystem.SYSTEM

        repository = FoodRepositoryImpl(
            foodDao,
            remoteFoodDao,
            profileDao,
            foodNetworkService,
            imageNetworkService,
            remoteImageDao,
            authRepository,
            fileSystem
        )
    }

    @Test
    fun testUpsertLocally() = runTest {
        val food = createTestFood()
        everySuspend { foodDao.getByIdWithImages(food.id) } returns null
        everySuspend { foodDao.upsert(any()) } returns Unit
        every { authRepository.authenticationPreferencesFlow } returns flowOf(AuthenticationPreferences("", "", 0, 0))

        repository.upsert(food)

        verifySuspend { foodDao.upsert(any()) }
    }

    @Test
    fun testUpsertWithSync() = runTest {
        val profileId = Uuid.random()
        val foodId = Uuid.random()
        val food = createTestFood(foodId, profileId)
        val serverUrl = "http://localhost"
        
        everySuspend { foodDao.getByIdWithImages(foodId) } returns null
        everySuspend { foodDao.upsert(any()) } returns Unit
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
        
        val serverFood = food.toDto(serverUrl).copy(serverId = Uuid.random())
        everySuspend { foodNetworkService.createFood(any(), any()) } returns Result.success(serverFood)
        everySuspend { remoteFoodDao.upsert(any()) } returns Unit

        repository.upsert(food)

        verifySuspend { foodDao.upsert(any()) }
        verifySuspend { foodNetworkService.createFood(any(), any()) }
        verifySuspend { remoteFoodDao.upsert(any()) }
    }

    private fun createTestFood(id: Uuid = Uuid.random(), profileId: Uuid = Uuid.random()) = Food(
        id = id,
        profileId = profileId,
        name = "Apple",
        createdAt = Instant.fromEpochSeconds(1700000000),
        lastModifiedAt = Instant.fromEpochSeconds(1700000000),
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
        bestBeforeUsedByDate = null,
        isUseBy = false,
        openedAt = null
    )
}
