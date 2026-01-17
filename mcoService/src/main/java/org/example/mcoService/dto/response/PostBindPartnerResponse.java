package org.example.mcoService.dto.response;

import lombok.Data;
import jakarta.xml.bind.annotation.*;
import java.time.LocalDateTime;

@Data
@XmlRootElement(name = "PostBindPartnerResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostBindPartnerResponse {

    @XmlElement(name = "RequestId",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String requestId;  // Эхо из запроса, для идентификации заявки

    @XmlElement(name = "MessageId",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String messageId;  // Идентификатор для опроса статуса через GetBindPartnerEventRequest

    @XmlElement(name = "BindDate",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private LocalDateTime bindDate;  // Опционально: дата отправки заявки (если API возвращает)
}