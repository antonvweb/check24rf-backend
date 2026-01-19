package org.example.mcoService.dto.request;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetUnboundPartnerRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetUnboundPartnerRequest {

    /**
     * Маркер для пагинации
     * "S_FROM_BEGINNING" - самые ранние данные
     * "S_FROM_END" - последние данные (самые свежие)
     * или значение NextMarker из предыдущего ответа
     */
    @XmlElement(name = "Marker", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String marker;
}