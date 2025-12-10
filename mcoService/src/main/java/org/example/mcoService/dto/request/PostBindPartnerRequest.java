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

    @XmlElement(name = "UserIdentifier", required = true)
    private String UserIdentifier;

    @XmlElementWrapper(name = "PermissionGroups")
    @XmlElement(name = "PermissionGroup")
    private List<String> PermissionGroups;

    @XmlElement(name = "ExpiredAt")
    private LocalDateTime ExpiredAt;

    @XmlElement(name = "IsUnverifiedIdentifier")
    private Boolean IsUnverifiedIdentifier;

    @XmlElement(name = "RequireNoActiveRequests")
    private Boolean RequireNoActiveRequests;
}