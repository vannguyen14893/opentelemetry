package com.demo.service.a.service;

import com.demo.service.a.entity.User;
import com.demo.service.a.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RedisService redisService;

    public List<User> list() {
        return userRepository.findAll(Pageable.ofSize(100)).getContent();
    }

    public User create(User user) {
        User save = userRepository.save(user);
        redisService.test(String.valueOf(save.getId()), save.getEmail());
        return save;
    }
}
