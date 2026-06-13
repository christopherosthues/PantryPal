package org.darthacheron.pantrypal.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface HttpClientFactory {
    fun create(): HttpClient
}

class HttpClientFactoryImpl(private val engine: HttpClientEngine) : HttpClientFactory {
    override fun create(): HttpClient = createHttpClient(engine)
}

internal fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        })
    }
    install(Logging) {
        level = LogLevel.INFO
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
    defaultRequest {
        // Required for server CSRF/CORS validation
        header(HttpHeaders.Origin, "http://localhost:8081")
        header("X-CSRF-Token", "pantrypal-default-app-token") // TODO: provide csrf token
        contentType(ContentType.Application.Json)
    }
    expectSuccess = false
}
