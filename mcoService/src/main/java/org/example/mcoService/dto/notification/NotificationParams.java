package org.example.mcoService.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO для передачи параметров уведомления.
 * Используется для заполнения шаблонов из NotificationType.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationParams {

    /**
     * Номер телефона пользователя (формат: 79991234567)
     */
    private String phoneNumber;

    /**
     * Переменные для подстановки в шаблоны.
     * Примеры:
     * - {"count": "3", "amount": "2450"} для NEW_RECEIPTS_AVAILABLE
     * - {"month": "январь", "count": "45", "amount": "28340", "average": "630"} для MONTHLY_STATISTICS
     */
    @Builder.Default
    private Map<String, String> templateVariables = new HashMap<>();

    /**
     * Внутренний ID элемента (купона/промокода) - необязательно.
     * Используется для CASHBACK уведомлений, но может быть полезен
     * для идентификации конкретных чеков.
     */
    private String externalItemId;

    /**
     * URL для перехода из уведомления (только HTTPS).
     * Может вести на страницу с деталями чеков или статистикой.
     */
    private String externalItemUrl;

    /**
     * Статический фабричный метод для быстрого создания параметров
     * с номером телефона и переменными.
     */
    public static NotificationParams of(String phoneNumber, Map<String, String> variables) {
        return NotificationParams.builder()
                .phoneNumber(phoneNumber)
                .templateVariables(variables != null ? variables : new HashMap<>())
                .build();
    }

    /**
     * Добавить переменную в шаблон.
     * Позволяет использовать fluent-style API.
     *
     * @param key   ключ переменной (например, "count")
     * @param value значение переменной (например, "5")
     * @return this для цепочки вызовов
     */
    public NotificationParams withVariable(String key, String value) {
        if (this.templateVariables == null) {
            this.templateVariables = new HashMap<>();
        }
        this.templateVariables.put(key, value);
        return this;
    }

    /**
     * Добавить переменную count (количество).
     */
    public NotificationParams withCount(int count) {
        return withVariable("count", String.valueOf(count));
    }

    /**
     * Добавить переменную amount (сумма).
     */
    public NotificationParams withAmount(String amount) {
        return withVariable("amount", amount);
    }

    /**
     * Добавить переменную month (месяц).
     */
    public NotificationParams withMonth(String month) {
        return withVariable("month", month);
    }

    /**
     * Добавить переменную average (средний чек).
     */
    public NotificationParams withAverage(String average) {
        return withVariable("average", average);
    }
}
