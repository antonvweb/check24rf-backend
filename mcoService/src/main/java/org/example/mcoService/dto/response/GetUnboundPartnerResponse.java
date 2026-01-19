package org.example.mcoService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetUnboundPartnerResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetUnboundPartnerResponse {

    /**
     * Список отключившихся пользователей (до 1000 записей)
     */
    @JsonProperty("Unbounds")
    @XmlElement(name = "Unbounds", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<UnboundUser> unbounds;

    /**
     * Маркер для следующего запроса
     */
    @JsonProperty("NextMarker")
    @XmlElement(name = "NextMarker", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String nextMarker;

    /**
     * Есть ли еще данные для выгрузки
     * true - есть еще, нужно делать следующий запрос
     * false - это последняя порция
     */
    @JsonProperty("HasMore")
    @XmlElement(name = "HasMore", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private Boolean hasMore;

    /**
     * Информация об одном отключившемся пользователе
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UnboundUser {

        /**
         * ID заявки на подключение (оригинальной)
         */
        @JsonProperty("RequestId")
        @XmlElement(name = "RequestId", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String requestId;

        /**
         * Телефон пользователя
         */
        @JsonProperty("UserIdentifier")
        @XmlElement(name = "UserIdentifier", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String userIdentifier;

        /**
         * Время отключения
         */
        @JsonProperty("ResponseTime")
        @XmlElement(name = "ResponseTime", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String responseTime;
    }
}