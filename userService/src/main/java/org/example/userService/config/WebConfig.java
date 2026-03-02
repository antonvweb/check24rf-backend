package org.example.userService.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Полностью отключаем CORS в Spring — им управляет nginx
        registry.addMapping("/**")
                .allowedOrigins(new String[0])
                .allowedMethods(new String[0])
                .allowedHeaders(new String[0]);
    }
}
