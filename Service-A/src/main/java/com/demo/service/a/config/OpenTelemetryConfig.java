package com.demo.service.a.config;

import com.demo.service.a.service.DetailedLoggingSpanExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
@Slf4j
public class OpenTelemetryConfig {
    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${spring.application.version}")
    private String version;
    @Value("${management.zipkin.tracing.endpoint}")
    private String zipkinEndPoint;
    @Value("${management.jaeger.tracing.endpoint}")
    private String jaegerUrl;
    @Value("${management.otlp.tracing.endpoint}")
    private String otlpEndpoint;

    @Value("${otel.exporter.otlp.protocol:http/protobuf}")
    private String otlpProtocol;
    @Bean
    public OpenTelemetry openTelemetry() {
        DetailedLoggingSpanExporter loggingExporter = new DetailedLoggingSpanExporter();
        SpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint(jaegerUrl)
                .setTimeout(30, TimeUnit.SECONDS)
                .build();
        BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(jaegerExporter)
                .setScheduleDelay(1000, TimeUnit.MILLISECONDS)
                .setMaxExportBatchSize(512)
                .setMaxQueueSize(2048)
                .setExporterTimeout(30000, TimeUnit.MILLISECONDS)
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setSampler(Sampler.alwaysOn())
                .addSpanProcessor(SimpleSpanProcessor.create(loggingExporter))
                .addSpanProcessor(BatchSpanProcessor.builder(otlpSpanExporter())
                        .setScheduleDelay(Duration.ofSeconds(1))
                        .setMaxQueueSize(2048)
                        .setMaxExportBatchSize(512)
                        .build())
                .addSpanProcessor(BatchSpanProcessor.builder(ZipkinSpanExporter.builder()
                        .setEndpoint(zipkinEndPoint)
                        .build()).build())
                .addSpanProcessor(batchSpanProcessor)
                .addResource(createResource())
                .build();
        // Configure MeterProvider with more attributes for better metrics
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .addResource(createResource())
                .build();

        // Build and register the OpenTelemetry SDK
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
                .put("deployment.environment", getEnvironment())
                .put("telemetry.sdk.language", "java")
                .build()
        );
    }
    private String getEnvironment() {
        String env = System.getenv("DEPLOYMENT_ENVIRONMENT");
        return StringUtils.hasText(env) ? env : "development";
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
        return openTelemetry.getTracer("Service-A");
    }
}
