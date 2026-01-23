package org.example.mcoService.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа о статусе заявки на подключение пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BindRequestStatusDto {

    /**
     * Идентификатор заявки
     */
    private String requestId;

    /**
     * Статус заявки:
     * - IN_PROGRESS - ожидает обработки пользователем
     * - REQUEST_APPROVED - одобрена пользователем
     * - REQUEST_REJECTED - отклонена пользователем
     * - REQUEST_EXPIRED - заявка истекла
     */
    private String status;

    /**
     * Человекочитаемое описание статуса
     */
    private String statusDescription;

    /**
     * Номер телефона пользователя
     */
    private String userIdentifier;

    /**
     * Группы разрешений
     */
    private String permissionGroups;

    /**
     * Время ответа пользователя (ISO 8601)
     */
    private String responseTime;

    /**
     * Причина отказа (только для статуса REQUEST_REJECTED)
     */
    private String rejectionReason;

    /**
     * Создать DTO из ответа MCO API
     */
    public static BindRequestStatusDto fromMcoResponse(
            org.example.mcoService.dto.response.GetBindPartnerStatusResponse.BindPartnerStatus status) {

        BindRequestStatusDto dto = BindRequestStatusDto.builder()
                .requestId(status.getRequestId())
                .status(status.getResult())
                .userIdentifier(status.getUserIdentifier())
                .permissionGroups(status.getPermissionGroups())
                .responseTime(status.getResponseTime())
                .rejectionReason(status.getRejectionReasonMessage())
                .build();

        // Добавляем человекочитаемое описание
        dto.setStatusDescription(getStatusDescription(status.getResult()));

        return dto;
    }

    /**
     * Получить человекочитаемое описание статуса
     */
    private static String getStatusDescription(String status) {
        if (status == null) return null;

        return switch (status) {
            case "IN_PROGRESS" -> "Ожидает одобрения пользователем в ЛК МЧО";
            case "REQUEST_APPROVED" -> "Одобрена - пользователь подключен к партнеру";
            case "REQUEST_DECLINED" -> "Отклонена пользователем";
            case "REQUEST_CANCELLED_AS_DUPLICATE" -> "Заявка отклонена по причине создания новой заявки";
            case "REQUEST_EXPIRED" -> "Заявка устарела";
            default -> "Неизвестный статус: " + status;
        };
    }
}