package org.darthacheron.pantrypal.server.profile

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import org.darthacheron.pantrypal.server.networking.extractProfileId
import org.darthacheron.pantrypal.server.networking.respondGone
import org.darthacheron.pantrypal.server.networking.respondNotFound
import org.darthacheron.pantrypal.server.networking.respondProblem
import org.darthacheron.pantrypal.shared.auth.UpdateUserDto
import org.darthacheron.pantrypal.shared.profile.ProfileDto
import org.koin.ktor.ext.inject
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
            call.respondProblem(e)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.updateProfile() {
    val profileService by inject<ProfileService>()

    put {
        val userId = call.extractProfileId() ?: return@put
        val profileDto = call.receive<ProfileDto>()

        profileService.syncProfile(userId, profileDto).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure { e ->
            when (e) {
                is ProfileNotFoundException -> call.respondNotFound(
                    title = "Profile not found",
                    detail = "Cannot sync a profile that does not exist."
                )

                is ProfileDeletedException -> call.respondGone(
                    title = "Profile deleted",
                    detail = "Cannot sync a deleted profile."
                )

                else -> call.respondProblem(e)
            }
        }
    }

    patch {
        val userId = call.extractProfileId() ?: return@patch
        val updateDto = call.receive<UpdateUserDto>()

        profileService.updateProfile(userId, updateDto).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure { e ->
            when (e) {
                is ProfileNotFoundException -> call.respondNotFound(
                    title = "Profile not found",
                    detail = "Cannot update a profile that does not exist."
                )

                is ProfileDeletedException -> call.respondGone(
                    title = "Profile deleted",
                    detail = "Cannot update a deleted profile."
                )

                else -> call.respondProblem(e)
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.deleteProfile() {
    delete {
        val profileService by inject<ProfileService>()
        val userId = call.extractProfileId() ?: return@delete
        val deleteRemote = call.request.queryParameters["remote"]?.toBoolean() ?: true

        profileService.deleteProfile(userId, deleteRemote).onSuccess {
            call.respond(HttpStatusCode.NoContent)
        }.onFailure { e ->
            when (e) {
                is ProfileNotFoundException -> call.respondNotFound(
                    title = "Profile not found",
                    detail = "Cannot delete a profile that does not exist."
                )

                is ProfileDeletedException -> call.respondGone(
                    title = "Profile already deleted",
                    detail = "This profile has already been deleted."
                )

                else -> call.respondProblem(e)
            }
        }
    }
}
