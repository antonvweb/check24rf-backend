package org.example.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.User;
import org.example.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private RedisTemplate<String, User> userCacheTemplate;
    
    private static final String USER_CACHE_PREFIX = "user:cache:";
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(5);
    
    // Публичные эндпоинты, не требующие аутентификации
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/send-code",
            "/api/auth/verify",
            "/api/auth/verify-captcha",
            "/api/auth/refresh",
            "/api/auth/csrf-token"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        
        // Пропустить health checks и публичные эндпоинты
        if (uri.startsWith("/actuator/") || uri.equals("/health")) {
            chain.doFilter(req, res);
            return;
        }
        
        // Пропустить публичные эндпоинты авторизации
        for (String publicEndpoint : PUBLIC_ENDPOINTS) {
            if (uri.startsWith(publicEndpoint)) {
                log.debug("Skipping authentication for public endpoint: {}", uri);
                chain.doFilter(req, res);
                return;
            }
        }

        // Извлечь токен из cookie
        String token = Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "accessToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        // Если нет в cookie, попробовать из Authorization header
        if (token == null) {
            String authHeader = req.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token != null && jwtUtil.isTokenValid(token) && !jwtUtil.isTokenBlacklisted(token)) {
            jwtUtil.getUserId(token).ifPresent(userIdStr -> {
                try {
                    UUID userId = UUID.fromString(userIdStr);
                    
                    // Попытка загрузить из кэша
                    String cacheKey = USER_CACHE_PREFIX + userId;
                    User user = userCacheTemplate.opsForValue().get(cacheKey);
                    
                    if (user == null) {
                        // Загрузить из БД и закэшировать
                        user = userRepo.findById(userId).orElse(null);
                        if (user != null) {
                            userCacheTemplate.opsForValue().set(cacheKey, user, USER_CACHE_TTL);
                            log.debug("User {} loaded from DB and cached", userId);
                        }
                    } else {
                        log.debug("User {} loaded from cache", userId);
                    }
                    
                    if (user != null) {
                        List<GrantedAuthority> authorities = user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, authorities);

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (IllegalArgumentException e) {
                    log.debug("Invalid user ID format", e);
                }
            });
        }

        chain.doFilter(req, res);
    }
}
