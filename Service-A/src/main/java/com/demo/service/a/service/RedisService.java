package com.demo.service.a.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {
    private final StringRedisTemplate redis;

    public String test(String key, String value) {
        log.info("Value: {}", value);
        redis.opsForValue().set(key, value);
        String valueRedis = redis.opsForValue().get(key);
        log.info("Value Redis: {}", valueRedis);
        return valueRedis;
    }

}
