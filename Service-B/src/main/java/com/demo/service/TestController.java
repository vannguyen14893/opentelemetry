package com.demo.service;

import com.demo.grpc.proto.HelloReply;
import com.demo.grpc.proto.HelloRequest;
import io.micrometer.tracing.Tracer;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.demo.grpc.proto.SimpleGrpc;
/**
 * TestController is a REST controller that provides endpoints to test XML and JSON responses.
 * It includes methods to return a welcome message and buttons in both XML and JSON formats.
 */
@RestController
public class TestController {
    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    @Autowired
    private Tracer tracer;
    @GrpcClient("hello")
    private SimpleGrpc.SimpleBlockingStub simpleBlobServiceBlockingStub;
    @GetMapping("/internal/response/json")
    public ResponseEntity<String> testResponseJson() {
        HelloRequest request = HelloRequest.newBuilder()
                .setName("ndvan123")
                .build();
        HelloReply helloReply = simpleBlobServiceBlockingStub.sayHello(request);
        String traceId = tracer.currentSpan() != null
                ? tracer.currentSpan().context().traceId()
                : "no-trace-id";

        String spanId = tracer.currentSpan() != null
                ? tracer.currentSpan().context().spanId()
                : "no-span-id";

        log.info("Processing /hello request - TraceId: {}, SpanId: {}", traceId, spanId);
        String jsonResponse = "{\n" +
                "  \"welcome\": \"Welcome to our application!\",\n" +
                "  \"buttons\": {\n" +
                "    \"submit\": \"Submit\",\n" +
                "    \"cancel\": \"Cancel\"\n" +
                "  },\n" +
                "  \"login\": {\n" +
                "    \"button\": \"Login\"\n" +
                "  }\n" +
                "}";
        return ResponseEntity.ok(helloReply.getMessage());
    }
}
