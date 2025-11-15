package com.demo.service.a;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * LoggingServerInterceptor is a gRPC server interceptor that logs the details of incoming gRPC calls.
 *
 * This interceptor logs the full method name of the received gRPC call by utilizing the SLF4J logging framework.
 * It can be used as part of a middleware chain to monitor and trace gRPC calls for debugging or audit purposes.
 *
 * Implements:
 * - {@link ServerInterceptor}
 *
 * Methods:
 * - {@code interceptCall}: Intercepts a gRPC call and enables the logging of its method name before delegating
 *   the execution to the next handler in the chain.
 */
@Slf4j
public class LoggingServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        log.info("Received gRPC call: {}", call.getMethodDescriptor().getFullMethodName());
        return next.startCall(call, headers);
    }
}
