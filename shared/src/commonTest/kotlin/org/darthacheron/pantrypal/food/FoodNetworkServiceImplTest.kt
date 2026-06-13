package org.darthacheron.pantrypal.food

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.food.FoodDto
import org.darthacheron.pantrypal.utils.HttpClientFactory
import org.darthacheron.pantrypal.utils.createHttpClient
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodNetworkServiceImplTest {

    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var clientFactory: HttpClientFactory
    private lateinit var service: FoodNetworkServiceImpl
    private val authFlow = MutableStateFlow(AuthenticationPreferences("test-token", "refresh", 3600, 3600, "profile-id"))
    private val serverUrl = "https://example.com"

    @BeforeTest
    fun setup() {
        authRepository = mock<AuthenticationPreferencesRepository> {
            every { authenticationPreferencesFlow } returns authFlow
        }
        clientFactory = mock<HttpClientFactory>()
    }

    private fun setupMockEngine(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK) {
        val mockEngine = MockEngine { _ ->
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = createHttpClient(mockEngine)
        every { clientFactory.create() } returns client
    }

    @Test
    fun testPushFoods() = runTest {
        val foods = listOf(createTestFoodDto("Apple"))
        setupMockEngine(Json.encodeToString(foods))

        service = FoodNetworkServiceImpl(authRepository, clientFactory)
        val result = service.pushFoods(foods, serverUrl)

        assertEquals(1, result.size)
        assertEquals("Apple", result[0].name)
    }

    @Test
    fun testPushFoodsNotAuthenticated() = runTest {
        authFlow.value = authFlow.value.copy(accessToken = "")
        service = FoodNetworkServiceImpl(authRepository, clientFactory)
        
        val result = service.pushFoods(emptyList(), serverUrl)
        assertEquals(0, result.size)
    }

    @Test
    fun testFetchChanges() = runTest {
        val foods = listOf(createTestFoodDto("Banana"))
        setupMockEngine(Json.encodeToString(foods))

        service = FoodNetworkServiceImpl(authRepository, clientFactory)
        val result = service.fetchChanges(Instant.fromEpochSeconds(0), serverUrl)

        assertEquals(1, result.size)
        assertEquals("Banana", result[0].name)
    }

    @Test
    fun testDeleteFood() = runTest {
        setupMockEngine("", HttpStatusCode.NoContent)

        service = FoodNetworkServiceImpl(authRepository, clientFactory)
        service.deleteFood(Uuid.generateV7(), serverUrl)
        // No exception thrown means success
    }

    @Test
    fun testPushFoodsServerError() = runTest {
        setupMockEngine("Internal Server Error", HttpStatusCode.InternalServerError)

        service = FoodNetworkServiceImpl(authRepository, clientFactory)
        
        try {
            service.pushFoods(listOf(createTestFoodDto("Apple")), serverUrl)
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            // Success
        }
    }

    @Test
    fun testFetchChangesServerError() = runTest {
        setupMockEngine("Internal Server Error", HttpStatusCode.InternalServerError)

        service = FoodNetworkServiceImpl(authRepository, clientFactory)
        
        try {
            service.fetchChanges(Instant.fromEpochSeconds(0), serverUrl)
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            // Success
        }
    }

    private fun createTestFoodDto(name: String) = FoodDto(
        serverId = Uuid.generateV7(),
        profileId = Uuid.generateV7(),
        clientId = Uuid.generateV7(),
        name = name,
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
        openedAt = null,
        createdAt = Instant.fromEpochSeconds(1700000000),
        lastModifiedAt = Instant.fromEpochSeconds(1700000000)
    )
}
