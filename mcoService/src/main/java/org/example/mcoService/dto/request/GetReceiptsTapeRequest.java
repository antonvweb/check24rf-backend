package org.example.mcoService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.*;

/**
 * ✅ ИСПРАВЛЕНО: Используется правильный namespace из официальной Postman коллекции МЧО
 *
 * БЫЛО (неправильно):
 * namespace = "urn://x-artefacts-gnivc-ru/ais3/smz/SmzIntegrationService/v0.1"
 *
 * СТАЛО (правильно):
 * namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0"
 *
 * Парадокс: URL сервиса = /ais3/smz/SmzIntegrationService
 *           Но XML namespace = DrPartnersIntegrationService
 *           Это подтверждено официальным примером от МЧО!
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetReceiptsTapeRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetReceiptsTapeRequest {

    @XmlElement(name = "Marker", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String marker;
}