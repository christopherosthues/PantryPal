package org.darthacheron.pantrypal.server.networking

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.darthacheron.pantrypal.shared.auth.ProblemDetails

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
    respond(HttpStatusCode.InternalServerError, ProblemDetails(
        title = "Internal Server Error",
        status = HttpStatusCode.InternalServerError.value,
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