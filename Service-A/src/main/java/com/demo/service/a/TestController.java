package com.demo.service.a;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TestController is a REST controller that provides endpoints to test XML and JSON responses.
 * It includes methods to return a welcome message and buttons in both XML and JSON formats.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class TestController {
    private final Tracer tracer;
    private final ExternalService externalService;
    private final MessageProducerService messageProducerService;

    @GetMapping("/internal/response/json")
    public ResponseEntity<String> testResponseJson() {
        String externalResult = externalService.callExternalApi("http://localhost:8081/service-b");
        return ResponseEntity.ok(externalResult);
    }

    @GetMapping("/send-message")
    public ResponseEntity<String> sendMessage() {
        messageProducerService.sendMessage("demo-topic", "Hello, Kafka!");
        return ResponseEntity.ok("Message sent to Kafka topic 'hello-topic'");
    }
}
