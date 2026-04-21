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
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.server.networking.createHttpClient
import org.darthacheron.pantrypal.server.networking.extractProfileId
import org.darthacheron.pantrypal.server.networking.respondGone
import org.darthacheron.pantrypal.server.networking.respondNotFound
import org.darthacheron.pantrypal.shared.auth.ProblemDetails
import org.darthacheron.pantrypal.shared.auth.TokenResponse
import org.darthacheron.pantrypal.shared.auth.UpdateUserDto
import org.koin.ktor.ext.inject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

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
        val userId = call.extractProfileId() ?: return@get

        profileService.getProfileById(userId).onSuccess { profile ->
            if (profile == null) {
                call.respondNotFound(
                    title = "Profile not found",
                    detail = "The profile associated with this account does not exist."
                )
            } else if (profile.deletedAt != null) {
                call.respondGone(
                    title = "Profile deleted",
                    detail = "The profile associated with this account has been deleted."
                )
            } else {
                call.respond(HttpStatusCode.OK, profile)
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

        val userId = call.extractProfileId() ?: return@put

        val updateDto = call.receive<UpdateUserDto>()

        profileService.getProfileById(userId).onSuccess { existingProfile ->
            if (existingProfile == null) {
                call.respondNotFound(
                    title = "Profile not found",
                    detail = "Cannot update a profile that does not exist."
                )
            } else if (existingProfile.deletedAt != null) {
                call.respondGone(
                    title = "Profile deleted",
                    detail = "Cannot update a deleted profile."
                )
            } else {
                try {
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
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get admin token")
                        return@onSuccess
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
                        call.respond(updateKeycloakResponse.status, updateKeycloakResponse.bodyAsText())
                        return@onSuccess
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
        }.onFailure { e ->
            call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                title = "Update failed",
                status = HttpStatusCode.InternalServerError.value,
                detail = e.message ?: "Failed to retrieve profile."
            ))
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.deleteProfile() {
    delete {
        val configurationService by inject<ConfigurationService>()
        val profileService by inject<ProfileService>()
        val httpClient = createHttpClient()

        val userId = call.extractProfileId() ?: return@delete
        val deleteRemote = call.request.queryParameters["remote"]?.toBoolean() ?: true

        profileService.getProfileById(userId).onSuccess { profile ->
            if (profile == null) {
                call.respondNotFound(
                    title = "Profile not found",
                    detail = "Cannot delete a profile that does not exist."
                )
            } else if (profile.deletedAt != null) {
                call.respondGone(
                    title = "Profile already deleted",
                    detail = "This profile has already been deleted."
                )
            } else {
                try {
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
                        if (adminTokenResponse.status != HttpStatusCode.OK) {
                            call.respond(HttpStatusCode.InternalServerError)
                            return@onSuccess
                        }
                        val adminToken = adminTokenResponse.body<TokenResponse>().accessToken

                        // 2. Delete from Keycloak
                        val deleteKeycloakResponse =
                            httpClient.delete("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users/$userId") {
                                header(HttpHeaders.Authorization, "Bearer $adminToken")
                            }

                        if (deleteKeycloakResponse.status != HttpStatusCode.NoContent && deleteKeycloakResponse.status != HttpStatusCode.OK) {
                            call.respond(deleteKeycloakResponse.status)
                            return@onSuccess
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
        }.onFailure { e ->
            call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                title = "Delete failed",
                status = HttpStatusCode.InternalServerError.value,
                detail = e.message ?: "An unexpected error occurred during profile deletion."
            ))
        }
    }
}
