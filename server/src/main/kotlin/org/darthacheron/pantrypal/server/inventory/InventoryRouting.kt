package org.darthacheron.pantrypal.server.inventory

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
import org.darthacheron.pantrypal.server.camera.ImageDeletedException
import org.darthacheron.pantrypal.server.camera.ImageNotFoundException
import org.darthacheron.pantrypal.server.networking.extractIdParameter
import org.darthacheron.pantrypal.server.networking.extractProfileId
import org.darthacheron.pantrypal.server.networking.respondBadRequest
import org.darthacheron.pantrypal.server.networking.respondForbidden
import org.darthacheron.pantrypal.server.networking.respondGone
import org.darthacheron.pantrypal.server.networking.respondNotFound
import org.darthacheron.pantrypal.server.networking.respondProblem
import org.darthacheron.pantrypal.core.inventory.InventoryItemDto
import org.koin.ktor.ext.inject
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Route.inventoryItemRoutes() {
    route("/inventory") {
        getAllInventoryItems()
        getInventoryItemById()
        createInventoryItem()
        updateInventoryItem()
        deleteInventoryItem()
        batchUpdateInventory()
        syncInventory()
        inventoryImageRoutes()
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.batchUpdateInventory() {
    post("/batch") {
        val inventoryItemService by inject<InventoryItemService>()
        val profileId = call.extractProfileId() ?: return@post
        val items = call.receive<List<InventoryItemDto>>()

        inventoryItemService.batchUpdateInventory(profileId, items).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure {
            call.respondProblem(it)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.syncInventory() {
    get("/sync") {
        val inventoryItemService by inject<InventoryItemService>()
        val profileId = call.extractProfileId() ?: return@get
        val sinceString = call.request.queryParameters["since"] ?: return@get call.respondBadRequest("Missing 'since' parameter")
        val since = Instant.parse(sinceString)

        inventoryItemService.syncInventory(profileId, since).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure {
            call.respondProblem(it)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.getAllInventoryItems() {
    get {
        val inventoryItemService by inject<InventoryItemService>()
        val profileId = call.extractProfileId() ?: return@get

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
        val profileId = call.extractProfileId() ?: return@get
        val id = call.extractIdParameter("id") ?: return@get

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
        val profileId = call.extractProfileId() ?: return@post
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
        val profileId = call.extractProfileId() ?: return@put
        val inventoryItemDto = call.receive<InventoryItemDto>()

        inventoryItemService.updateInventoryItem(inventoryItemDto, profileId).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure { e ->
            when (e) {
                is InventoryItemMissingIdException -> call.respondBadRequest(e.message!!)
                is InventoryItemNotFoundException -> call.respondNotFound(
                    title = "Inventory item not found",
                    detail = "Cannot update non-existent inventory item."
                )
                is InventoryItemNoAccessException -> call.respondForbidden(detail = "You do not have permission to update this inventory item.")
                is InventoryItemDeletedException -> call.respondGone(
                    title = "Inventory item deleted",
                    detail = "Cannot update a deleted inventory item."
                )
                else -> call.respondProblem(e)
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.deleteInventoryItem() {
    delete("/{id}") {
        val inventoryItemService by inject<InventoryItemService>()
        val profileId = call.extractProfileId() ?: return@delete
        val id = call.extractIdParameter("id") ?: return@delete

        inventoryItemService.deleteInventoryItem(id, profileId).onSuccess {
            call.respond(HttpStatusCode.NoContent)
        }.onFailure { e ->
            when (e) {
                is InventoryItemNotFoundException -> call.respondNotFound(
                    title = "Inventory item not found",
                    detail = "Cannot delete non-existent inventory item."
                )
                is InventoryItemNoAccessException -> call.respondForbidden(detail = "You do not have permission to delete this inventory item.")
                is InventoryItemDeletedException -> call.respondGone(
                    title = "Inventory item already deleted",
                    detail = "This inventory item has already been deleted."
                )
                else -> call.respondProblem(e)
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.inventoryImageRoutes() {
    route("/{id}/images") {
        get {
            val inventoryItemService by inject<InventoryItemService>()
            val profileId = call.extractProfileId() ?: return@get
            val id = call.extractIdParameter("id") ?: return@get

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
            val profileId = call.extractProfileId() ?: return@get
            val imageId = call.extractIdParameter("imageId") ?: return@get

            inventoryItemService.getImageMetadata(imageId, profileId).onSuccess { image ->
                if (image == null) {
                    call.respondNotFound(
                        title = "Image not found",
                        detail = "The requested inventory image does not exist."
                    )
                } else if (image.deletedAt != null) {
                    call.respondGone(
                        title = "Image deleted",
                        detail = "The requested image has been deleted."
                    )
                } else {
                    inventoryItemService.getImage(imageId, profileId).onSuccess { bytes ->
                        if (bytes != null) {
                            call.respondBytes(bytes, ContentType.Image.JPEG, HttpStatusCode.OK)
                        } else {
                            call.respondNotFound(
                                title = "Image not found",
                                detail = "The image file could not be found on the server."
                            )
                        }
                    }.onFailure { call.respondProblem(it) }
                }
            }.onFailure { call.respondProblem(it) }
        }

        post {
            val inventoryItemService by inject<InventoryItemService>()
            val profileId = call.extractProfileId() ?: return@post
            val id = call.extractIdParameter("id") ?: return@post
            val isPrimary = call.request.queryParameters["isPrimary"]?.toBoolean() ?: false

            val imageData = call.receiveChannel().readRemaining().readByteArray()
            inventoryItemService.saveImage(id, profileId, isPrimary, imageData).onSuccess {
                call.respond(HttpStatusCode.Created, it)
            }.onFailure { e ->
                when (e) {
                    is InventoryItemNotFoundException -> call.respondNotFound(
                        title = "Inventory item not found",
                        detail = "Cannot add images to a non-existent inventory item."
                    )
                    is InventoryItemDeletedException -> call.respondGone(
                        title = "Inventory item deleted",
                        detail = "Cannot add images to a deleted inventory item."
                    )
                    else -> call.respondProblem(e)
                }
            }
        }

        delete("/{imageId}") {
            val inventoryItemService by inject<InventoryItemService>()
            val profileId = call.extractProfileId() ?: return@delete
            val imageId = call.extractIdParameter("imageId") ?: return@delete

            inventoryItemService.deleteImage(imageId, profileId).onSuccess { deleted ->
                if (deleted) call.respond(HttpStatusCode.NoContent)
                else call.respondNotFound(
                    title = "Image not found",
                    detail = "Failed to delete the inventory image."
                )
            }.onFailure { e ->
                when (e) {
                    is ImageNotFoundException -> call.respondNotFound(
                        title = "Image not found",
                        detail = "Cannot delete a non-existent inventory image."
                    )
                    is ImageDeletedException -> call.respondGone(
                        title = "Image already deleted",
                        detail = "This image has already been deleted."
                    )
                    else -> call.respondProblem(e)
                }
            }
        }
    }
}
