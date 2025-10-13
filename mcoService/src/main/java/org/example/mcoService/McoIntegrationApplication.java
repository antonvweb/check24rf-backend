package org.example.mcoService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class McoIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(McoIntegrationApplication.class, args);
    }
}