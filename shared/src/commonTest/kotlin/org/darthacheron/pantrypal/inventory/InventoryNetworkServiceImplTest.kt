package org.darthacheron.pantrypal.inventory

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.inventory.InventoryItemDto
import org.darthacheron.pantrypal.utils.HttpClientFactoryImpl
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryNetworkServiceImplTest {

    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var service: InventoryNetworkServiceImpl

    private val testJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    @BeforeTest
    fun setup() {
        authRepository = mock<AuthenticationPreferencesRepository>()
    }

    private fun setupService(mockEngine: MockEngine) {
        val clientFactory = HttpClientFactoryImpl(mockEngine)
        service = InventoryNetworkServiceImpl(authRepository, clientFactory)
    }

    @Test
    fun testPushInventoryItems_Success() = runTest {
        val serverUrl = "http://localhost"
        val responseItem = createTestItemDto("Sugar").copy(serverId = Uuid.random())
        val responseJson = testJson.encodeToString(listOf(responseItem))
        
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/inventory/batch", request.url.encodedPath)
            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val items = listOf(createTestItemDto("Sugar"))
        val result = service.pushInventoryItems(items, serverUrl)
        
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertEquals("Sugar", list[0].name)
        assertNotNull(list[0].serverId)
    }

    @Test
    fun testDeleteInventoryItem() = runTest {
        val serverUrl = "http://localhost"
        val serverId = Uuid.random()
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/inventory/$serverId", request.url.encodedPath)
            assertEquals(HttpMethod.Delete, request.method)
            respond(content = "", status = HttpStatusCode.NoContent)
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val result = service.deleteInventoryItem(serverId, serverUrl)
        assertTrue(result.isSuccess)
        assertEquals(result.getOrNull(), true)
    }

    private fun createTestItemDto(name: String) = InventoryItemDto(
        serverId = null,
        clientId = Uuid.random(),
        profileId = Uuid.random(),
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
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now()
    )
}
