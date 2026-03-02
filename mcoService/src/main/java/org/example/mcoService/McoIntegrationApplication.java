package org.example.mcoService;

import org.example.mcoService.config.McoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "org.example.common",
        "org.example.mcoService"
})
@EntityScan(basePackages = {
        "org.example.common.entity",
        "org.example.mcoService.entity"
})
@EnableJpaRepositories(basePackages = {"org.example.common.repository", "org.example.mcoService.repository"})  // ← это важно!
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(McoProperties.class)
public class McoIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(McoIntegrationApplication.class, args);
    }
}