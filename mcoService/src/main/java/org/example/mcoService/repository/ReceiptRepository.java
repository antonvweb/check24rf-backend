package org.example.mcoService.repository;

import org.example.mcoService.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}