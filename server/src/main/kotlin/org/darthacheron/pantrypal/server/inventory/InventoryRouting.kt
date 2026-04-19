package org.darthacheron.pantrypal.server.inventory

import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Route.inventoryItemRoutes() {
    route("/inventory") {
        inventoryItemById()
        createInventoryItem()
        updateInventoryItem()
        deleteInventoryItem()
    }
}

fun Route.inventoryItemById() {
    get("/{id}") {
        val inventoryItemService by inject<InventoryItemService>()
        val id = call.parameters["id"]
        call.respondText("Inventory item with id $id")
    }
}

fun Route.createInventoryItem() {
    post("/") {
        val inventoryItemService by inject<InventoryItemService>()
        val inventoryItem = call.receive<InventoryItemDto>()
        call.respondText("Inventory created: $inventoryItem")
    }
}

fun Route.updateInventoryItem() {
    put("/{id}") {
        val inventoryItemService by inject<InventoryItemService>()
        val inventoryItem = call.receive<InventoryItemDto>()
        call.respondText("Inventory updated: $inventoryItem")
    }
}

fun Route.deleteInventoryItem() {
    delete("/{id}") {
        val inventoryItemService by inject<InventoryItemService>()
        val id = call.parameters["id"]
        call.respondText("Inventory deleted with id $id")
    }
}