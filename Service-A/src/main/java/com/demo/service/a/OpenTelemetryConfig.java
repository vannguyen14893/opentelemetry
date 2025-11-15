package com.demo.service.a;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentelemetry.semconv.ServiceAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up OpenTelemetry within the application.
 * This class defines beans required to initialize OpenTelemetry and configure tracing capabilities.
 * OpenTelemetry is used to collect and analyze telemetry data, enabling observability for the application.
 *
 * The primary functionality includes initializing a TracerProvider with AlwaysOn sampling and configuring
 * W3C trace context propagation.
 *
 * The configured Tracer can be utilized for manual instrumentation within application
 * components to create and manage spans for traceability.
 */
@Configuration
public class OpenTelemetryConfig {


    @Bean
    public OpenTelemetry openTelemetry() {
        DetailedLoggingSpanExporter loggingExporter = new DetailedLoggingSpanExporter();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setSampler(Sampler.alwaysOn())
                .addSpanProcessor(SimpleSpanProcessor.create(loggingExporter))
                .addResource(developmentResource())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(
                        W3CTraceContextPropagator.getInstance()
                ))
                .buildAndRegisterGlobal();
    }
    public static Resource developmentResource() {
        return Resource.create(Attributes.builder()
                .put(ServiceAttributes.SERVICE_NAME, "my-service")
                .put(ServiceAttributes.SERVICE_VERSION, "dev")
                .build()
        );
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("Service-A");
    }
}