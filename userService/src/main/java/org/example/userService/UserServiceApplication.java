package org.example.userService;

import org.example.common.utils.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "org.example.userService",
        "org.example.common"  // Сканируем пакеты из common модуля
})
@EntityScan(basePackages = {
        "org.example.userService.entity",  // Локальные entity
        "org.example.common.entity"        // Entity из common модуля
})
@EnableJpaRepositories(basePackages = {
        "org.example.userService.repository", // Локальные repository
        "org.example.common.repository"       // Repository из common модуля
})
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
