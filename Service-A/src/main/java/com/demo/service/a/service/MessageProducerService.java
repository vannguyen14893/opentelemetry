package com.demo.service.a.service;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Tracer tracer;

    public void sendMessage(String topic, String key, Object message) {
        Span currentSpan = tracer.currentSpan();
        assert currentSpan != null;
        String traceId = currentSpan.context().traceId();
        log.info("Sending message to Kafka - Topic: {}, Key: {}, TraceId: {}",
                topic, key, traceId);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, message);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Message sent successfully - Topic: {}, Partition: {}, Offset: {}, TraceId: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        traceId);
            } else {
                log.error("Failed to send message - TraceId: {}, Error: {}",
                        traceId, ex.getMessage());
            }
        });
    }

    /**
     * Gửi message đơn giản chỉ với value
     */
    public void sendMessage(String topic, Object message) {
        sendMessage(topic, null, message);
    }
}