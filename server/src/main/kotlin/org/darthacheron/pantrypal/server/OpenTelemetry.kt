package org.darthacheron.pantrypal.server

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.semconv.ServiceAttributes
import org.darthacheron.pantrypal.server.configuration.DynamicConfigurationService

fun getOpenTelemetry(serviceName: String, dynamicConfigService: DynamicConfigurationService): OpenTelemetry {
    // Disable metrics exporter
    System.setProperty("otel.metrics.exporter", "none")

    return AutoConfiguredOpenTelemetrySdk.builder().addResourceCustomizer { oldResource, _ ->
        oldResource.toBuilder()
            .putAll(oldResource.attributes)
            .put(ServiceAttributes.SERVICE_NAME, serviceName)
            .build()
    }.addTracerProviderCustomizer { builder, _ ->
        builder.setSampler(object : Sampler {
            override fun shouldSample(
                parentContext: io.opentelemetry.context.Context,
                traceId: String,
                name: String,
                spanKind: io.opentelemetry.api.trace.SpanKind,
                attributes: io.opentelemetry.api.common.Attributes,
                parentLinks: List<io.opentelemetry.sdk.trace.data.LinkData>
            ): io.opentelemetry.sdk.trace.samplers.SamplingResult {
                val ratio = dynamicConfigService.config.diagnostics.telemetrySamplingRate
                return Sampler.traceIdRatioBased(ratio).shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks)
            }

            override fun getDescription(): String = "DynamicTraceIdRatioBasedSampler"
        })
    }.build().openTelemetrySdk
}

