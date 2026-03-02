package org.example.mcoService.config;

import org.example.common.utils.CorsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Глобальная CORS конфигурация для всех запросов.
 */
@Configuration
public class WebConfig {

    @Autowired
    private CorsProperties corsProperties;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Разрешаем запросы с указанных доменов
        config.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());

        // Разрешаем все методы
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Разрешаем все заголовки
        config.setAllowedHeaders(List.of("*"));

        // Разрешаем отправку учетных данных
        config.setAllowCredentials(true);

        // Кэшируем preflight запросы на 1 час
        config.setMaxAge(3600L);

        // Применяем ко всем путям
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
