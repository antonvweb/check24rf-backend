package org.example.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AuthService {
    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder encoder;

    private static final long ACCESS_EXPIRY = 15 * 60 * 1000;
    private static final long REFRESH_EXPIRY = 7 * 24 * 60 * 60 * 1000;

    public void register(RegisterRequest req) {
        if (userRepo.existsByLogin(req.getLogin()))
            throw new RuntimeException("Login already exists");

        User user = new User();
        user.setLogin(req.getLogin());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        userRepo.save(user);
    }

    public void authenticate(LoginRequest req, HttpServletResponse response) {
        User user = userRepo.findByLogin(req.getLogin())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!encoder.matches(req.getPassword(), user.getPassword()))
            throw new BadCredentialsException("Invalid password");

        String access = jwtUtil.generateToken(user.getLogin(), user.getRole(), ACCESS_EXPIRY);
        String refresh = jwtUtil.generateToken(user.getLogin(), user.getRole(), REFRESH_EXPIRY);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", access)
                .httpOnly(true).path("/").maxAge(ACCESS_EXPIRY / 1000).build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true).path("/api/auth/refresh").maxAge(REFRESH_EXPIRY / 1000).build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) throw new RuntimeException("No cookies");

        String token = Arrays.stream(cookies)
                .filter(c -> c.getName().equals("refreshToken"))
                .findFirst().map(Cookie::getValue)
                .orElseThrow(() -> new RuntimeException("No refresh token"));

        if (jwtUtil.isExpired(token)) throw new RuntimeException("Refresh expired");

        String login = jwtUtil.getUsername(token);
        Role role = jwtUtil.getRole(token);

        String newAccess = jwtUtil.generateToken(login, role, ACCESS_EXPIRY);
        String newRefresh = jwtUtil.generateToken(login, role, REFRESH_EXPIRY);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccess)
                .httpOnly(true).path("/").maxAge(ACCESS_EXPIRY / 1000).build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefresh)
                .httpOnly(true).path("/api/auth/refresh").maxAge(REFRESH_EXPIRY / 1000).build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}

