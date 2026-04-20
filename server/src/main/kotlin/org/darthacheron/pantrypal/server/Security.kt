package org.darthacheron.pantrypal.server

import com.auth0.jwk.UrlJwkProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.csrf.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.darthacheron.pantrypal.server.authentication.authenticationRoutes
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.server.food.foodRoutes
import org.darthacheron.pantrypal.server.inventory.inventoryItemRoutes
import org.darthacheron.pantrypal.server.profile.ProfileRepository
import org.darthacheron.pantrypal.server.profile.ProfileService
import org.darthacheron.pantrypal.server.profile.profileRoutes
import org.darthacheron.pantrypal.shared.auth.*
import org.darthacheron.pantrypal.shared.profile.ProfileDto
import org.koin.ktor.ext.inject
import java.net.URI
import kotlin.getValue
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Application.configureSecurity() {
    val configurationService by inject<ConfigurationService>()
    val jwkProvider = UrlJwkProvider(URI("${configurationService.keycloakBaseUrl}/protocol/openid-connect/certs").toURL())

    authentication {
        jwt("auth-jwt") {
            realm = configurationService.keycloakRealm
            verifier(jwkProvider, configurationService.jwtDomain) {
                acceptLeeway(3)
                withAudience(configurationService.jwtAudience)
                withIssuer(configurationService.jwtDomain)
            }
            validate { credential ->
                if (credential.payload.audience.contains(configurationService.jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }

        oauth("auth-oauth-keycloak") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "keycloak",
                    authorizeUrl = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/auth",
                    accessTokenUrl = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/token",
                    requestMethod = HttpMethod.Post,
                    clientId = configurationService.keycloakClientId,
                    clientSecret = "", // Public client
                    defaultScopes = listOf("openid", "profile", "email", "offline_access")
                )
            }
            client = HttpClient(CIO)
        }
    }

    install(CSRF) {
        allowOrigin("http://localhost:8080")
        originMatchesHost()
        checkHeader("X-CSRF-Token")
    }

    routing {
        authenticationRoutes()

        authenticate("auth-jwt") {
            profileRoutes()
            foodRoutes()
            inventoryItemRoutes()
        }
    }
}
