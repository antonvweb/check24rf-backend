package org.example.mcoService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetReceiptsTapeRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/kkt/DrPartnersIntegrationService/v0.1")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetReceiptsTapeRequest {

    @XmlElement(name = "Marker", required = true)
    private String marker; // S_FROM_END или S_FROM_BEGINNING для первого запроса
}