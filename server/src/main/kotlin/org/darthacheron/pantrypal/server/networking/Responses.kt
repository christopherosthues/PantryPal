package org.darthacheron.pantrypal.server.networking

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import org.darthacheron.pantrypal.server.configuration.*
import org.darthacheron.pantrypal.server.keycloak.InvalidCredentialsException
import org.darthacheron.pantrypal.server.keycloak.KeycloakException
import org.darthacheron.pantrypal.server.keycloak.UserAlreadyExistsException
import org.darthacheron.pantrypal.server.profile.ProfileAlreadyExistsException
import org.darthacheron.pantrypal.server.profile.ProfileDeletedException
import org.darthacheron.pantrypal.server.profile.ProfileNotFoundException
import org.darthacheron.pantrypal.core.auth.ProblemDetails
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
suspend fun ApplicationCall.extractProfileId(): Uuid? {
    val principal = principal<JWTPrincipal>() ?: run {
        respondUnauthorized()
        return null
    }

    val profileIdStr = principal.payload.getClaim("sub").asString() ?: run {
        respondBadRequestUserId()
        return null
    }

    return Uuid.parse(profileIdStr)
}

@OptIn(ExperimentalUuidApi::class)
suspend fun ApplicationCall.extractIdParameter(idName: String) : Uuid? {
    val parameter = parameters["id"] ?: run {
        respondBadRequest("Missing $idName")
        return null
    }

    return Uuid.parse(parameter)
}

suspend fun ApplicationCall.respondNotFound(title: String, detail: String) {
    respond(HttpStatusCode.NotFound, ProblemDetails(
        title = title,
        status = HttpStatusCode.NotFound.value,
        detail = detail
    ))
}

suspend fun ApplicationCall.respondUnauthorized() {
    respond(HttpStatusCode.Unauthorized, ProblemDetails(
        title = "Unauthorized",
        status = HttpStatusCode.Unauthorized.value,
        detail = "You are not logged in."
    ))
}

suspend fun ApplicationCall.respondBadRequest(detail: String) {
    respond(HttpStatusCode.BadRequest, ProblemDetails(
        title = "Bad Request",
        status = HttpStatusCode.BadRequest.value,
        detail = detail
    ))
}

suspend fun ApplicationCall.respondBadRequestUserId() {
    respondBadRequest(detail = "Missing user ID.")
}

suspend fun ApplicationCall.respondForbidden(detail: String) {
    respond(HttpStatusCode.Forbidden, ProblemDetails(
        title = "Forbidden",
        status = HttpStatusCode.Forbidden.value,
        detail = detail
    ))
}

suspend fun ApplicationCall.respondProblem(e: Throwable) {
    val (status, title, errors) = when (e) {
        is RegistrationDisabledException -> Triple(HttpStatusCode.Forbidden, "Registration Disabled", null)
        is RemoteSyncDisabledException -> Triple(HttpStatusCode.ServiceUnavailable, "Sync Disabled", null)
        is ImageTooLargeException -> Triple(HttpStatusCode.PayloadTooLarge, "Image Too Large", null)
        is UnsupportedImageTypeException -> Triple(HttpStatusCode.UnsupportedMediaType, "Unsupported Image Type", null)
        is InvalidCredentialsException -> Triple(HttpStatusCode.Unauthorized, "Invalid Credentials", null)
        is ProfileNotFoundException -> Triple(HttpStatusCode.NotFound, "Profile Not Found", null)
        is ProfileDeletedException -> Triple(HttpStatusCode.Gone, "Profile Deleted", null)
        is UserAlreadyExistsException -> Triple(HttpStatusCode.Conflict, "User Already Exists", null)
        is ProfileAlreadyExistsException -> Triple(HttpStatusCode.Conflict, "Profile Already Exists", mapOf("profile" to listOf(e.detail)))
        is KeycloakException -> Triple(e.status, "Identity Provider Error", null)
        else -> Triple(HttpStatusCode.InternalServerError, "Internal Server Error", null)
    }

    respond(status, ProblemDetails(
        title = title,
        status = status.value,
        detail = e.message ?: "An unexpected error occurred.",
        errors = errors
    ))
}

suspend fun ApplicationCall.respondGone(title: String, detail: String) {
    respond(HttpStatusCode.Gone, ProblemDetails(
        title = title,
        status = HttpStatusCode.Gone.value,
        detail = detail
    ))
}