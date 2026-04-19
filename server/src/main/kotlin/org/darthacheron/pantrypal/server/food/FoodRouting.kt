package org.darthacheron.pantrypal.server.food

import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.darthacheron.pantrypal.shared.food.FoodDto
import org.koin.ktor.ext.inject

fun Route.foodRoutes() {
    route("/food") {
        foodById()
        createFood()
        updateFood()
        deleteFood()
    }
}

fun Route.foodById() {
    get("/{id}") {
        val foodService by inject<FoodService>()
        val id = call.parameters["id"]
        call.respondText("Food with id $id")
    }
}

fun Route.createFood() {
    post("/") {
        val foodService by inject<FoodService>()
        val food = call.receive<FoodDto>()
        call.respondText("Food created: $food")
    }
}

fun Route.updateFood() {
    put("/{id}") {
        val foodService by inject<FoodService>()
        val food = call.receive<FoodDto>()
        call.respondText("Food updated: $food")
    }
}

fun Route.deleteFood() {
    delete("/{id}") {
        val foodService by inject<FoodService>()
        val id = call.parameters["id"]
        call.respondText("Food deleted with id $id")
    }
}
