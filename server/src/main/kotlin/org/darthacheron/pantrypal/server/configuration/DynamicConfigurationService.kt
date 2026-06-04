package org.darthacheron.pantrypal.server.configuration

import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.slf4j.LoggerFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class ServerDynamicConfig(
    val deletionGracePeriodDays: Int = 30
)

class DynamicConfigurationService(private val configurationService: ConfigurationService) {
    private val logger = LoggerFactory.getLogger(DynamicConfigurationService::class.java)
    private val mongoClient = MongoClient.create(configurationService.mongodbUri)
    private val database = mongoClient.getDatabase(configurationService.mongodbDatabase)
    private val collection = database.getCollection<Document>("server_configuration")

    private var _cachedConfig = ServerDynamicConfig()
    val config: ServerDynamicConfig get() = _cachedConfig

    init {
        runBlocking {
            loadConfig()
        }
    }

    suspend fun loadConfig() {
        logger.info("Loading dynamic configuration from MongoDB")
        val doc = collection.find(Document("_id", "global_config")).firstOrNull()
        if (doc != null) {
            try {
                val jsonStr = doc.toJson()
                _cachedConfig = Json.decodeFromString<ServerDynamicConfig>(jsonStr)
                logger.info("Loaded dynamic configuration: {}", _cachedConfig)
            } catch (e: Exception) {
                logger.error("Failed to parse dynamic configuration from MongoDB, using defaults", e)
            }
        } else {
            logger.info("No dynamic configuration found in MongoDB, using defaults and saving them")
            saveConfig(ServerDynamicConfig())
        }
    }

    suspend fun saveConfig(newConfig: ServerDynamicConfig) {
        logger.info("Saving dynamic configuration to MongoDB: {}", newConfig)
        val jsonStr = Json.encodeToString(newConfig)
        val doc = Document.parse(jsonStr)
        doc["_id"] = "global_config"
        
        collection.replaceOne(
            Document("_id", "global_config"),
            doc,
            ReplaceOptions().upsert(true)
        )
        _cachedConfig = newConfig
    }

    fun close() {
        logger.info("Closing MongoDB client")
        mongoClient.close()
    }
}
