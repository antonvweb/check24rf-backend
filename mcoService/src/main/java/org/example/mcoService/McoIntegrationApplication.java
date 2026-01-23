package org.example.mcoService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "org.example.common"
})
@EnableJpaRepositories(basePackages = "org.example.common.repository")  // ← это важно!
@EnableAsync
public class McoIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(McoIntegrationApplication.class, args);
    }
}