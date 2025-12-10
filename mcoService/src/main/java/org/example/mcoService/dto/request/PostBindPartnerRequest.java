package org.example.mcoService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.xml.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "PostBindPartnerRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostBindPartnerRequest {

    @XmlElement(name = "requestId", required = true)
    private String requestId;

    @XmlElement(name = "userIdentifier", required = true)
    private String userIdentifier;

    @XmlElementWrapper(name = "permissionGroups")
    @XmlElement(name = "permissionGroup")
    private List<String> permissionGroups;

    @XmlElement(name = "expiredAt")
    private LocalDateTime expiredAt;

    @XmlElement(name = "isUnverifiedIdentifier")
    private Boolean isUnverifiedIdentifier;

    @XmlElement(name = "requireNoActiveRequests")
    private Boolean requireNoActiveRequests;
}