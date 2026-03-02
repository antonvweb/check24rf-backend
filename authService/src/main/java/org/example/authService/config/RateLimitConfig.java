package org.example.authService.config;

import com.bucket4j.Bandwidth;
import com.bucket4j.Bucket;
import com.bucket4j.Bucket4j;
import com.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Конфигурация Rate Limiting с использованием Bucket4J
 * Ограничивает количество запросов для предотвращения DDoS и brute-force атак
 */
@Configuration
public class RateLimitConfig {
    
    /**
     * Хранилище bucket'ов для каждого IP адреса
     * ConcurrentHashMap обеспечивает потокобезопасность
     */
    @Bean
    public Map<String, Bucket> buckets() {
        return new ConcurrentHashMap<>();
    }
    
    /**
     * Лимит для auth endpoints (login, register, verification)
     * 5 запросов в секунду - защита от brute-force
     */
    @Bean
    public Bandwidth authLimit() {
        return Bandwidth.classic(5, Refill.greedy(5, Duration.ofSeconds(1)));
    }
    
    /**
     * Лимит для обычных API endpoints
     * 20 запросов в секунду - стандартный лимит
     */
    @Bean
    public Bandwidth apiLimit() {
        return Bandwidth.classic(20, Refill.greedy(20, Duration.ofSeconds(1)));
    }
    
    /**
     * Создание bucket'а для IP адреса с кастомным лимитом
     */
    public Bucket createBucket(String ip, Bandwidth bandwidth) {
        return Bucket4j.builder()
                .addLimit(bandwidth)
                .build();
    }
}
