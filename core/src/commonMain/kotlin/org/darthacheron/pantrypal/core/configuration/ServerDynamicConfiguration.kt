package org.darthacheron.pantrypal.core.configuration

import kotlinx.serialization.Serializable

@Serializable
data class DeletionConfiguration(
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
data class RateLimitConfiguration(
    val rateLimitCapacity: Int = 100
)

@Serializable
data class DiagnosticsConfiguration(
    val telemetrySamplingRate: Double = 1.0
)

@Serializable
data class LoggingConfiguration(
    val serverLogLevel: String = "INFO"
)

@Serializable
data class ServerDynamicConfiguration(
    val deletion: DeletionConfiguration = DeletionConfiguration(),
    val features: FeatureToggles = FeatureToggles(),
    val storage: StorageLimits = StorageLimits(),
    val rateLimiting: RateLimitConfiguration = RateLimitConfiguration(),
    val diagnostics: DiagnosticsConfiguration = DiagnosticsConfiguration(),
    val logging: LoggingConfiguration = LoggingConfiguration()
)
