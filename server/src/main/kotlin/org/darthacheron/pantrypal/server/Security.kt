package org.darthacheron.pantrypal.server

import com.auth0.jwk.UrlJwkProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
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

        jwt("auth-jwt-admin") {
            realm = configurationService.keycloakRealm
            verifier(jwkProvider, configurationService.keycloakIssuer) {
                acceptLeeway(3)
                withAudience(configurationService.keycloakAudience)
                withIssuer(configurationService.keycloakIssuer)
            }
            validate { credential ->
                val audienceValid = credential.payload.audience.contains(configurationService.keycloakAudience)
                val realmAccess = credential.payload.getClaim("realm_access").asMap()
                val roles = realmAccess?.get("roles") as? List<*>
                val isAdmin = roles?.contains("admin") == true

                if (audienceValid && isAdmin) JWTPrincipal(credential.payload) else null
            }
        }

        oauth("auth-oauth-keycloak") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "keycloak",
                    authorizeUrl = "${configurationService.keycloakExternalBaseUrl}/realms/${configurationService.keycloakRealm}/protocol/openid-connect/auth",
                    accessTokenUrl = "${configurationService.keycloakBaseUrl}/realms/${configurationService.keycloakRealm}/protocol/openid-connect/token",
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
//        allowOrigin("http://localhost:8080")
//        allowOrigin("http://localhost:8081")
//        originMatchesHost()
        checkHeader("X-CSRF-Token") {
            it == configurationService.appToken
        }
    }
}
