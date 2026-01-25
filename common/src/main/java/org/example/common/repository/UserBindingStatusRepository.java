package org.example.common.repository;

import org.example.common.entity.UserBindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// UserBindingStatusRepository.java - добавить этот метод
@Repository
public interface UserBindingStatusRepository extends JpaRepository<UserBindingStatus, UUID> {

    Optional<UserBindingStatus> findByPhoneNumber(String phoneNumber);

    // Добавляем новый метод для создания или обновления статуса
    @Modifying
    @Query("UPDATE UserBindingStatus ubs SET ubs.bindingStatus = :status, ubs.boundAt = :boundAt, ubs.partnerConnected = true WHERE ubs.phoneNumber = :phoneNumber")
    void updateBindingStatus(@Param("phoneNumber") String phoneNumber,
                             @Param("status") UserBindingStatus.BindingStatus status,
                             @Param("boundAt") LocalDateTime boundAt);

    // Или альтернативно - метод для создания записи, если её нет
    @Transactional
    default UserBindingStatus createOrUpdateBindingStatus(String phoneNumber, String requestId,
                                                          UserBindingStatus.BindingStatus status) {
        Optional<UserBindingStatus> existing = findByPhoneNumber(phoneNumber);

        if (existing.isPresent()) {
            UserBindingStatus entity = existing.get();
            entity.setBindingStatus(status);
            entity.setRequestId(requestId);
            entity.setPartnerConnected(true);
            entity.setReceiptsEnabled(true);
            entity.setNotificationsEnabled(true);
            entity.setBoundAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return save(entity);
        } else {
            UserBindingStatus entity = UserBindingStatus.builder()
                    .phoneNumber(phoneNumber)
                    .requestId(requestId)
                    .bindingStatus(status)
                    .partnerConnected(true)
                    .receiptsEnabled(true)
                    .notificationsEnabled(true)
                    .boundAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .bound(true)
                    .build();
            return save(entity);
        }
    }
}