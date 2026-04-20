package org.darthacheron.pantrypal.server.inventory

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
import org.darthacheron.pantrypal.shared.auth.ProblemDetails
import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Route.inventoryItemRoutes() {
    route("/inventory") {
        getAllInventoryItems()
        getInventoryItemById()
        createInventoryItem()
        updateInventoryItem()
        deleteInventoryItem()
        inventoryImageRoutes()
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.getAllInventoryItems() {
    get {
        val inventoryItemService by inject<InventoryItemService>()
        val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing sub claim")
        val profileId = Uuid.parse(profileIdStr)

        inventoryItemService.getAllInventoryItemsByProfileId(profileId).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure {
            call.respondProblem(it)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.getInventoryItemById() {
    get("/{id}") {
        val inventoryItemService by inject<InventoryItemService>()
        val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing sub claim")
        val profileId = Uuid.parse(profileIdStr)
        val idStr = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val id = Uuid.parse(idStr)

        inventoryItemService.getInventoryItemById(id).onSuccess { item ->
            if (item == null) {
                call.respond(HttpStatusCode.NotFound, ProblemDetails(
                    title = "Inventory item not found",
                    status = HttpStatusCode.NotFound.value,
                    detail = "The requested inventory item does not exist."
                ))
            } else if (item.profileId != profileId) {
                call.respond(HttpStatusCode.Forbidden, ProblemDetails(
                    title = "Forbidden",
                    status = HttpStatusCode.Forbidden.value,
                    detail = "You do not have permission to access this inventory item."
                ))
            } else if (item.deletedAt != null) {
                call.respond(HttpStatusCode.Gone, ProblemDetails(
                    title = "Inventory item deleted",
                    status = HttpStatusCode.Gone.value,
                    detail = "The requested inventory item has been deleted."
                ))
            } else {
                call.respond(HttpStatusCode.OK, item)
            }
        }.onFailure {
            call.respondProblem(it)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.createInventoryItem() {
    post {
        val inventoryItemService by inject<InventoryItemService>()
        val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing sub claim")
        val profileId = Uuid.parse(profileIdStr)
        val inventoryItemDto = call.receive<InventoryItemDto>()

        inventoryItemService.createInventoryItem(inventoryItemDto, profileId).onSuccess {
            call.respond(HttpStatusCode.Created, it)
        }.onFailure {
            call.respondProblem(it)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.updateInventoryItem() {
    put {
        val inventoryItemService by inject<InventoryItemService>()
        val principal = call.principal<JWTPrincipal>() ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing sub claim")
        val profileId = Uuid.parse(profileIdStr)
        val inventoryItemDto = call.receive<InventoryItemDto>()

        val id = inventoryItemDto.serverId ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing serverId")
        
        inventoryItemService.getInventoryItemById(id).onSuccess { existing ->
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound, ProblemDetails(
                    title = "Inventory item not found",
                    status = HttpStatusCode.NotFound.value,
                    detail = "Cannot update non-existent inventory item."
                ))
            } else if (existing.profileId != profileId) {
                call.respond(HttpStatusCode.Forbidden, ProblemDetails(
                    title = "Forbidden",
                    status = HttpStatusCode.Forbidden.value,
                    detail = "You do not have permission to update this inventory item."
                ))
            } else if (existing.deletedAt != null) {
                call.respond(HttpStatusCode.Gone, ProblemDetails(
                    title = "Inventory item deleted",
                    status = HttpStatusCode.Gone.value,
                    detail = "Cannot update a deleted inventory item."
                ))
            } else {
                inventoryItemService.updateInventoryItem(inventoryItemDto, profileId).onSuccess { updated ->
                    if (updated != null) {
                        call.respond(HttpStatusCode.OK, updated)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to update inventory item.")
                    }
                }.onFailure { call.respondProblem(it) }
            }
        }.onFailure { call.respondProblem(it) }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.deleteInventoryItem() {
    delete("/{id}") {
        val inventoryItemService by inject<InventoryItemService>()
        val principal = call.principal<JWTPrincipal>() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing sub claim")
        val profileId = Uuid.parse(profileIdStr)
        val idStr = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        val id = Uuid.parse(idStr)

        inventoryItemService.getInventoryItemById(id).onSuccess { existing ->
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound, ProblemDetails(
                    title = "Inventory item not found",
                    status = HttpStatusCode.NotFound.value,
                    detail = "Cannot delete non-existent inventory item."
                ))
            } else if (existing.profileId != profileId) {
                call.respond(HttpStatusCode.Forbidden, ProblemDetails(
                    title = "Forbidden",
                    status = HttpStatusCode.Forbidden.value,
                    detail = "You do not have permission to delete this inventory item."
                ))
            } else if (existing.deletedAt != null) {
                call.respond(HttpStatusCode.Gone, ProblemDetails(
                    title = "Inventory item already deleted",
                    status = HttpStatusCode.Gone.value,
                    detail = "This inventory item has already been deleted."
                ))
            } else {
                inventoryItemService.deleteInventoryItem(id, profileId).onSuccess { deleted ->
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to delete inventory item.")
                    }
                }.onFailure { call.respondProblem(it) }
            }
        }.onFailure { call.respondProblem(it) }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.inventoryImageRoutes() {
    route("/{id}/image") {
        get {
            val inventoryItemService by inject<InventoryItemService>()
            val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing sub claim")
            val profileId = Uuid.parse(profileIdStr)
            val idStr = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = Uuid.parse(idStr)

            inventoryItemService.getInventoryItemById(id).onSuccess { item ->
                if (item == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else if (item.profileId != profileId) {
                    call.respond(HttpStatusCode.Forbidden, ProblemDetails(
                        title = "Forbidden",
                        status = HttpStatusCode.Forbidden.value,
                        detail = "You do not have permission to access this inventory image."
                    ))
                } else if (item.deletedAt != null) {
                    call.respond(HttpStatusCode.Gone, ProblemDetails(
                        title = "Inventory item deleted",
                        status = HttpStatusCode.Gone.value,
                        detail = "Cannot access image of a deleted inventory item."
                    ))
                } else {
                    inventoryItemService.getInventoryImage(id, profileId).onSuccess { bytes ->
                        if (bytes != null) {
                            call.respondBytes(bytes, ContentType.Image.JPEG, HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }.onFailure { call.respondProblem(it) }
                }
            }.onFailure { call.respondProblem(it) }
        }

        post {
            val inventoryItemService by inject<InventoryItemService>()
            val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing sub claim")
            val profileId = Uuid.parse(profileIdStr)
            val idStr = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val id = Uuid.parse(idStr)
            
            inventoryItemService.getInventoryItemById(id).onSuccess { item ->
                if (item == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else if (item.profileId != profileId) {
                    call.respond(HttpStatusCode.Forbidden, ProblemDetails(
                        title = "Forbidden",
                        status = HttpStatusCode.Forbidden.value,
                        detail = "You do not have permission to upload an image for this inventory item."
                    ))
                } else if (item.deletedAt != null) {
                    call.respond(HttpStatusCode.Gone, ProblemDetails(
                        title = "Inventory item deleted",
                        status = HttpStatusCode.Gone.value,
                        detail = "Cannot upload image for a deleted inventory item."
                    ))
                } else {
                    val imageData = call.receiveChannel().readRemaining().readByteArray()
                    inventoryItemService.saveInventoryImage(id, profileId, imageData).onSuccess {
                        call.respond(HttpStatusCode.OK)
                    }.onFailure { call.respondProblem(it) }
                }
            }.onFailure { call.respondProblem(it) }
        }
    }
}

private suspend fun ApplicationCall.respondProblem(e: Throwable) {
    respond(HttpStatusCode.InternalServerError, ProblemDetails(
        title = "Internal Server Error",
        status = HttpStatusCode.InternalServerError.value,
        detail = e.message ?: "An unexpected error occurred."
    ))
}
