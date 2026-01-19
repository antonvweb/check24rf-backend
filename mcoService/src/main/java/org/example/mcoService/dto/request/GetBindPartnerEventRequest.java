package org.example.mcoService.dto.request;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetBindPartnerEventRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetBindPartnerEventRequest {

    /**
     * Маркер для пагинации событий
     * "S_FROM_BEGINNING" - с самого начала (base64: UxFST01fQkVHSU5OSU5H)
     * "S_FROM_END" - с конца (base64: U19GUk9NX0VORA==)
     * или значение из предыдущего ответа
     */
    @XmlElement(name = "Marker", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String marker;
}