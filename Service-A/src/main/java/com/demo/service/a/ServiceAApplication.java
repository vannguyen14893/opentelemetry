package com.demo.service.a;

import com.demo.service.a.entity.User;
import com.demo.service.a.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;

@SpringBootApplication
public class ServiceAApplication {
    @Autowired
    private UserRepository userRepository;
    public static void main(String[] args) {
        SpringApplication.run(ServiceAApplication.class, args);
    }

    @PostConstruct
    public void init() {
        List<User> users =new ArrayList();
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setName("User" + i);
            user.setEmail("user" + UUID.randomUUID()+ i + "@example.com");
            user.setPassword("password" + UUID.randomUUID());
            users.add(user);
        }
        userRepository.saveAll(users);
    }
}
