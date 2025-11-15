package com.demo.service.a.controller;

import com.demo.service.a.entity.User;
import com.demo.service.a.service.ExternalService;
import com.demo.service.a.service.MessageProducerService;
import com.demo.service.a.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TestController is a REST controller that provides endpoints to test XML and JSON responses.
 * It includes methods to return a welcome message and buttons in both XML and JSON formats.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final ExternalService externalService;
    private final MessageProducerService messageProducerService;
    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getListUser() {
        List<User> users = userService.list();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping("/send-message")
    public ResponseEntity<String> sendMessage() {
        messageProducerService.sendMessage("demo-topic", "Hello, Kafka!");
        return ResponseEntity.ok("Message sent to Kafka topic 'hello-topic'");
    }

    @GetMapping("/call-external")
    public ResponseEntity<String> callExternalApi(@RequestParam(defaultValue = "http://localhost:8081/service-b") String url) {
        String externalResult = externalService.callExternalApi(url);
        return ResponseEntity.ok(externalResult);
    }
}
