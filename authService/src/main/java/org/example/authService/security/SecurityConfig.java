package org.example.authService.security;

import org.example.common.utils.CorsProperties;
import org.example.common.security.JwtFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtFilter jwtFilter;
    private final CorsProperties corsProperties;

    @Autowired
    public SecurityConfig(JwtFilter jwtFilter, CorsProperties corsProperties) {
        this.jwtFilter = jwtFilter;
        this.corsProperties = corsProperties;

        log.info("SecurityConfig успешно создан");
        log.info("JwtFilter инжектирован: {}", jwtFilter != null ? "да" : "нет");
        log.info("CorsProperties инжектирован: {}", corsProperties != null ? "да" : "нет");

        if (corsProperties != null) {
            List<String> origins = corsProperties.getAllowedOrigins();
            log.info("Начальное значение allowedOrigins из CorsProperties при создании конфига: {}",
                    origins != null ? origins : "null");
            if (origins == null || origins.isEmpty()) {
                log.warn("ВНИМАНИЕ: allowedOrigins пустой или null уже на этапе создания SecurityConfig");
            }
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.debug("Начинается конфигурация SecurityFilterChain");

        return http
                .cors(cors -> {
                    log.debug("Включаем CORS с использованием нашего CorsConfigurationSource");
                    cors.configurationSource(corsConfigurationSource());
                })
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("=== ЗАПУСК СОЗДАНИЯ CORS КОНФИГУРАЦИИ ===");

        CorsConfiguration configuration = new CorsConfiguration();

        // Получаем origins
        List<String> originsFromProps = corsProperties.getAllowedOrigins();
        log.info("Получено из CorsProperties.getAllowedOrigins(): {}",
                originsFromProps != null ? originsFromProps : "null");

        List<String> finalOrigins;

        if (originsFromProps == null) {
            log.error("!!! CorsProperties вернул null для allowedOrigins !!!");
            finalOrigins = List.of("*"); // временный fallback для теста
            log.warn("Используем временный fallback: allowedOrigins = *");
        } else if (originsFromProps.isEmpty()) {
            log.error("!!! Список allowedOrigins ПУСТОЙ (size = 0) !!!");
            finalOrigins = List.of("http://172.16.0.1:3000", "http://localhost:3000", "*");
            log.warn("Установлен fallback-список origins: {}", finalOrigins);
        } else {
            finalOrigins = originsFromProps;
            log.info("Используем реальные origins из конфигурации: {}", finalOrigins);
        }

        configuration.setAllowedOrigins(finalOrigins);
        log.info("Установлено в CorsConfiguration: allowedOrigins = {}", configuration.getAllowedOrigins());

        // Методы
        List<String> methods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD");
        configuration.setAllowedMethods(methods);
        log.info("allowedMethods = {}", methods);

        // Заголовки
        configuration.setAllowedHeaders(List.of("*"));
        log.info("allowedHeaders = * (все заголовки разрешены)");

        // Credentials
        configuration.setAllowCredentials(true);
        log.info("allowCredentials = true");

        // Exposed headers (полезно для фронта)
        List<String> exposed = List.of("Authorization", "Content-Disposition", "Set-Cookie", "X-Requested-With");
        configuration.setExposedHeaders(exposed);
        log.info("exposedHeaders = {}", exposed);

        // Время кэширования preflight-ответа
        configuration.setMaxAge(3600L);
        log.info("maxAge = 3600 секунд (1 час)");

        // Создаём и регистрируем источник
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        log.info("CORS зарегистрирован для паттерна /**");

        log.info("=== CORS КОНФИГУРАЦИЯ УСПЕШНО СОЗДАНА ===");
        log.info("Итоговые allowed origins: {}", configuration.getAllowedOrigins());

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}