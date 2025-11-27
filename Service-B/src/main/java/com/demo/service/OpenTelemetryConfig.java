package com.demo.service;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenTelemetryConfig {
    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${spring.application.version}")
    private String version;
    @Value("${management.otlp.tracing.endpoint}")
    private String otlpEndpoint;
    @Value("${otel.exporter.otlp.protocol:http/protobuf}")
    private String otlpProtocol;

    /**
     * Tạo OpenTelemetry instance với NoOp exporter
     * Chỉ tạo trace ID mà không export đi đâu cả
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setSampler(Sampler.alwaysOn())
                .addSpanProcessor(BatchSpanProcessor.builder(otlpSpanExporter())
                        .setScheduleDelay(Duration.ofSeconds(1))
                        .setMaxQueueSize(2048)
                        .setMaxExportBatchSize(512)
                        .build())
                .addResource(createResource())
                .build();

        // Configure MeterProvider with resource attributes for better metrics
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .addResource(createResource())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setPropagators(ContextPropagators.create(
                        TextMapPropagator.composite(
                                W3CTraceContextPropagator.getInstance(),
                                W3CBaggagePropagator.getInstance()
                        )))
                .buildAndRegisterGlobal();
    }

    private Resource createResource() {
        return Resource.create(Attributes.builder()
                .put(ServiceAttributes.SERVICE_NAME, serviceName)
                .put(ServiceAttributes.SERVICE_VERSION, version)
                .build()
        );
    }

    @Bean
    public OtlpHttpSpanExporter otlpSpanExporter() {
        // Using HTTP protocol (recommended for most cases)
        if ("http/protobuf".equals(otlpProtocol)) {
            return OtlpHttpSpanExporter.builder()
                    .setEndpoint(otlpEndpoint + "/v1/traces")
                    .setTimeout(Duration.ofSeconds(10))
                    .build();
        } else {
            // Using gRPC protocol
            throw new UnsupportedOperationException("Use otlpGrpcSpanExporter for gRPC protocol");
        }
    }


    @Bean
    public OtlpGrpcSpanExporter otlpGrpcSpanExporter() {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .setTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("Service-B");
    }
}
