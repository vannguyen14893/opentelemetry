package com.demo.service.a;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ExternalService {

    private static final Logger log = LoggerFactory.getLogger(ExternalService.class);

    private final RestTemplate restTemplate;
    private final Tracer tracer;

    public ExternalService(RestTemplate restTemplate, Tracer tracer) {
        this.restTemplate = restTemplate;
        this.tracer = tracer;
    }

    /**
     * Gọi API bên ngoài - Trace ID sẽ tự động được thêm vào header
     */
    public Map<String, Object> callExternalApi(String url) {
        String traceId = getCurrentTraceId();

        log.info("Calling external API: {} with TraceId: {}", url, traceId);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            log.info("External API responded - TraceId: {}, Status: {}",
                    traceId, response.getStatusCode());

            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling external API - TraceId: {}, Error: {}",
                    traceId, e.getMessage());
            throw e;
        }
    }

    /**
     * Gọi API với POST method
     */
    public Map<String, Object> postToExternalApi(String url, Object requestBody) {
        String traceId = getCurrentTraceId();

        log.info("POST to external API: {} with TraceId: {}", url, traceId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            log.info("External API POST responded - TraceId: {}, Status: {}",
                    traceId, response.getStatusCode());

            return response.getBody();
        } catch (Exception e) {
            log.error("Error POST to external API - TraceId: {}, Error: {}",
                    traceId, e.getMessage());
            throw e;
        }
    }

    /**
     * Gọi nhiều API liên tiếp (chain) - tất cả sẽ có cùng trace ID
     */
    public Map<String, Object> callMultipleApis(String url1, String url2) {
        String traceId = getCurrentTraceId();

        log.info("Starting multiple API calls with TraceId: {}", traceId);

        Map<String, Object> result1 = callExternalApi(url1);
        Map<String, Object> result2 = callExternalApi(url2);

        log.info("Completed multiple API calls with TraceId: {}", traceId);

        return Map.of(
                "traceId", traceId,
                "result1", result1,
                "result2", result2
        );
    }

    private String getCurrentTraceId() {
        return tracer.currentSpan() != null
                ? tracer.currentSpan().context().traceId()
                : "no-trace-id";
    }
}
