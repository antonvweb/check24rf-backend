package org.example.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Общий фильтр Rate Limiting для всех сервисов.
 * Поддерживает разные лимиты для разных путей и автоочистку неактивных записей.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, RateLimiterEntry> rateLimiters = new ConcurrentHashMap<>();
    private final List<PathRateLimit> pathLimits;
    private final double defaultRequestsPerSecond;
    private final ScheduledExecutorService cleanupExecutor;

    private static final long ENTRY_TTL_MS = TimeUnit.MINUTES.toMillis(10);

    public RateLimitFilter(double defaultRequestsPerSecond, List<PathRateLimit> pathLimits) {
        this.defaultRequestsPerSecond = defaultRequestsPerSecond;
        this.pathLimits = pathLimits != null ? pathLimits : List.of();

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = getClientIP(request);
        String path = request.getRequestURI();
        double limit = resolveLimit(path);

        String key = ip + ":" + limit;
        RateLimiterEntry entry = rateLimiters.computeIfAbsent(key,
                k -> new RateLimiterEntry(RateLimiter.create(limit)));
        entry.touch();

        if (!entry.rateLimiter.tryAcquire(Duration.ZERO)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Map.of(
                            "success", false,
                            "message", "Слишком много запросов. Попробуйте позже.",
                            "retryAfter", 1
                    )
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private double resolveLimit(String path) {
        for (PathRateLimit pl : pathLimits) {
            if (path.startsWith(pl.pathPrefix)) {
                return pl.requestsPerSecond;
            }
        }
        return defaultRequestsPerSecond;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        rateLimiters.entrySet().removeIf(e -> now - e.getValue().lastAccessMs > ENTRY_TTL_MS);
    }

    @Override
    public void destroy() {
        cleanupExecutor.shutdownNow();
    }

    private String getClientIP(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        String ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    private static class RateLimiterEntry {
        final RateLimiter rateLimiter;
        volatile long lastAccessMs;

        RateLimiterEntry(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
            this.lastAccessMs = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessMs = System.currentTimeMillis();
        }
    }

    /**
     * Настройка лимита для конкретного пути.
     * Пути проверяются по startsWith, первое совпадение побеждает.
     */
    public record PathRateLimit(String pathPrefix, double requestsPerSecond) {}
}
