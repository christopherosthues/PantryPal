package org.darthacheron.pantrypal.server

import com.auth0.jwk.UrlJwkProvider
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.csrf.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.net.URI

fun Application.configureSecurity() {
    val keycloakBaseUrl = environment.config.property("keycloak.baseUrl").getString()
    val keycloakClientId = environment.config.property("keycloak.clientId").getString()
    val keycloakRealm = environment.config.property("keycloak.realm").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtDomain = environment.config.property("jwt.domain").getString()

    val jwkProvider = UrlJwkProvider(URI("$jwtDomain/protocol/openid-connect/certs").toURL())

    authentication {
        jwt("auth-jwt") {
            realm = keycloakRealm
            verifier(jwkProvider, jwtDomain) {
                acceptLeeway(3)
                withAudience(jwtAudience)
                withIssuer(jwtDomain)
            }
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }

        oauth("auth-oauth-keycloak") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "keycloak",
                    authorizeUrl = "$keycloakBaseUrl/protocol/openid-connect/auth",
                    accessTokenUrl = "$keycloakBaseUrl/protocol/openid-connect/token",
                    requestMethod = HttpMethod.Post,
                    clientId = keycloakClientId,
                    clientSecret = "", // Public client
                    defaultScopes = listOf("openid", "profile", "email", "offline_access")
                )
            }
            client = HttpClient(CIO)
        }
    }

    install(CSRF) {
        // tests Origin is an expected value
        allowOrigin("http://localhost:8080")

        // tests Origin matches Host header
        originMatchesHost()

        // custom header checks
        checkHeader("X-CSRF-Token")
    }

    routing {
        authenticate("auth-oauth-keycloak") {
            get("/login") {
                // Redirects to Keycloak login page
            }
            get("/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                if (principal != null) {
                    call.respond(mapOf(
                        "access_token" to principal.accessToken,
                        "refresh_token" to principal.refreshToken,
                        "expires_in" to principal.expiresIn,
                        "token_type" to principal.tokenType
                    ))
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }

        get("/register") {
            val registrationUrl = "$keycloakBaseUrl/protocol/openid-connect/registrations" +
                    "?client_id=$keycloakClientId" +
                    "&response_type=code" +
                    "&scope=openid%20profile%20email%20offline_access" +
                    "&redirect_uri=${"http://localhost:8080/callback".encodeURLParameter()}"
            call.respondRedirect(registrationUrl)
        }
    }
}
