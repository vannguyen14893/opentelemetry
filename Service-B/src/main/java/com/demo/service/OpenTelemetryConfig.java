package com.demo.service;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {
    @Value( "${spring.application.name}")
    private String serviceName;
    @Value( "${spring.application.version}")
    private String version;
    @Value("${management.zipkin.tracing.endpoint}")
    private String zipkinEndPoint;
    @Value("${management.jaeger.tracing.endpoint}")
    private String jaegerUrl;
    /**
     * Tạo OpenTelemetry instance với NoOp exporter
     * Chỉ tạo trace ID mà không export đi đâu cả
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addResource(developmentResource())
                .addSpanProcessor(BatchSpanProcessor.builder(ZipkinSpanExporter.builder()
                        .setEndpoint(zipkinEndPoint)
                        .build()).build())
                .addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder()
                        .setEndpoint(jaegerUrl).build()).build())
                .setSampler(Sampler.alwaysOn())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)

                .setPropagators(ContextPropagators.create(
                        W3CTraceContextPropagator.getInstance()
                ))
                .buildAndRegisterGlobal();
    }
    private  Resource developmentResource() {
        return Resource.create(Attributes.builder()
                .put(ServiceAttributes.SERVICE_NAME, serviceName)
                .put(ServiceAttributes.SERVICE_VERSION, version)
                .build()
        );
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("Service-B");
    }
}