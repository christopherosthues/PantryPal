package org.darchacheron.pantrypal.food

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodNetworkService(private val httpClient: HttpClient) {

    suspend fun pushFoods(foods: List<Food>, serverUrl: String): List<Food> {
        return httpClient.post("$serverUrl/api/food/batch") {
            contentType(ContentType.Application.Json)
            setBody(foods)
        }.body()
    }

    suspend fun fetchChanges(lastSync: Instant, serverUrl: String): List<Food> {
        return httpClient.get("$serverUrl/api/food/sync") {
            parameter("since", lastSync.toString())
        }.body()
    }

    suspend fun deleteFood(serverId: Uuid, serverUrl: String) {
        httpClient.delete("$serverUrl/api/food/$serverId")
    }
}
