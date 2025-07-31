package org.example.billingService.controller;

import org.springframework.security.core.Authentication;
import org.example.billingService.dto.CreateSubscriptionRequest;
import org.example.billingService.dto.SubscriptionResponse;
import org.example.billingService.entity.SubscriptionType;
import org.example.billingService.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/subscription")
public class SubscriptionController extends BillingController {

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Получение доступных типов подписки с ценами
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getSubscriptionTypes() {
        SubscriptionType[] types = SubscriptionType.values();
        return ResponseEntity.ok(Map.of(
                "subscriptionTypes", types,
                "message", "Доступные типы подписки"
        ));
    }

    /**
     * Создание новой подписки
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSubscription(
            @RequestBody CreateSubscriptionRequest request,
            Authentication authentication) {

        try {
            UUID userId = getUserIdFromAuth(authentication);
            SubscriptionResponse subscription = subscriptionService.createSubscription(userId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subscription", subscription,
                    "message", "Подписка успешно создана"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Получение текущей подписки пользователя
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentSubscription(Authentication authentication) {
        UUID userId = getUserIdFromAuth(authentication);
        Optional<SubscriptionResponse> subscription = subscriptionService.getUserSubscription(userId);

        return subscription.map(subscriptionResponse -> ResponseEntity.ok(Map.of(
                "hasSubscription", true,
                "subscription", subscriptionResponse
        ))).orElseGet(() -> ResponseEntity.ok(Map.of(
                "hasSubscription", false,
                "message", "У вас нет активной подписки"
        )));
    }

    /**
     * Получение всех подписок пользователя
     */
    @GetMapping("/history")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionHistory(Authentication authentication) {
        UUID userId = getUserIdFromAuth(authentication);
        List<SubscriptionResponse> subscriptions = subscriptionService.getAllUserSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Проверка активности подписки
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkSubscriptionStatus(Authentication authentication) {
        UUID userId = getUserIdFromAuth(authentication);
        boolean hasActive = subscriptionService.hasActiveSubscription(userId);

        return ResponseEntity.ok(Map.of(
                "hasActiveSubscription", hasActive,
                "userId", userId
        ));
    }

    /**
     * Продление подписки
     */
    @PostMapping("/extend")
    public ResponseEntity<Map<String, Object>> extendSubscription(
            @RequestBody CreateSubscriptionRequest request,
            Authentication authentication) {

        try {
            UUID userId = getUserIdFromAuth(authentication);
            SubscriptionResponse subscription = subscriptionService.extendSubscription(userId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subscription", subscription,
                    "message", "Подписка успешно продлена"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Отмена подписки
     */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelSubscription(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuth(authentication);
            subscriptionService.cancelSubscription(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Подписка успешно отменена"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Извлечение ID пользователя из Authentication
     */
    private UUID getUserIdFromAuth(Authentication authentication) {
        // Адаптируйте под вашу систему аутентификации
        // Например, если в Principal хранится UUID:
        return UUID.fromString(authentication.getName());

        // Или если используете кастомный UserDetails:
        // UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // return ((CustomUserDetails) userDetails).getUserId();
    }

}
