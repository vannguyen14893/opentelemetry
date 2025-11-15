package com.demo.service.a.config;

import com.demo.service.a.service.DetailedLoggingSpanExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up OpenTelemetry within the application.
 * This class defines beans required to initialize OpenTelemetry and configure tracing capabilities.
 * OpenTelemetry is used to collect and analyze telemetry data, enabling observability for the application.
 * <p>
 * The primary functionality includes initializing a TracerProvider with AlwaysOn sampling and configuring
 * W3C trace context propagation.
 * <p>
 * The configured Tracer can be utilized for manual instrumentation within application
 * components to create and manage spans for traceability.
 */
@Configuration
public class OpenTelemetryConfig {
    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${spring.application.version}")
    private String version;
    @Value("${management.zipkin.tracing.endpoint}")
    private String zipkinEndPoint;

    @Bean
    public OpenTelemetry openTelemetry() {
        DetailedLoggingSpanExporter loggingExporter = new DetailedLoggingSpanExporter();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setSampler(Sampler.alwaysOn())
                .addSpanProcessor(SimpleSpanProcessor.create(loggingExporter))
                .addSpanProcessor(BatchSpanProcessor.builder(ZipkinSpanExporter.builder()
                        .setEndpoint(zipkinEndPoint)
                        .build()).build())
                .addResource(developmentResource())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(
                        W3CTraceContextPropagator.getInstance()
                ))
                .buildAndRegisterGlobal();
    }

    private Resource developmentResource() {
        return Resource.create(Attributes.builder()
                .put(ServiceAttributes.SERVICE_NAME, serviceName)
                .put(ServiceAttributes.SERVICE_VERSION, version)
                .build()
        );
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("Service-A");
    }
}