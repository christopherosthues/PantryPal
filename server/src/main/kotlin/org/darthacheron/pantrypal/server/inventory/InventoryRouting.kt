package org.darthacheron.pantrypal.server.inventory

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
        val principal = call.principal<JWTPrincipal>() ?: return@get call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respondBadRequestUserId()
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
        val principal = call.principal<JWTPrincipal>() ?: return@get call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respondBadRequestUserId()
        val profileId = Uuid.parse(profileIdStr)
        val idStr = call.parameters["id"] ?: return@get call.respondBadRequest("Missing inventory item ID")
        val id = Uuid.parse(idStr)

        inventoryItemService.getInventoryItemById(id).onSuccess { item ->
            if (item == null) {
                call.respondNotFound(
                    title = "Inventory item not found",
                    detail = "The requested inventory item does not exist."
                )
            } else if (item.profileId != profileId) {
                call.respondForbidden(detail = "You do not have permission to access this inventory item.")
            } else if (item.deletedAt != null) {
                call.respondGone(
                    title = "Inventory item deleted",
                    detail = "The requested inventory item has been deleted."
                )
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
        val principal = call.principal<JWTPrincipal>() ?: return@post call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@post call.respondBadRequestUserId()
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
        val principal = call.principal<JWTPrincipal>() ?: return@put call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@put call.respondBadRequestUserId()
        val profileId = Uuid.parse(profileIdStr)
        val inventoryItemDto = call.receive<InventoryItemDto>()

        val id = inventoryItemDto.serverId ?: return@put call.respondBadRequest("Missing inventory item server ID")
        
        inventoryItemService.getInventoryItemById(id).onSuccess { existing ->
            if (existing == null) {
                call.respondNotFound(
                    title = "Inventory item not found",
                    detail = "Cannot update non-existent inventory item."
                )
            } else if (existing.profileId != profileId) {
                call.respondForbidden(detail = "You do not have permission to update this inventory item.")
            } else if (existing.deletedAt != null) {
                call.respondGone(
                    title = "Inventory item deleted",
                    detail = "Cannot update a deleted inventory item."
                )
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
        val principal = call.principal<JWTPrincipal>() ?: return@delete call.respondUnauthorized()
        val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@delete call.respondBadRequestUserId()
        val profileId = Uuid.parse(profileIdStr)
        val idStr = call.parameters["id"] ?: return@delete call.respondBadRequest("Missing inventory item ID")
        val id = Uuid.parse(idStr)

        inventoryItemService.getInventoryItemById(id).onSuccess { existing ->
            if (existing == null) {
                call.respondNotFound(
                    title = "Inventory item not found",
                    detail = "Cannot delete non-existent inventory item."
                )
            } else if (existing.profileId != profileId) {
                call.respondForbidden(detail = "You do not have permission to delete this inventory item.")
            } else if (existing.deletedAt != null) {
                call.respondGone(
                    title = "Inventory item already deleted",
                    detail = "This inventory item has already been deleted."
                )
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
    route("/{id}/images") {
        get {
            val inventoryItemService by inject<InventoryItemService>()
            val principal = call.principal<JWTPrincipal>() ?: return@get call.respondUnauthorized()
            val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respondBadRequestUserId()
            val profileId = Uuid.parse(profileIdStr)
            val idStr = call.parameters["id"] ?: return@get call.respondBadRequest("Missing inventory item ID")
            val id = Uuid.parse(idStr)

            inventoryItemService.getInventoryItemById(id).onSuccess { item ->
                if (item == null) {
                    call.respondNotFound(
                        title = "Inventory item not found",
                        detail = "The requested inventory item does not exist."
                    )
                } else if (item.profileId != profileId) {
                    call.respondForbidden(detail = "You do not have permission to access this inventory image.")
                } else if (item.deletedAt != null) {
                    call.respondGone(
                        title = "Inventory item deleted",
                        detail = "Cannot access image of a deleted inventory item."
                    )
                } else {
                    call.respond(HttpStatusCode.OK, item.primaryImage?.let { listOf(it) + item.additionalImages } ?: item.additionalImages)
                }
            }.onFailure { call.respondProblem(it) }
        }

        get("/{imageId}") {
            val inventoryItemService by inject<InventoryItemService>()
            val principal = call.principal<JWTPrincipal>() ?: return@get call.respondUnauthorized()
            val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@get call.respondBadRequestUserId()
            val profileId = Uuid.parse(profileIdStr)
            val imageIdStr = call.parameters["imageId"] ?: return@get call.respondBadRequest("Missing image ID")
            val imageId = Uuid.parse(imageIdStr)

            inventoryItemService.getImage(imageId, profileId).onSuccess { bytes ->
                if (bytes != null) {
                    call.respondBytes(bytes, ContentType.Image.JPEG, HttpStatusCode.OK)
                } else {
                    call.respondNotFound(
                        title = "Image not found",
                        detail = "The requested inventory item does not exist."
                    )
                }
            }.onFailure { call.respondProblem(it) }
        }

        post {
            val inventoryItemService by inject<InventoryItemService>()
            val principal = call.principal<JWTPrincipal>() ?: return@post call.respondUnauthorized()
            val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@post call.respondBadRequestUserId()
            val profileId = Uuid.parse(profileIdStr)
            val idStr = call.parameters["id"] ?: return@post call.respondBadRequest("Missing inventory item ID")
            val id = Uuid.parse(idStr)
            val isPrimary = call.request.queryParameters["isPrimary"]?.toBoolean() ?: false

            val imageData = call.receiveChannel().readRemaining().readByteArray()
            inventoryItemService.saveImage(id, profileId, isPrimary, imageData).onSuccess {
                call.respond(HttpStatusCode.Created, it)
            }.onFailure { call.respondProblem(it) }
        }

        delete("/{imageId}") {
            val inventoryItemService by inject<InventoryItemService>()
            val principal = call.principal<JWTPrincipal>() ?: return@delete call.respondUnauthorized()
            val profileIdStr = principal.payload.getClaim("sub").asString() ?: return@delete call.respondBadRequestUserId()
            val profileId = Uuid.parse(profileIdStr)
            val imageIdStr = call.parameters["imageId"] ?: return@delete call.respondBadRequest("Missing image ID")
            val imageId = Uuid.parse(imageIdStr)

            inventoryItemService.deleteImage(imageId, profileId).onSuccess { deleted ->
                if (deleted) call.respond(HttpStatusCode.NoContent)
                else call.respondNotFound(title = "Image not found")
            }.onFailure { call.respondProblem(it) }
        }
    }
}
