package org.darchacheron.pantrypal.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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

internal fun createHttpClient(accessToken: String): HttpClient = HttpClient(CIO) {
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
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        // Required for server CSRF/CORS validation
        header(HttpHeaders.Origin, "http://localhost:8081")
        header("X-CSRF-Token", "PantryPal") // TODO: provide csrf token
        contentType(ContentType.Application.Json)
    }
    expectSuccess = true
}
