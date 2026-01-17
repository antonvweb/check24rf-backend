package org.example.mcoService.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа при создании заявки на подключение пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateBindRequestDto {

    /**
     * Идентификатор заявки (для отслеживания статуса)
     */
    private String requestId;

    /**
     * Номер телефона пользователя
     */
    private String userIdentifier;

    /**
     * Группы разрешений
     */
    private String permissionGroups;

    /**
     * URL для проверки статуса заявки
     */
    private String statusCheckUrl;

    /**
     * Инструкция для пользователя
     */
    private String userInstruction;
}