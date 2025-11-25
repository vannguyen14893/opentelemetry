package com.demo.service.a;

import com.demo.service.a.entity.User;
import com.demo.service.a.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

@SpringBootApplication
public class ServiceAApplication {
    @Autowired
    private UserRepository userRepository;
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        System.out.println("Default Timezone: " + TimeZone.getDefault().getID());

        SpringApplication.run(ServiceAApplication.class, args);
    }

    //@PostConstruct
    public void init() {
        List<User> users =new ArrayList();
        for (int i = 0; i < 1000; i++) {
            User user = new User();
            user.setName("User" + i);
            user.setEmail("user" + UUID.randomUUID()+ i + "@example.com");
            users.add(user);
        }
        userRepository.saveAll(users);
    }
}
