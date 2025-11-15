package com.demo.service;

import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorConfigurer;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.common.util.InterceptorOrder;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@RequiredArgsConstructor
public class GrpcServerConfig {

    private final OpenTelemetry openTelemetry;
    @Bean
    public GlobalClientInterceptorConfigurer globalClientInterceptor() {
        return registry -> {
            // Add OpenTelemetry interceptor
            registry.add(
                    GrpcTelemetry.create(openTelemetry)
                            .newClientInterceptor()
            );
        };
    }
}
