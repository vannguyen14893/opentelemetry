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
    @GrpcClient("hello")
    private SimpleGrpc.SimpleBlockingStub simpleBlobServiceBlockingStub;
    @GetMapping("/service-b")
    public ResponseEntity<String> testResponseJson() {

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
        return ResponseEntity.ok(jsonResponse);
    }
    @GetMapping("/send-grpc")
    public ResponseEntity<String> sendGrpc() {
        HelloRequest request = HelloRequest.newBuilder()
                .setName("ndvan123")
                .build();
        HelloReply helloReply = simpleBlobServiceBlockingStub.sayHello(request);
        return ResponseEntity.ok(helloReply.getMessage());
    }
}
