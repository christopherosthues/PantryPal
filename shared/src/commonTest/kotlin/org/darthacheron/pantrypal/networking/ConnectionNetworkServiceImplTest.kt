package org.darthacheron.pantrypal.networking

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.darthacheron.pantrypal.utils.HttpClientFactory
import kotlin.test.*

class ConnectionNetworkServiceImplTest {

    private lateinit var clientFactory: HttpClientFactory
    private lateinit var service: ConnectionNetworkServiceImpl

    @BeforeTest
    fun setup() {
        clientFactory = mock<HttpClientFactory>()
    }

    private fun setupService(mockEngine: MockEngine) {
        val client = HttpClient(mockEngine)
        every { clientFactory.create() } returns client
        service = ConnectionNetworkServiceImpl(clientFactory)
    }

    @Test
    fun testConnection_Success() = runTest {
        val serverUrl = "http://localhost"
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/connection/test", request.url.encodedPath)
            respond(content = "Connection successful", status = HttpStatusCode.OK)
        }
        setupService(mockEngine)

        val result = service.testConnection(serverUrl)
        
        assertTrue(result.isSuccess)
        assertEquals("Connection successful", result.getOrNull())
    }

    @Test
    fun testConnection_Failure() = runTest {
        val serverUrl = "http://localhost"
        val mockEngine = MockEngine { _ ->
            respond(content = "Unauthorized", status = HttpStatusCode.Unauthorized)
        }
        setupService(mockEngine)

        val result = service.testConnection(serverUrl)
        
        assertTrue(result.isFailure)
    }
}
