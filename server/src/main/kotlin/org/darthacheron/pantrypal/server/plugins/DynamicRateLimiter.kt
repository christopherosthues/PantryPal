package org.darthacheron.pantrypal.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.respond
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.darthacheron.pantrypal.server.configuration.DynamicConfigurationService
import org.darthacheron.pantrypal.shared.auth.ProblemDetails
import org.koin.ktor.ext.inject
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class TokenBucket(var capacity: Int, val refillRate: Duration) {
    private var tokens: Double = capacity.toDouble()
    private var lastRefill: Instant = Clock.System.now()
    private val mutex = Mutex()

    suspend fun tryConsume(): Boolean = mutex.withLock {
        refill()
        if (tokens >= 1.0) {
            tokens -= 1.0
            true
        } else {
            false
        }
    }

    private fun refill() {
        val now = Clock.System.now()
        val elapsed = now - lastRefill
        val refillAmount = (elapsed.inWholeMilliseconds.toDouble() / refillRate.inWholeMilliseconds.toDouble()) * capacity
        tokens = (tokens + refillAmount).coerceAtMost(capacity.toDouble())
        lastRefill = now
    }
    
    fun updateCapacity(newCapacity: Int) {
        capacity = newCapacity
        // Optionally reset tokens or scale them
    }
}

val DynamicRateLimiter = createApplicationPlugin(name = "DynamicRateLimiter") {
    val dynamicConfigService by application.inject<DynamicConfigurationService>()
    val buckets = ConcurrentHashMap<String, TokenBucket>()

    onCall { call ->
        // Skip for admin routes
        if (call.request.local.uri.startsWith("/admin")) return@onCall

        val config = dynamicConfigService.config.rateLimiting
        val host = call.request.local.remoteHost
        
        val bucket = buckets.getOrPut(host) { 
            TokenBucket(config.rateLimitCapacity, 1.minutes) 
        }
        
        // Update capacity if it changed in config
        if (bucket.capacity != config.rateLimitCapacity) {
            bucket.updateCapacity(config.rateLimitCapacity)
        }

        if (!bucket.tryConsume()) {
            call.respond(HttpStatusCode.TooManyRequests, ProblemDetails(
                title = "Too Many Requests",
                status = HttpStatusCode.TooManyRequests.value,
                detail = "Rate limit exceeded. Please try again later."
            ))
        }
    }
}
