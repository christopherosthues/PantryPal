package org.darthacheron.pantrypal.networking

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.darthacheron.pantrypal.utils.HttpClientFactoryImpl
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionNetworkServiceImplTest {

    private lateinit var service: ConnectionNetworkServiceImpl

    @BeforeTest
    fun setup() {
    }

    private fun setupService(mockEngine: MockEngine) {
        val clientFactory = HttpClientFactoryImpl(mockEngine)
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
