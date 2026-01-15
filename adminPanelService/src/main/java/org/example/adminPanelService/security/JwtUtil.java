package org.example.adminPanelService.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.example.adminPanelService.entity.Role;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key = Keys.hmacShaKeyFor("your_super_secret_key_256bits_abcd1234".getBytes(StandardCharsets.UTF_8));


    public String generateToken(String subject, Role role, long expirationMillis) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public boolean isExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Role getRole(String token) {
        return Role.valueOf(getClaims(token).get("role", String.class));
    }

    public boolean isAccessTokenValid(String token) {
        try {
            Claims claims = getClaims(token);
            String username = claims.getSubject();
            boolean expired = claims.getExpiration().before(new Date());

            System.out.println("✅ Token subject: " + username);
            System.out.println("📅 Expiration: " + claims.getExpiration());
            System.out.println("⌛ Expired? " + expired);

            return !expired && username != null && !username.isEmpty();

        } catch (ExpiredJwtException e) {
            System.out.println("❌ Token expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("❌ Unsupported JWT: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("❌ Malformed JWT: " + e.getMessage());
        } catch (SignatureException e) {
            System.out.println("❌ Invalid JWT signature: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Empty or null token: " + e.getMessage());
        }

        return false;
    }

}


