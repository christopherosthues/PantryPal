package org.darthacheron.pantrypal.server.profile

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.darthacheron.pantrypal.shared.auth.ProblemDetails
import org.darthacheron.pantrypal.shared.auth.TokenResponse
import org.darthacheron.pantrypal.shared.auth.UpdateUserDto
import org.koin.ktor.ext.inject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Route.profileRoutes() {
    route("/profile") {
        getProfile()
        createProfile()
        updateProfile()
        deleteProfile()
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.getProfile() {
    get {
        val profileService by inject<ProfileService>()
        val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val userIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "Missing sub claim"
        )
        val userId = Uuid.parse(userIdStr)

        val profile = profileService.getProfile(userId)
        if (profile == null) {
            call.respond(
                // TODO return Gone for deleted
                HttpStatusCode.NotFound, ProblemDetails(
                    title = "Profile not found",
                    status = HttpStatusCode.NotFound.value,
                    detail = "The profile associated with this account has been deleted or does not exist."
                )
            )
        } else {
            call.respond(profile)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.updateProfile() {
    put {
        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        val principal = call.principal<JWTPrincipal>() ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val userIdStr = principal.payload.getClaim("sub").asString() ?: return@put call.respond(
            HttpStatusCode.BadRequest,
            "Missing sub claim"
        )
        val userId = Uuid.parse(userIdStr)

        val updateDto = call.receive<UpdateUserDto>()

        try {
            // 1. Get Admin Token
            val adminTokenResponse: HttpResponse = httpClient.submitForm(
                url = "$keycloakBaseUrl/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "password")
                    append("client_id", "admin-cli")
                    append("username", keycloakAdminUser)
                    append("password", keycloakAdminPassword)
                }
            )

            if (adminTokenResponse.status != HttpStatusCode.OK) {
                return@put call.respond(HttpStatusCode.InternalServerError, "Failed to get admin token")
            }
            val adminToken = adminTokenResponse.body<TokenResponse>().accessToken

            // 2. Update Keycloak
            val updateKeycloakResponse = httpClient.put("$keycloakBaseUrl/admin/realms/$keycloakRealm/users/$userId") {
                header(HttpHeaders.Authorization, "Bearer $adminToken")
                contentType(ContentType.Application.Json)
                val body = mutableMapOf<String, Any>()
                updateDto.username?.let { body["username"] = it }
                updateDto.email?.let { body["email"] = it }
                updateDto.password?.let {
                    body["credentials"] = listOf(
                        mapOf(
                            "type" to "password",
                            "value" to it,
                            "temporary" to false
                        )
                    )
                }
                setBody(body)
            }

            if (updateKeycloakResponse.status != HttpStatusCode.NoContent && updateKeycloakResponse.status != HttpStatusCode.OK) {
                return@put call.respond(updateKeycloakResponse.status, updateKeycloakResponse.bodyAsText())
            }

            // 3. Update Database
            val existingProfile = profileService.getProfile(userId) ?: return@put call.respond(HttpStatusCode.NotFound)
            val updatedProfile = existingProfile.copy(
                username = updateDto.username ?: existingProfile.username,
                email = updateDto.email ?: existingProfile.email,
                lastModifiedAt = Clock.System.now(),
                lastSyncedAt = Clock.System.now()
            )
            profileService.updateProfile(updatedProfile)

            call.respond(HttpStatusCode.OK, updatedProfile)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.message ?: "Update failed")
        }
    }
}

delete("/users/me") {
    val principal = call.principal<JWTPrincipal>() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
    val userIdStr = principal.payload.getClaim("sub").asString() ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing sub claim")
    val userId = Uuid.parse(userIdStr)
    val deleteRemote = call.request.queryParameters["remote"]?.toBoolean() ?: true

    try {
        if (deleteRemote) {
            // 1. Get Admin Token
            val adminTokenResponse: HttpResponse = httpClient.submitForm(
                url = "$keycloakBaseUrl/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "password")
                    append("client_id", "admin-cli")
                    append("username", keycloakAdminUser)
                    append("password", keycloakAdminPassword)
                }
            )
            if (adminTokenResponse.status != HttpStatusCode.OK) return@delete call.respond(HttpStatusCode.InternalServerError)
            val adminToken = adminTokenResponse.body<TokenResponse>().accessToken

            // 2. Delete from Keycloak
            val deleteKeycloakResponse = httpClient.delete("$keycloakBaseUrl/admin/realms/$keycloakRealm/users/$userId") {
                header(HttpHeaders.Authorization, "Bearer $adminToken")
            }

            if (deleteKeycloakResponse.status != HttpStatusCode.NoContent && deleteKeycloakResponse.status != HttpStatusCode.OK) {
                return@delete call.respond(deleteKeycloakResponse.status)
            }
        }

        // 3. Soft Delete from Database
        profileService.deleteProfile(userId)
        call.respond(HttpStatusCode.NoContent)
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError)
    }
}