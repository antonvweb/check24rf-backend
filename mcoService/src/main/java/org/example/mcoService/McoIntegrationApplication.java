package org.example.mcoService;

import org.example.mcoService.config.McoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "org.example.common",
        "org.example.mcoService"
})
@EntityScan(basePackages = {
        "org.example.common.entity"        // Entity из common модуля
})
@EnableJpaRepositories(basePackages = "org.example.common.repository")  // ← это важно!
@EnableAsync
@EnableConfigurationProperties(McoProperties.class)
public class McoIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(McoIntegrationApplication.class, args);
    }
}