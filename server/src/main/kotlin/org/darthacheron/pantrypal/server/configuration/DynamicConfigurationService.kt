package org.darthacheron.pantrypal.server.configuration

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
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
import org.slf4j.Logger

@Serializable
data class DeletionConfig(
    val gracePeriodDays: Int = 30
)

@Serializable
data class FeatureToggles(
    val maintenanceModeEnabled: Boolean = false,
    val registrationEnabled: Boolean = true,
    val remoteSyncEnabled: Boolean = true
)

@Serializable
data class StorageLimits(
    val maxImageUploadSizeMB: Int = 10,
    val supportedImageTypes: List<String> = listOf("jpg", "jpeg", "png", "webp")
)

@Serializable
data class RateLimitConfig(
    val rateLimitCapacity: Int = 100
)

@Serializable
data class DiagnosticsConfig(
    val telemetrySamplingRate: Double = 1.0
)

@Serializable
data class LoggingConfig(
    val serverLogLevel: String = "INFO"
)

@Serializable
data class ServerDynamicConfig(
    val deletion: DeletionConfig = DeletionConfig(),
    val features: FeatureToggles = FeatureToggles(),
    val storage: StorageLimits = StorageLimits(),
    val rateLimiting: RateLimitConfig = RateLimitConfig(),
    val diagnostics: DiagnosticsConfig = DiagnosticsConfig(),
    val logging: LoggingConfig = LoggingConfig()
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
                applyLoggingConfig(_cachedConfig.logging)
            } catch (e: Exception) {
                logger.error("Failed to parse dynamic configuration from MongoDB, using defaults", e)
            }
        } else {
            logger.info("No dynamic configuration found in MongoDB, using defaults and saving them")
            val defaultConfig = ServerDynamicConfig()
            saveConfig(defaultConfig)
            applyLoggingConfig(defaultConfig.logging)
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
        applyLoggingConfig(newConfig.logging)
    }

    private fun applyLoggingConfig(loggingConfig: LoggingConfig) {
        try {
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
            val level = Level.toLevel(loggingConfig.serverLogLevel, Level.INFO)
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
