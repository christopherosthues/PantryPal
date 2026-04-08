package org.darthacheron.pantrypal.server

//import dev.hayden.KHealth
import com.codahale.metrics.Slf4jReporter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.metrics.dropwizard.DropwizardMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import java.util.concurrent.TimeUnit

fun Application.configureMonitoring() {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
//    install(KHealth)
    install(DropwizardMetrics) {
        Slf4jReporter.forRegistry(registry)
            .outputTo(this@configureMonitoring.log)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()
            .start(10, TimeUnit.SECONDS)
    }
    val openTelemetry = getOpenTelemetry(serviceName = "opentelemetry-ktor-sample-server")

    install(KtorServerTelemetry) {
        setOpenTelemetry(openTelemetry)

        capturedRequestHeaders(HttpHeaders.UserAgent)

        spanKindExtractor {
            if (httpMethod == HttpMethod.Post) {
                SpanKind.PRODUCER
            } else {
                SpanKind.CLIENT
            }
        }

        attributesExtractor {
            onStart {
                attributes.put("start-time", System.currentTimeMillis())
            }
            onEnd {
                attributes.put("end-time", System.currentTimeMillis())
            }
        }
    }
    install(CallLogging) {
        callIdMdc("call-id")
    }
    routing {
        get("/hello") {
            call.respondText("Hello World!")
        }

        post("/post") {
            val postData = call.receiveText()
            call.respondText("Received: $postData")
        }
    }
}
