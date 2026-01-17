package org.example.mcoService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ на запрос получения статуса заявки на подключение
 * Протокол 3.2.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetBindPartnerStatusResponse", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"statuses"})
public class GetBindPartnerStatusResponse {

    @JsonProperty("Statuses")
    @XmlElementWrapper(name = "Statuses", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    @XmlElement(name = "BindPartnerStatus", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<BindPartnerStatus> statuses;

    /**
     * Статус одной заявки
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"requestId", "result", "userIdentifier", "rejectionReasonMessage"})
    public static class BindPartnerStatus {

        /**
         * Идентификатор заявки
         */
        @JsonProperty("RequestId")
        @XmlElement(name = "RequestId", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String requestId;

        /**
         * Результат запроса:
         * - WAIT - ожидает обработки
         * - REQUEST_APPROVED - одобрена
         * - REQUEST_REJECTED - отклонена
         * - REQUEST_EXPIRED - истекла
         */
        @JsonProperty("Result")
        @XmlElement(name = "Result", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String result;

        /**
         * Основной номер телефона покупателя
         * Формат: 79998887766
         */
        @JsonProperty("UserIdentifier")
        @XmlElement(name = "UserIdentifier", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String userIdentifier;

        /**
         * Сообщение пользователю с причиной отказа
         * Присутствует только если result = REQUEST_REJECTED
         */
        @JsonProperty("RejectionReasonMessage")
        @XmlElement(name = "RejectionReasonMessage", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String rejectionReasonMessage;
    }
}