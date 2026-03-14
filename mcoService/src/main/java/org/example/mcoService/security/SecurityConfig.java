package org.example.mcoService.security;

import org.example.common.security.JwtFilter;
import org.example.common.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Autowired
    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter(15.0, List.of(
                new RateLimitFilter.PathRateLimit("/api/mco/bind-users-batch", 2.0),
                new RateLimitFilter.PathRateLimit("/api/mco/bind-user", 3.0),
                new RateLimitFilter.PathRateLimit("/api/mco/send-notification", 5.0),
                new RateLimitFilter.PathRateLimit("/api/mco/unbind-user", 3.0),
                new RateLimitFilter.PathRateLimit("/api/mco/register", 1.0)
        ));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/mco/ws").permitAll()
                        .requestMatchers("/ws").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/mco/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                        .xssProtection(org.springframework.security.config.Customizer.withDefaults())
                )
                .build();
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
