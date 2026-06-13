package org.darthacheron.pantrypal.profile

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
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.utils.HttpClientFactoryImpl
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileNetworkServiceImplTest {

    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var service: ProfileNetworkServiceImpl

    @BeforeTest
    fun setup() {
        authRepository = mock<AuthenticationPreferencesRepository>()
    }

    private fun setupService(mockEngine: MockEngine) {
        val clientFactory = HttpClientFactoryImpl(mockEngine)
        service = ProfileNetworkServiceImpl(authRepository, clientFactory)
    }

    @Test
    fun testFetchProfile_Success() = runTest {
        val serverUrl = "http://localhost"
        val serverId = Uuid.random().toString()
        val clientId = Uuid.random().toString()
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/profile", request.url.encodedPath)
            assertEquals("Bearer token123", request.headers[HttpHeaders.Authorization])
            respond(
                content = """
                    {
                        "serverId": "$serverId",
                        "clientId": "$clientId",
                        "username": "testuser",
                        "email": "test@example.com",
                        "createdAt": "2023-01-01T00:00:00Z",
                        "lastModifiedAt": "2023-01-01T00:00:00Z",
                        "lastSyncedAt": "2023-01-01T00:00:00Z"
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

        val result = service.fetchProfile(serverUrl)
        
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals("testuser", profile.username)
    }

    @Test
    fun testFetchProfile_NotFound() = runTest {
        val serverUrl = "http://localhost"
        val mockEngine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.NotFound)
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val result = service.fetchProfile(serverUrl)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RemoteAccountDeletedException)
    }

    @Test
    fun testFetchProfile_ServerError() = runTest {
        val serverUrl = "http://localhost"
        val mockEngine = MockEngine { _ ->
            respond(content = "Error", status = HttpStatusCode.InternalServerError)
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val result = service.fetchProfile(serverUrl)
        
        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is RemoteAccountDeletedException)
    }

    @Test
    fun testUpdateProfile_Success() = runTest {
        val serverUrl = "http://localhost"
        val profileId = Uuid.random()
        val serverUserId = Uuid.random().toString()
        val profile = Profile(
            id = profileId,
            username = "testuser",
            email = "test@example.com",
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now()
        )
        
        val mockEngine = MockEngine { request ->
            assertEquals(HttpMethod.Put, request.method)
            respond(
                content = """
                    {
                        "serverId": "$serverUserId",
                        "clientId": "$profileId",
                        "username": "testuser",
                        "email": "test@example.com",
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

        val result = service.updateProfile(profile, serverUrl, null, null, null)
        
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun testUpdateProfile_Deleted() = runTest {
        val serverUrl = "http://localhost"
        val profile = Profile(id = Uuid.random(), username = "u", email = "e", createdAt = Clock.System.now(), lastModifiedAt = Clock.System.now())
        
        val mockEngine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.Gone)
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val result = service.updateProfile(profile, serverUrl, null, null, null)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RemoteAccountDeletedException)
    }

    @Test
    fun testDeleteProfile_Success() = runTest {
        val serverUrl = "http://localhost"
        val mockEngine = MockEngine { request ->
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("true", request.url.parameters["remote"])
            respond(content = "", status = HttpStatusCode.NoContent)
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val result = service.deleteProfile(serverUrl, true)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrDefault(false))
    }
}
