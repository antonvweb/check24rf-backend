package org.example.billingService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.userService.entity.User;
import org.example.userService.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String token = Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
                .filter(c -> c.getName().equals("accessToken"))
                .findFirst().map(Cookie::getValue).orElse(null);

        if (token != null && !jwtUtil.isExpired(token)) {
            Optional<String> userIdOpt = jwtUtil.getUserId(token);

            if (userIdOpt.isPresent()) {
                UUID userId = UUID.fromString(userIdOpt.get());
                Optional<User> userOpt = userRepo.findById(userId);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    // 👇 Если ролей нет — можно оставить список пустым
                    List<GrantedAuthority> authorities = List.of(); // либо List.of(new SimpleGrantedAuthority("ROLE_USER"))

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        chain.doFilter(req, res);
    }
}



