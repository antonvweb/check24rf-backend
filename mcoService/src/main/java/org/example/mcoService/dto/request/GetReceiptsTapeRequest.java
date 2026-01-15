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
        namespace = "urn://x-artefacts-gnivc-ru/ais3/smz/SmzIntegrationService/v0.1")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetReceiptsTapeRequest {

    @XmlElement(name = "Marker", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/smz/SmzIntegrationService/v0.1")
    private String marker;
}