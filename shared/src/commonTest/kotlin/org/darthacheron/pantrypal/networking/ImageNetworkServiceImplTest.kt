package org.darthacheron.pantrypal.networking

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.utils.HttpClientFactory
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ImageNetworkServiceImplTest {

    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var clientFactory: HttpClientFactory
    private lateinit var service: ImageNetworkServiceImpl

    @BeforeTest
    fun setup() {
        authRepository = mock<AuthenticationPreferencesRepository>()
        clientFactory = mock<HttpClientFactory>()
    }

    private fun setupService(mockEngine: MockEngine) {
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        every { clientFactory.create() } returns client
        service = ImageNetworkServiceImpl(authRepository, clientFactory)
    }

    @Test
    fun testUploadFoodImage_Success() = runTest {
        val foodId = Uuid.random()
        val serverUrl = "http://localhost"
        val imageUuid = Uuid.random()
        val profileUuid = Uuid.random()
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/food/$foodId/images", request.url.encodedPath)
            assertEquals("true", request.url.parameters["isPrimary"])
            respond(
                content = """
                    {
                        "serverId": "$imageUuid",
                        "profileId": "$profileUuid",
                        "createdAt": "2023-01-01T00:00:00Z",
                        "lastModifiedAt": "2023-01-01T00:00:00Z"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val result = service.uploadFoodImage(foodId, byteArrayOf(1, 2, 3), true, serverUrl)
        
        assertTrue(result.isSuccess)
        assertEquals(imageUuid, result.getOrNull()?.serverId)
    }

    @Test
    fun testDeleteFoodImage_Success() = runTest {
        val foodId = Uuid.random()
        val imageId = Uuid.random()
        val serverUrl = "http://localhost"
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/food/$foodId/images/$imageId", request.url.encodedPath)
            assertEquals(HttpMethod.Delete, request.method)
            respond(content = "", status = HttpStatusCode.NoContent)
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val result = service.deleteFoodImage(foodId, imageId, serverUrl)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrDefault(false))
    }
}
