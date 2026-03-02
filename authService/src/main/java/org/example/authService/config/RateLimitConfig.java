package org.example.authService.config;

import org.example.authService.filter.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Конфигурация Rate Limiting
 * Ограничивает количество запросов для предотвращения DDoS и brute-force атак
 */
@Configuration
public class RateLimitConfig {

    @PostConstruct
    public void init() {
        // Инициализация при старте
    }
}
