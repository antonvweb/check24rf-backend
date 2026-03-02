package org.example.authService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "org.example.common",
        "org.example.authService"
})
@EntityScan(basePackages = {
        "org.example.common.entity",
        "org.example.authService.entity"
})
@EnableJpaRepositories(basePackages = {
        "org.example.common.repository",
        "org.example.authService.repository"
})
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
