package org.example.mcoService.config;

import org.example.common.utils.CorsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig {

    @Autowired
    private CorsProperties corsProperties;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }

    /**
     * Пул потоков специально для polling-задач MCO (проверка статусов заявок)
     * Можно использовать в BindApprovalPollingService через @Autowired TaskExecutor
     */
    @Bean(name = "mcoPollingExecutor")
    public TaskExecutor mcoPollingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);           // базовое количество потоков
        executor.setMaxPoolSize(12);           // максимум потоков при переполнении очереди
        executor.setQueueCapacity(80);         // очередь задач перед созданием новых потоков
        executor.setThreadNamePrefix("mco-poll-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30); // даём время на завершение задач при остановке приложения
        executor.initialize();
        return executor;
    }
}