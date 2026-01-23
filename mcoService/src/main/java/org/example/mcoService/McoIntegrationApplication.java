package org.example.mcoService;

import org.example.mcoService.config.McoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(McoProperties.class)
public class McoIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(McoIntegrationApplication.class, args);
    }
}