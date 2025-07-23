package org.example.config;

import org.example.utils.CorsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Autowired
    private CorsProperties corsProperties;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://172.19.0.1:3000",
                                "https://www.xn--24-mlcu7d.xn--p1ai",
                                "https://чек24.рф",
                                "https://www.чек24.рф",
                                "https://api.чек24.рф",
                                "https://xn--24-mlcu7d.xn--p1ai",
                                "https://api.xn--24-mlcu7d.xn--p1ai"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600); // кэш preflight-запросов на 1 час
            }
        };
    }
}
