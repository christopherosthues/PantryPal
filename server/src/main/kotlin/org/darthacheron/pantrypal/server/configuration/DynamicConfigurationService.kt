package org.darthacheron.pantrypal.server.configuration

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.bson.Document
import org.darthacheron.pantrypal.core.configuration.LoggingConfiguration
import org.darthacheron.pantrypal.core.configuration.ServerDynamicConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DynamicConfigurationService(configurationService: ConfigurationService) {
    private val logger = LoggerFactory.getLogger(DynamicConfigurationService::class.java)
    private val mongoClient = MongoClient.create(configurationService.mongodbUri)
    private val database = mongoClient.getDatabase(configurationService.mongodbDatabase)
    private val collection = database.getCollection<Document>("server_configuration")

    private var _cachedConfiguration = ServerDynamicConfiguration()
    val config: ServerDynamicConfiguration get() = _cachedConfiguration

    init {
        runBlocking {
            loadConfiguration()
        }
    }

    suspend fun loadConfiguration() {
        logger.info("Loading dynamic configuration from MongoDB")
        val doc = collection.find(Document("_id", "global_config")).firstOrNull()
        if (doc != null) {
            try {
                val jsonStr = doc.toJson()
                _cachedConfiguration = Json.decodeFromString<ServerDynamicConfiguration>(jsonStr)
                logger.info("Loaded dynamic configuration: {}", _cachedConfiguration)
                applyLoggingConfiguration(_cachedConfiguration.logging)
            } catch (e: Exception) {
                logger.error("Failed to parse dynamic configuration from MongoDB, using defaults", e)
            }
        } else {
            logger.info("No dynamic configuration found in MongoDB, using defaults and saving them")
            val defaultConfig = ServerDynamicConfiguration()
            saveConfiguration(defaultConfig)
            applyLoggingConfiguration(defaultConfig.logging)
        }
    }

    suspend fun saveConfiguration(newConfiguration: ServerDynamicConfiguration) {
        logger.info("Saving dynamic configuration to MongoDB: {}", newConfiguration)
        val jsonStr = Json.encodeToString(newConfiguration)
        val doc = Document.parse(jsonStr)
        doc["_id"] = "global_config"
        
        collection.replaceOne(
            Document("_id", "global_config"),
            doc,
            ReplaceOptions().upsert(true)
        )
        _cachedConfiguration = newConfiguration
        applyLoggingConfiguration(newConfiguration.logging)
    }

    private fun applyLoggingConfiguration(loggingConfiguration: LoggingConfiguration) {
        try {
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
            val level = Level.toLevel(loggingConfiguration.serverLogLevel, Level.INFO)
            rootLogger.level = level
            logger.info("Applied log level: {}", level)
        } catch (e: Exception) {
            logger.error("Failed to apply logging configuration", e)
        }
    }

    fun close() {
        logger.info("Closing MongoDB client")
        mongoClient.close()
    }
}
