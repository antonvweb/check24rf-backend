package org.example.adminPanelService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.adminPanelService.entity.Role;
import org.example.adminPanelService.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepo;

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String token = Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
                .filter(c -> c.getName().equals("adminRefreshToken"))
                .findFirst().map(Cookie::getValue).orElse(null);

        if (token != null && !jwtUtil.isExpired(token)) {
            String username = jwtUtil.getUsername(token);
            Role role = jwtUtil.getRole(token);
           log.info("User role - {}", role.toString());

            UserDetails user = userRepo.findByLogin(username)
                    .map(u -> User.withUsername(username).password(u.getPassword()).roles(role.name()).build())
                    .orElse(null);

            if (user != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(req, res);
    }
}


