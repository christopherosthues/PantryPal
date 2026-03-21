package org.darchacheron.pantrypal.authentication

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object JwtUtils {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class)
    fun getUserIdFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) {
                return null
            }
            
            // JWT payload is the second part
            val payloadBase64 = parts[1]
            
            // Base64Url decoding (replacing characters if needed)
            val decodedBytes = Base64.UrlSafe.decode(payloadBase64)
            val decodedString = decodedBytes.decodeToString()
            
            val jsonObject = json.parseToJsonElement(decodedString).jsonObject
            // Standard JWT claim for user id is "sub", but check if your server uses "id" or something else
            jsonObject["sub"]?.jsonPrimitive?.content ?: jsonObject["id"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Logger.withTag("JwtUtils").e(e) { "Error decoding JWT token" }
            null
        }
    }
}
