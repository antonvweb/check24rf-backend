package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptMarkerService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String MARKER_KEY_PREFIX = "receipt:marker:";

    public void saveMarker(String phoneNumber, String marker) {
        String key = MARKER_KEY_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(key, marker, Duration.ofDays(7));
        log.debug("Сохранен маркер для {}: {}", phoneNumber, marker);
    }

    public String getMarker(String phoneNumber) {
        String key = MARKER_KEY_PREFIX + phoneNumber;
        String marker = redisTemplate.opsForValue().get(key);
        log.debug("Получен маркер для {}: {}", phoneNumber, marker);
        return marker != null ? marker : "S_FROM_END";
    }
}