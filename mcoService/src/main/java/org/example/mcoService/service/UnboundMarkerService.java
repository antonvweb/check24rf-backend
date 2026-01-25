package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnboundMarkerService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String UNBOUND_MARKER_KEY = "mco:unbound:marker";

    public void saveMarker(String marker) {
        redisTemplate.opsForValue().set(UNBOUND_MARKER_KEY, marker, Duration.ofDays(7));
        log.debug("Сохранен маркер для отключившихся пользователей: {}", marker);
    }

    public String getMarker() {
        String marker = redisTemplate.opsForValue().get(UNBOUND_MARKER_KEY);
        log.debug("Получен маркер для отключившихся пользователей: {}", marker);
        return marker != null ? marker : "S_FROM_END";
    }

    public void resetMarker() {
        redisTemplate.delete(UNBOUND_MARKER_KEY);
        log.info("Маркер для отключившихся пользователей сброшен");
    }
}