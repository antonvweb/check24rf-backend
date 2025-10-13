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
        namespace = "urn://x-artefacts-gnivc-ru/ais3/kkt/DrPartnersIntegrationService/v0.1")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostBindPartnerRequest {

    @XmlElement(name = "RequestId", required = true)
    private String requestId;

    @XmlElement(name = "UserIdentifier", required = true)
    private String userIdentifier; // Номер телефона в формате 79998887766

    @XmlElementWrapper(name = "PermissionGroups")
    @XmlElement(name = "PermissionGroup")
    private List<String> permissionGroups; // DEFAULT

    @XmlElement(name = "ExpiredAt")
    private LocalDateTime expiredAt;

    @XmlElement(name = "IsUnverifiedIdentifier")
    private Boolean isUnverifiedIdentifier;

    @XmlElement(name = "RequireNoActiveRequests")
    private Boolean requireNoActiveRequests;
}