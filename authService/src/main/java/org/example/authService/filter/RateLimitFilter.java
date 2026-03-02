package org.example.authService.filter;

import com.google.common.util.concurrent.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Фильтр Rate Limiting на уровне приложения
 * Использует Google Guava RateLimiter для ограничения запросов
 *
 * Защита от:
 * - DDoS атак
 * - Brute-force атак
 * - Злоупотребления API
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Хранилище RateLimiter для каждого IP
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    // Лимит: 10 запросов в секунду
    private static final double REQUESTS_PER_SECOND = 10.0;
    private static final Duration TIMEOUT = Duration.ZERO; // Не ждать, сразу возвращать 429

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // Получаем IP адрес клиента
        String ip = getClientIP(request);

        // Получаем или создаём RateLimiter для этого IP
        RateLimiter rateLimiter = rateLimiters.computeIfAbsent(ip,
            k -> RateLimiter.create(REQUESTS_PER_SECOND, 1, TimeUnit.SECONDS));

        // Проверяем, есть ли доступные токены
        if (!rateLimiter.tryAcquire(TIMEOUT)) {
            // Лимит превышен
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                Map.of(
                    "success", false,
                    "message", "Слишком много запросов. Попробуйте позже.",
                    "retryAfter", 1 // секунда
                )
            ));
            return;
        }

        // Продолжаем обработку запроса
        filterChain.doFilter(request, response);
    }

    /**
     * Получение IP адреса клиента с учётом прокси
     */
    private String getClientIP(HttpServletRequest request) {
        String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        String ip = null;

        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For может содержать несколько IP через запятую
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                break;
            }
        }

        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Обработка IPv6 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }
}
