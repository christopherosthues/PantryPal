package org.darthacheron.pantrypal.server

import com.auth0.jwk.UrlJwkProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.oauth
import io.ktor.server.plugins.csrf.CSRF
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.koin.ktor.ext.inject
import java.net.URI
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Application.configureSecurity() {
    val configurationService by inject<ConfigurationService>()
    val jwkProvider = UrlJwkProvider(URI("${configurationService.keycloakBaseUrl}/realms/${configurationService.keycloakRealm}/protocol/openid-connect/certs").toURL())

    authentication {
        jwt("auth-jwt") {
            realm = configurationService.keycloakRealm
            verifier(jwkProvider, configurationService.keycloakIssuer) {
                acceptLeeway(3)
                withAudience(configurationService.keycloakAudience)
                withIssuer(configurationService.keycloakIssuer)
            }
            validate { credential ->
                if (credential.payload.audience.contains(configurationService.keycloakAudience)) JWTPrincipal(credential.payload) else null
            }
        }

        oauth("auth-oauth-keycloak") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "keycloak",
                    authorizeUrl = "${configurationService.keycloakExternalBaseUrl}/realms/${configurationService.keycloakRealm}/protocol/openid-connect/auth",
                    accessTokenUrl = "${configurationService.keycloakExternalBaseUrl}/realms/${configurationService.keycloakRealm}/protocol/openid-connect/token",
                    requestMethod = HttpMethod.Post,
                    clientId = configurationService.keycloakClientId,
                    clientSecret = configurationService.keycloakClientSecret,
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
}
