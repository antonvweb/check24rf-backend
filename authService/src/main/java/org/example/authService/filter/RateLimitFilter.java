package org.example.authService.filter;

import com.bucket4j.Bandwidth;
import com.bucket4j.Bucket;
import com.bucket4j.Bucket4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authService.utils.IPUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фильтр Rate Limiting на уровне приложения
 * Ограничивает количество запросов с одного IP адреса
 * 
 * Защита от:
 * - DDoS атак
 * - Brute-force атак
 * - Злоупотребления API
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Autowired
    private Map<String, Bucket> buckets;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Общий лимит: 10 запросов в секунду на IP
    private static final int GENERAL_LIMIT = 10;
    private static final Duration REFILL_DURATION = Duration.ofSeconds(1);
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Получаем IP адрес клиента
        String ip = getClientIP(request);
        
        // Получаем или создаём bucket для этого IP
        Bucket bucket = buckets.computeIfAbsent(ip, k -> createBucket());
        
        // Проверяем, есть ли доступные токены
        if (!bucket.tryConsume(1)) {
            // Лимит превышен
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                Map.of(
                    "success", false, 
                    "message", "Слишком много запросов. Попробуйте позже.",
                    "retryAfter", REFILL_DURATION.getSeconds()
                )
            ));
            return;
        }
        
        // Продолжаем обработку запроса
        filterChain.doFilter(request, response);
    }
    
    /**
     * Создание нового bucket'а с настройками лимита
     */
    private Bucket createBucket() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(GENERAL_LIMIT, 
                    com.bucket4j.Refill.greedy(GENERAL_LIMIT, REFILL_DURATION)))
                .build();
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
