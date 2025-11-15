package com.demo.service.a;

import io.opentelemetry.api.OpenTelemetry;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorConfigurer;
import net.devh.boot.grpc.server.interceptor.GlobalServerInterceptorConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;

/**
 * GrpcServerConfig is a configuration class for setting up gRPC server and client interceptors
 * with OpenTelemetry support. This class enables telemetry and logging for gRPC communications.
 */
@Configuration
@RequiredArgsConstructor
public class GrpcServerConfig {

    private final OpenTelemetry openTelemetry;
    @Bean
    public GlobalServerInterceptorConfigurer globalServerInterceptor() {
        return registry -> {
            registry.add(
                    GrpcTelemetry.create(openTelemetry)
                            .newServerInterceptor()
            );
            registry.add(new LoggingServerInterceptor());
        };
    }
    @Bean
    public GlobalClientInterceptorConfigurer globalClientInterceptor() {
        return registry -> {
            registry.add(
                    GrpcTelemetry.create(openTelemetry)
                            .newClientInterceptor()
            );
        };
    }}
