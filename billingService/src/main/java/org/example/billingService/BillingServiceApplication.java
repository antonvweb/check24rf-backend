package org.example.billingService;

import org.example.billingService.controller.SubscriptionController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

@SpringBootApplication
public class BillingServiceApplication {
    public static void main(String[] args) {
        System.out.println("=== ЗАПУСК BILLING SERVICE ===");

        ConfigurableApplicationContext context = SpringApplication.run(BillingServiceApplication.class, args);

        // Проверяем, что контроллер зарегистрирован
        String[] controllerBeans = context.getBeanNamesForType(SubscriptionController.class);
        System.out.println("=== НАЙДЕННЫЕ КОНТРОЛЛЕРЫ ПОДПИСОК ===");
        for (String beanName : controllerBeans) {
            System.out.println("Контроллер найден: " + beanName);
        }

        if (controllerBeans.length == 0) {
            System.out.println("❌ КОНТРОЛЛЕР НЕ НАЙДЕН!");
        }

        // Проверяем все контроллеры
        String[] allControllers = context.getBeanNamesForAnnotation(RestController.class);
        System.out.println("=== ВСЕ REST КОНТРОЛЛЕРЫ ===");
        for (String controller : allControllers) {
            System.out.println("REST контроллер: " + controller);
        }

        // Проверяем маршруты
        RequestMappingHandlerMapping mapping = context.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
        System.out.println("=== ЗАРЕГИСТРИРОВАННЫЕ МАРШРУТЫ ===");
        map.forEach((info, method) -> {
            if (info.toString().contains("billing") || info.toString().contains("subscription")) {
                System.out.println("Маршрут: " + info + " -> " + method.getMethod().getName());
            }
        });

        System.out.println("=== BILLING SERVICE ЗАПУЩЕН ===");
    }
}
