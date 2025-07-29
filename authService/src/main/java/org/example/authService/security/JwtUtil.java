package org.example.authService.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.example.authService.entity.TokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String CLAIM_USER_ID = "id";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final int MIN_SECRET_LENGTH = 32; // 256 bits

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token.expiration:#{60*60*1000}}")  // 1 час по умолчанию
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:#{7*24*60*60*1000}}")  // 7 дней по умолчанию
    private long refreshTokenExpiration;

    private SecretKey signingKey;

    @PostConstruct
    private void init() {
        validateSecretKey();
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        logger.info("JWT utility initialized successfully");
    }

    private void validateSecretKey() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalArgumentException("JWT secret cannot be null or empty");
        }

        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("JWT secret must be at least %d bytes (%d bits)",
                            MIN_SECRET_LENGTH, MIN_SECRET_LENGTH * 8)
            );
        }
    }

    /**
     * Генерирует access токен для пользователя
     */
    public String generateAccessToken(String userId) {
        return generateToken(userId, accessTokenExpiration, "access");
    }

    /**
     * Генерирует refresh токен для пользователя
     */
    public String generateRefreshToken(String userId) {
        return generateToken(userId, refreshTokenExpiration, "refresh");
    }

    /**
     * Генерирует токен с кастомным временем жизни
     */
    public String generateTokenFromUser(String userId, long expirationMillis) {
        return generateToken(userId, expirationMillis, "custom");
    }

    private String generateToken(String userId, long expirationMillis, String tokenType) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("Expiration time must be positive");
        }

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMillis);

        return Jwts.builder()
                .setSubject(userId)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Optional<Claims> getClaimsOptional(String token) {
        try {
            return Optional.of(getClaims(token));
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Failed to extract claims from token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Claims getClaims(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isExpired(String token) {
        return getClaimsOptional(token)
                .map(claims -> claims.getExpiration().before(new Date()))
                .orElse(true);
    }

    public Optional<String> getUserId(String token) {
        return getClaimsOptional(token)
                .map(Claims::getSubject);
    }

    public Optional<String> getTokenType(String token) {
        return getClaimsOptional(token)
                .map(claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token, "access");
    }

    public boolean isRefreshTokenValid(String token) {
        return isTokenValid(token, "refresh");
    }

    public boolean isTokenValid(String token) {
        return isTokenValid(token, null);
    }

    private boolean isTokenValid(String token, String expectedType) {
        try {
            Claims claims = getClaims(token);

            // Проверяем основные поля
            String subject = claims.getSubject();
            Date expiration = claims.getExpiration();
            Date now = new Date();

            boolean hasValidSubject = StringUtils.hasText(subject);
            boolean isNotExpired = expiration != null && expiration.after(now);

            // Проверяем тип токена если указан
            boolean hasValidType = true;
            if (expectedType != null) {
                String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
                hasValidType = expectedType.equals(tokenType);
            }

            boolean isValid = hasValidSubject && isNotExpired && hasValidType;

            if (logger.isDebugEnabled()) {
                logger.debug("Token validation - Subject: {}, Expired: {}, Type valid: {}, Overall valid: {}",
                        subject, !isNotExpired, hasValidType, isValid);
            }

            return isValid;

        } catch (ExpiredJwtException e) {
            logger.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.debug("Invalid token argument: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error during token validation", e);
            return false;
        }
    }

    public Optional<Long> getTimeToExpiration(String token) {
        return getClaimsOptional(token)
                .map(claims -> {
                    Date expiration = claims.getExpiration();
                    long timeLeft = expiration.getTime() - System.currentTimeMillis();
                    return Math.max(0, timeLeft / 1000); // в секундах
                });
    }

    public boolean shouldRefreshToken(String token) {
        return getTimeToExpiration(token)
                .map(timeLeft -> timeLeft < Duration.ofMinutes(5).getSeconds())
                .orElse(true);
    }

    public Optional<TokenInfo> getTokenInfo(String token) {
        return getClaimsOptional(token)
                .map(claims -> TokenInfo.builder()
                        .userId(claims.getSubject())
                        .tokenType(claims.get(CLAIM_TOKEN_TYPE, String.class))
                        .issuedAt(claims.getIssuedAt())
                        .expiresAt(claims.getExpiration())
                        .build());
    }
}


