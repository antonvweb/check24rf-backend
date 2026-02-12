package org.example.mcoService.dto.api;

import java.math.BigDecimal;

/**
 * Результат сохранения чеков.
 * Используется для передачи информации о сохраненных чеках
 * для последующей отправки уведомлений.
 *
 * @param count     количество сохраненных чеков
 * @param totalSum  общая сумма сохраненных чеков
 */
public record SaveReceiptsResult(int count, BigDecimal totalSum) {

    public static SaveReceiptsResult empty() {
        return new SaveReceiptsResult(0, BigDecimal.ZERO);
    }

    public boolean hasNewReceipts() {
        return count > 0;
    }

    public String getTotalSumFormatted() {
        return totalSum.setScale(2).toPlainString();
    }
}