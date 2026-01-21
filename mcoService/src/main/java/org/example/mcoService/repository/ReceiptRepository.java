package org.example.mcoService.repository;

import org.example.mcoService.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    /**
     * Проверка существования чека по фискальным данным (для дедупликации)
     */
    boolean existsByFiscalSignAndFiscalDocumentNumberAndFiscalDriveNumber(
            Long fiscalSign,
            Long fiscalDocumentNumber,
            String fiscalDriveNumber
    );

    /**
     * Поиск чека по фискальным данным
     */
    Optional<Receipt> findByFiscalSignAndFiscalDocumentNumberAndFiscalDriveNumber(
            Long fiscalSign,
            Long fiscalDocumentNumber,
            String fiscalDriveNumber
    );

    /**
     * Получить все чеки пользователя
     */
    List<Receipt> findByUserIdOrderByReceiptDateTimeDesc(UUID userId);

    /**
     * Получить чеки пользователя за период
     */
    List<Receipt> findByUserIdAndReceiptDateTimeBetweenOrderByReceiptDateTimeDesc(
            UUID userId,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * Получить последний чек пользователя (для проверки есть ли новые)
     */
    @Query("SELECT r FROM Receipt r WHERE r.userId = :userId ORDER BY r.receiveDate DESC LIMIT 1")
    Optional<Receipt> findLatestByUserId(@Param("userId") UUID userId);

    /**
     * Подсчет чеков пользователя
     */
    long countByUserId(UUID userId);

    /**
     * Получить чеки по userIdentifier (телефон из MCO)
     */
    List<Receipt> findByUserIdentifierOrderByReceiptDateTimeDesc(String userIdentifier);
}