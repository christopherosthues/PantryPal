package org.darthacheron.pantrypal.server.profile

import io.ktor.client.call.body
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
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.server.networking.createHttpClient
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

        profileService.getProfileById(userId).onSuccess { profile ->
            if (profile == null) {
                call.respond(
                    HttpStatusCode.NotFound, ProblemDetails(
                        title = "Profile not found",
                        status = HttpStatusCode.NotFound.value,
                        detail = "The profile associated with this account does not exist."
                    )
                )
            } else if (profile.deletedAt != null) {
                call.respond(
                    HttpStatusCode.Gone, ProblemDetails(
                        title = "Profile deleted",
                        status = HttpStatusCode.Gone.value,
                        detail = "The profile associated with this account has been deleted."
                    )
                )
            } else {
                call.respond(profile)
            }
        }.onFailure { e ->
            call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                title = "Error fetching profile",
                status = HttpStatusCode.InternalServerError.value,
                detail = e.message ?: "An unexpected error occurred."
            ))
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.updateProfile() {
    put {
        val configurationService by inject<ConfigurationService>()
        val profileService by inject<ProfileService>()
        val httpClient = createHttpClient()

        val principal = call.principal<JWTPrincipal>() ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val userIdStr = principal.payload.getClaim("sub").asString() ?: return@put call.respond(
            HttpStatusCode.BadRequest,
            "Missing sub claim"
        )
        val userId = Uuid.parse(userIdStr)

        val updateDto = call.receive<UpdateUserDto>()

        try {
            // Check existence and deleted status first
            val checkResult = profileService.getProfileById(userId)
            if (checkResult.isFailure) {
                return@put call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                    title = "Update failed",
                    status = HttpStatusCode.InternalServerError.value,
                    detail = checkResult.exceptionOrNull()?.message ?: "Failed to retrieve profile."
                ))
            }

            val existingProfile = checkResult.getOrNull()

            if (existingProfile == null) {
                return@put call.respond(HttpStatusCode.NotFound, ProblemDetails(
                    title = "Profile not found",
                    status = HttpStatusCode.NotFound.value,
                    detail = "Cannot update a profile that does not exist."
                ))
            } else if (existingProfile.deletedAt != null) {
                return@put call.respond(HttpStatusCode.Gone, ProblemDetails(
                    title = "Profile deleted",
                    status = HttpStatusCode.Gone.value,
                    detail = "Cannot update a deleted profile."
                ))
            }

            // 1. Get Admin Token
            val adminTokenResponse: HttpResponse = httpClient.submitForm(
                url = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "password")
                    append("client_id", "admin-cli")
                    append("username", configurationService.keycloakAdminUser)
                    append("password", configurationService.keycloakAdminPassword)
                }
            )

            if (adminTokenResponse.status != HttpStatusCode.OK) {
                return@put call.respond(HttpStatusCode.InternalServerError, "Failed to get admin token")
            }
            val adminToken = adminTokenResponse.body<TokenResponse>().accessToken

            // 2. Update Keycloak
            val updateKeycloakResponse = httpClient.put("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users/$userId") {
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
            val updatedProfile = existingProfile.copy(
                username = updateDto.username ?: existingProfile.username,
                email = updateDto.email ?: existingProfile.email,
                lastModifiedAt = Clock.System.now(),
                lastSyncedAt = Clock.System.now()
            )
            profileService.updateProfile(updatedProfile).onSuccess {
                call.respond(HttpStatusCode.OK, it)
            }.onFailure { e ->
                call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                    title = "Update failed",
                    status = HttpStatusCode.InternalServerError.value,
                    detail = e.message ?: "Failed to update profile in database."
                ))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                title = "Update failed",
                status = HttpStatusCode.InternalServerError.value,
                detail = e.message ?: "An unexpected error occurred during profile update."
            ))
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.deleteProfile() {
    delete("/users/me") {
        val configurationService by inject<ConfigurationService>()
        val profileService by inject<ProfileService>()
        val httpClient = createHttpClient()

        val principal = call.principal<JWTPrincipal>() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        val userIdStr = principal.payload.getClaim("sub").asString() ?: return@delete call.respond(
            HttpStatusCode.BadRequest,
            "Missing sub claim"
        )
        val userId = Uuid.parse(userIdStr)
        val deleteRemote = call.request.queryParameters["remote"]?.toBoolean() ?: true

        try {
            // Check if profile exists and if it's already deleted
            val profileResult = profileService.getProfileById(userId)
            val profile = profileResult.getOrNull()
            
            if (profile == null) {
                return@delete call.respond(HttpStatusCode.NotFound, ProblemDetails(
                    title = "Profile not found",
                    status = HttpStatusCode.NotFound.value,
                    detail = "Cannot delete a profile that does not exist."
                ))
            } else if (profile.deletedAt != null) {
                // If it's already deleted, we can return Gone or just success if we want idempotency.
                // The prompt says "Return NotFound ... and gone for requests where the profile is deleted".
                return@delete call.respond(HttpStatusCode.Gone, ProblemDetails(
                    title = "Profile already deleted",
                    status = HttpStatusCode.Gone.value,
                    detail = "This profile has already been deleted."
                ))
            }

            if (deleteRemote) {
                // 1. Get Admin Token
                val adminTokenResponse: HttpResponse = httpClient.submitForm(
                    url = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/token",
                    formParameters = parameters {
                        append("grant_type", "password")
                        append("client_id", "admin-cli")
                        append("username", configurationService.keycloakAdminUser)
                        append("password", configurationService.keycloakAdminPassword)
                    }
                )
                if (adminTokenResponse.status != HttpStatusCode.OK) return@delete call.respond(HttpStatusCode.InternalServerError)
                val adminToken = adminTokenResponse.body<TokenResponse>().accessToken

                // 2. Delete from Keycloak
                val deleteKeycloakResponse =
                    httpClient.delete("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users/$userId") {
                        header(HttpHeaders.Authorization, "Bearer $adminToken")
                    }

                if (deleteKeycloakResponse.status != HttpStatusCode.NoContent && deleteKeycloakResponse.status != HttpStatusCode.OK) {
                    return@delete call.respond(deleteKeycloakResponse.status)
                }
            }

            // 3. Soft Delete from Database
            profileService.deleteProfile(userId).onSuccess {
                call.respond(HttpStatusCode.NoContent)
            }.onFailure { e ->
                call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                    title = "Delete failed",
                    status = HttpStatusCode.InternalServerError.value,
                    detail = e.message ?: "Failed to delete profile from database."
                ))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                title = "Delete failed",
                status = HttpStatusCode.InternalServerError.value,
                detail = e.message ?: "An unexpected error occurred during profile deletion."
            ))
        }
    }
}
