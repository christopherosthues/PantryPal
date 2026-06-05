package org.darthacheron.pantrypal.server.networking

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import org.darthacheron.pantrypal.server.configuration.ImageTooLargeException
import org.darthacheron.pantrypal.server.configuration.RegistrationDisabledException
import org.darthacheron.pantrypal.server.configuration.RemoteSyncDisabledException
import org.darthacheron.pantrypal.server.configuration.UnsupportedImageTypeException
import org.darthacheron.pantrypal.shared.auth.ProblemDetails
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
    val (status, title) = when (e) {
        is RegistrationDisabledException -> HttpStatusCode.Forbidden to "Registration Disabled"
        is RemoteSyncDisabledException -> HttpStatusCode.ServiceUnavailable to "Sync Disabled"
        is ImageTooLargeException -> HttpStatusCode.PayloadTooLarge to "Image Too Large"
        is UnsupportedImageTypeException -> HttpStatusCode.UnsupportedMediaType to "Unsupported Image Type"
        else -> HttpStatusCode.InternalServerError to "Internal Server Error"
    }

    respond(status, ProblemDetails(
        title = title,
        status = status.value,
        detail = e.message ?: "An unexpected error occurred."
    ))
}

suspend fun ApplicationCall.respondGone(title: String, detail: String) {
    respond(HttpStatusCode.Gone, ProblemDetails(
        title = title,
        status = HttpStatusCode.Gone.value,
        detail = detail
    ))
}