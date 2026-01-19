package org.example.mcoService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "PostNotificationResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostNotificationResponse {

    /**
     * Идентификатор рассылки
     */
    @JsonProperty("RequestId")
    @XmlElement(name = "RequestId", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String requestId;

    /**
     * Время обработки запроса
     * Формат: YYYY-MM-DDThh:mm:ss[.SSS]±hh:mm
     */
    @JsonProperty("HandledAt")
    @XmlElement(name = "HandledAt", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String handledAt;
}