package org.example.mcoService.dto.request;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@XmlRootElement(name = "GetBindPartnerStatusRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetBindPartnerStatusRequest {

    @XmlElement(name = "RequestId", required = true)
    private List<String> requestIds;
}