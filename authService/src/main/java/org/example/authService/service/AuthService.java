package org.example.authService.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authService.dto.LoginRequest;
import org.example.authService.entity.User;
import org.example.authService.repository.UserRepository;
import org.example.authService.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {
    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private SmsService smsService;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token.expiration:#{7*24*60*60*1000}}")
    private long REFRESH_EXPIRY;
    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(5);

    public String authenticate(LoginRequest req, HttpServletResponse response) {
        User user = userRepo.findByPhoneNumber(req.getPhoneNumber())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setPhoneNumber(req.getPhoneNumber());
                    return userRepo.save(newUser);
                });

        String accessToken = jwtUtil.generateAccessToken(user.getId().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(REFRESH_EXPIRY / 1000)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return accessToken;
    }

    public void sendVerificationCode(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be null or empty");
        }
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        redisTemplate.opsForValue().set(phoneNumber, code, CODE_EXPIRATION);
        smsService.sendSms(phoneNumber, "Code: " + code);
    }


    public boolean verifyCode(String phone, String code) {
        String redisCode = (String) redisTemplate.opsForValue().get(phone);
        return code.equals(redisCode);
    }

    public String refreshToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) throw new RuntimeException("No cookies");

        String token = Arrays.stream(cookies)
                .filter(c -> c.getName().equals("refreshToken"))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new RuntimeException("No refresh token"));

        if (jwtUtil.isExpired(token)) throw new RuntimeException("Refresh expired");

        Optional<String> userId = jwtUtil.getUserId(token);

        String newAccess =  jwtUtil.generateAccessToken(userId.orElse(null));
        String newRefresh =  jwtUtil.generateRefreshToken(userId.orElse(null));

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefresh)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(REFRESH_EXPIRY / 1000)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return newAccess;
    }
}

