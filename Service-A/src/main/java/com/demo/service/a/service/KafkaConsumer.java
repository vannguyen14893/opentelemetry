package com.demo.service.a.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    @KafkaListener(topics = "demo-topic", groupId = "demo-group")
    public void listen(ConsumerRecord<String, Object> record) {
        log.info("Message value: {}", record.value());
    }
}
