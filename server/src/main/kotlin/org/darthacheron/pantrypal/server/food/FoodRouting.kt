package org.darthacheron.pantrypal.server.food

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.darthacheron.pantrypal.server.networking.respondBadRequest
import org.darthacheron.pantrypal.server.networking.respondBadRequestUserId
import org.darthacheron.pantrypal.server.networking.respondForbidden
import org.darthacheron.pantrypal.server.networking.respondGone
import org.darthacheron.pantrypal.server.networking.respondNotFound
import org.darthacheron.pantrypal.server.networking.respondProblem
import org.darthacheron.pantrypal.server.networking.respondUnauthorized
import org.darthacheron.pantrypal.shared.auth.ProblemDetails
import org.darthacheron.pantrypal.shared.food.FoodDto
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Route.foodRoutes() {
    route("/food") {
        getAllFood()
        getFoodById()
        createFood()
        updateFood()
        deleteFood()
        foodImageRoutes()
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.getAllFood() {
    get {
        val foodService by inject<FoodService>()
        val principal = call.principal<JWTPrincipal>() ?: return@get call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respondBadRequestUserId()
        val profileId = Uuid.parse(profileIdStr)

        foodService.getAllFoodByProfileId(profileId).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure {
            call.respondProblem(it)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.getFoodById() {
    get("/{id}") {
        val foodService by inject<FoodService>()
        val principal = call.principal<JWTPrincipal>() ?: return@get call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respondBadRequestUserId()
        val profileId = Uuid.parse(profileIdStr)
        val idStr = call.parameters["id"] ?: return@get call.respondBadRequest("Missing food item ID")
        val id = Uuid.parse(idStr)

        foodService.getFoodById(id).onSuccess { food ->
            if (food == null) {
                call.respondNotFound(
                    title = "Food not found",
                    detail = "The requested food item does not exist."
                )
            } else if (food.profileId != profileId) {
                call.respondForbidden(detail = "You do not have permission to access this food item.")
            } else if (food.deletedAt != null) {
                call.respondGone(
                    title = "Food deleted",
                    detail = "The requested food item has been deleted."
                )
            } else {
                call.respond(HttpStatusCode.OK, food)
            }
        }.onFailure {
            call.respondProblem(it)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.createFood() {
    post {
        val foodService by inject<FoodService>()
        val principal = call.principal<JWTPrincipal>() ?: return@post call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@post call.respondBadRequestUserId()
        val profileId = Uuid.parse(profileIdStr)
        val foodDto = call.receive<FoodDto>()

        foodService.createFood(foodDto, profileId).onSuccess {
            call.respond(HttpStatusCode.Created, it)
        }.onFailure {
            call.respondProblem(it)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.updateFood() {
    put {
        val foodService by inject<FoodService>()
        val principal = call.principal<JWTPrincipal>() ?: return@put call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@put call.respondBadRequestUserId()
        val profileId = Uuid.parse(profileIdStr)
        val foodDto = call.receive<FoodDto>()

        val id = foodDto.serverId ?: return@put call.respondBadRequest("Missing food item server ID")
        
        foodService.getFoodById(id).onSuccess { existing ->
            if (existing == null) {
                call.respondNotFound(
                    title = "Food not found",
                    detail = "Cannot update non-existent food item."
                )
            } else if (existing.profileId != profileId) {
                call.respondForbidden(detail = "You do not have permission to update this food item.")
            } else if (existing.deletedAt != null) {
                call.respondGone(
                    title = "Food deleted",
                    detail = "Cannot update a deleted food item."
                )
            } else {
                foodService.updateFood(foodDto, profileId).onSuccess { updated ->
                    if (updated != null) {
                        call.respond(HttpStatusCode.OK, updated)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to update food item.")
                    }
                }.onFailure { call.respondProblem(it) }
            }
        }.onFailure { call.respondProblem(it) }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.deleteFood() {
    delete("/{id}") {
        val foodService by inject<FoodService>()
        val principal = call.principal<JWTPrincipal>() ?: return@delete call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@delete call.respondBadRequestUserId()
        val profileId = Uuid.parse(profileIdStr)
        val idStr = call.parameters["id"] ?: return@delete call.respondBadRequest("Missing food item ID")
        val id = Uuid.parse(idStr)

        foodService.getFoodById(id).onSuccess { existing ->
            if (existing == null) {
                call.respondNotFound(
                    title = "Food not found",
                    detail = "Cannot delete non-existent food item."
                )
            } else if (existing.profileId != profileId) {
                call.respondForbidden(detail = "You do not have permission to delete this food item.")
            } else if (existing.deletedAt != null) {
                call.respondGone(
                    title = "Food already deleted",
                    detail = "This food item has already been deleted."
                )
            } else {
                foodService.deleteFood(id, profileId).onSuccess { deleted ->
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to delete food item.")
                    }
                }.onFailure { call.respondProblem(it) }
            }
        }.onFailure { call.respondProblem(it) }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.foodImageRoutes() {
    route("/{id}/image") {
        get {
            val foodService by inject<FoodService>()
            val principal = call.principal<JWTPrincipal>() ?: return@get call.respondUnauthorized()
            val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respondBadRequestUserId()
            val profileId = Uuid.parse(profileIdStr)
            val idStr = call.parameters["id"] ?: return@get call.respondBadRequest("Missing food item ID")
            val id = Uuid.parse(idStr)

            foodService.getFoodById(id).onSuccess { food ->
                if (food == null) {
                    call.respondNotFound(
                        title = "Food not found",
                        detail = "The requested food item does not exist."
                    )
                } else if (food.profileId != profileId) {
                    call.respondForbidden(detail = "You do not have permission to access this food image.")
                } else if (food.deletedAt != null) {
                    call.respondGone(
                        title = "Food deleted",
                        detail = "Cannot access image of a deleted food item."
                    )
                } else {
                    foodService.getFoodImage(id, profileId).onSuccess { bytes ->
                        if (bytes != null) {
                            call.respondBytes(bytes, ContentType.Image.JPEG, HttpStatusCode.OK)
                        } else {
                            call.respondNotFound(
                                title = "Food image not found",
                                detail = "The requested food image does not exist."
                            )
                        }
                    }.onFailure { call.respondProblem(it) }
                }
            }.onFailure { call.respondProblem(it) }
        }

        post {
            val foodService by inject<FoodService>()
            val principal = call.principal<JWTPrincipal>() ?: return@post call.respondUnauthorized()
            val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@post call.respondBadRequestUserId()
            val profileId = Uuid.parse(profileIdStr)
            val idStr = call.parameters["id"] ?: return@post call.respondBadRequest("Missing food item ID")
            val id = Uuid.parse(idStr)
            
            foodService.getFoodById(id).onSuccess { food ->
                if (food == null) {
                    call.respondNotFound(
                        title = "Food not found",
                        detail = "The requested food item does not exist."
                    )
                } else if (food.profileId != profileId) {
                    call.respondForbidden(detail = "You do not have permission to upload an image for this food item.")
                } else if (food.deletedAt != null) {
                    call.respondGone(
                        title = "Food deleted",
                        detail = "Cannot upload image for a deleted food item."
                    )
                } else {
                    val imageData = call.receiveChannel().readRemaining().readByteArray()
                    foodService.saveFoodImage(id, profileId, imageData).onSuccess {
                        call.respond(HttpStatusCode.OK)
                    }.onFailure { call.respondProblem(it) }
                }
            }.onFailure { call.respondProblem(it) }
        }
    }
}
