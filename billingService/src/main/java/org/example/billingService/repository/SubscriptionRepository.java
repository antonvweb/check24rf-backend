package org.example.billingService.repository;

import org.springframework.data.repository.query.Param; // ✅ ПРАВИЛЬНЫЙ импорт
import org.example.billingService.entity.SubscriptionData;
import org.example.billingService.entity.SubscriptionStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionData, Long> {

    Optional<SubscriptionData> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    List<SubscriptionData> findByUserId(UUID userId);

    @Query("SELECT s FROM SubscriptionData s WHERE s.userId = :userId AND s.status = :status AND s.endDate > :currentDate")
    Optional<SubscriptionData> findActiveSubscriptionByUserId(@Param("userId") UUID userId,
                                                              @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT s FROM SubscriptionData s WHERE s.status = :status AND s.endDate < :currentDate")
    List<SubscriptionData> findExpiredSubscriptions(@Param("status") SubscriptionStatus status,
                                                    @Param("currentDate") LocalDateTime currentDate);
}