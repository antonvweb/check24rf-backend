package org.example.billingService.service;

import jakarta.transaction.Transactional;
import org.example.billingService.dto.CreateSubscriptionRequest;
import org.example.billingService.dto.SubscriptionResponse;
import org.example.billingService.entity.SubscriptionData;
import org.example.billingService.entity.SubscriptionStatus;
import org.example.billingService.entity.SubscriptionType;
import org.example.billingService.repository.SubscriptionRepository;
import org.example.userService.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentService paymentService;

    /**
     * Создание новой подписки (имитация оплаты)
     */
    public SubscriptionResponse createSubscription(UUID userId, CreateSubscriptionRequest request) {
        // Проверяем, есть ли уже активная подписка
        Optional<SubscriptionData> existingSubscription = getActiveSubscription(userId);
        if (existingSubscription.isPresent()) {
            throw new RuntimeException("У пользователя уже есть активная подписка");
        }

        SubscriptionType type = request.getSubscriptionType();

        // Имитируем процесс оплаты
        boolean paymentSuccess = paymentService.processPayment(userId, type.getPriceInCents());

        if (!paymentSuccess) {
            throw new RuntimeException("Ошибка оплаты");
        }

        // Создаем подписку
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusMonths(type.getMonths());

        SubscriptionData SubscriptionData = new SubscriptionData(
                userId,
                SubscriptionStatus.ACTIVE,
                startDate,
                endDate,
                type.getPriceInCents()
        );

        SubscriptionData = subscriptionRepository.save(SubscriptionData);

        logger.info("Создана новая подписка для пользователя {}: {} месяцев", userId, type.getMonths());

        return mapToResponse(SubscriptionData);
    }
    /**
     * Получение активной подписки пользователя
     */
    public Optional<SubscriptionData> getActiveSubscription(UUID userId) {
        return subscriptionRepository.findActiveSubscriptionByUserId(userId, LocalDateTime.now());
    }

    /**
     * Получение информации о подписке пользователя
     */
    public Optional<SubscriptionResponse> getUserSubscription(UUID userId) {
        Optional<SubscriptionData> SubscriptionData = getActiveSubscription(userId);
        return SubscriptionData.map(this::mapToResponse);
    }

    /**
     * Получение всех подписок пользователя
     */
    public List<SubscriptionResponse> getAllUserSubscriptions(UUID userId) {
        List<SubscriptionData> subscriptions = subscriptionRepository.findByUserId(userId);
        return subscriptions.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Проверка, есть ли у пользователя активная подписка
     */
    public boolean hasActiveSubscription(UUID userId) {
        return getActiveSubscription(userId).isPresent();
    }

    /**
     * Отмена подписки
     */
    public void cancelSubscription(UUID userId) {
        Optional<SubscriptionData> SubscriptionData = getActiveSubscription(userId);
        if (SubscriptionData.isPresent()) {
            SubscriptionData sub = SubscriptionData.get();
            sub.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(sub);
            logger.info("Подписка отменена для пользователя {}", userId);
        } else {
            throw new RuntimeException("У пользователя нет активной подписки");
        }
    }

    /**
     * Продление подписки
     */
    public SubscriptionResponse extendSubscription(UUID userId, CreateSubscriptionRequest request) {
        Optional<SubscriptionData> existingSubscription = getActiveSubscription(userId);

        if (existingSubscription.isEmpty()) {
            // Если нет активной подписки, создаем новую
            return createSubscription(userId, request);
        }

        SubscriptionType type = request.getSubscriptionType();

        // Имитируем оплату
        boolean paymentSuccess = paymentService.processPayment(userId, type.getPriceInCents());
        if (!paymentSuccess) {
            throw new RuntimeException("Ошибка оплаты");
        }

        // Продлеваем существующую подписку
        SubscriptionData SubscriptionData = existingSubscription.get();
        SubscriptionData.setEndDate(SubscriptionData.getEndDate().plusMonths(type.getMonths()));
        SubscriptionData = subscriptionRepository.save(SubscriptionData);

        logger.info("Подписка продлена для пользователя {} на {} месяцев", userId, type.getMonths());

        return mapToResponse(SubscriptionData);
    }

    /**
     * Автоматическое обновление истекших подписок (выполняется каждый час)
     */
    @Scheduled(fixedRate = 3600000) // каждый час
    public void updateExpiredSubscriptions() {
        List<SubscriptionData> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(LocalDateTime.now());

        for (SubscriptionData SubscriptionData : expiredSubscriptions) {
            SubscriptionData.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(SubscriptionData);
            logger.info("Подписка {} помечена как истекшая", SubscriptionData.getId());
        }

        if (!expiredSubscriptions.isEmpty()) {
            logger.info("Обновлено {} истекших подписок", expiredSubscriptions.size());
        }
    }

    /**
     * Преобразование SubscriptionData в SubscriptionResponse
     */
    private SubscriptionResponse mapToResponse(SubscriptionData SubscriptionData) {
        long daysLeft = 0;
        boolean isActive = SubscriptionData.isActive();

        if (isActive) {
            daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), SubscriptionData.getEndDate());
        }

        return new SubscriptionResponse(
                SubscriptionData.getId(),
                SubscriptionData.getStatus(),
                SubscriptionData.getStartDate(),
                SubscriptionData.getEndDate(),
                SubscriptionData.getPricePaid() / 100.0, // конвертируем в рубли
                isActive,
                daysLeft
        );
    }
}
