package org.example.mcoService.repository;

import org.example.mcoService.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
     * Получить все чеки пользователя
     */
    Page<Receipt> findByUserIdOrderByReceiptDateTimeDesc(UUID userId, Pageable pageable);

    /**
     * Удалить все чеки пользователя по userId
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Receipt r WHERE r.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    /**
     * Удалить все чеки пользователя по userIdentifier (номер телефона)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Receipt r WHERE r.userIdentifier = :userIdentifier")
    int deleteByUserIdentifier(@Param("userIdentifier") String userIdentifier);

    /**
     * Получить количество чеков пользователя по userId
     */
    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);

    /**
     * Получить количество чеков по userIdentifier
     */
    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.userIdentifier = :userIdentifier")
    long countByUserIdentifier(@Param("userIdentifier") String userIdentifier);

    /**
     * Пакетное удаление чеков по userId (native query для оптимизации)
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM receipts WHERE user_id = :userId " +
            "AND id IN (SELECT id FROM receipts WHERE user_id = :userId " +
            "ORDER BY created_at LIMIT :limit)",
            nativeQuery = true)
    int deleteBatchByUserId(@Param("userId") UUID userId, @Param("limit") int limit);

    /**
     * Пакетное удаление чеков по userIdentifier (native query для оптимизации)
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM receipts WHERE user_identifier = :userIdentifier " +
            "AND id IN (SELECT id FROM receipts WHERE user_identifier = :userIdentifier " +
            "ORDER BY created_at LIMIT :limit)",
            nativeQuery = true)
    int deleteBatchByUserIdentifier(@Param("userIdentifier") String userIdentifier, @Param("limit") int limit);
}