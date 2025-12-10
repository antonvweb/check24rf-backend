package org.example.mcoService.dto.request;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.xml.bind.annotation.*;
import org.example.mcoService.dto.LocalDateTimeAdapter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "PostBindPartnerRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
public class PostBindPartnerRequest {

    @XmlElement(name = "RequestId", required = true)
    private String requestId;

    @XmlElement(name = "UserIdentifier", required = true)
    private String userIdentifier;

    @XmlElementWrapper(name = "PermissionGroups")
    @XmlElement(name = "PermissionGroup")
    private List<String> permissionGroups;

    @XmlElement(name = "ExpiredAt")
    private LocalDateTime expiredAt;

    @XmlElement(name = "IsUnverifiedIdentifier")
    private Boolean isUnverifiedIdentifier;

    @XmlElement(name = "RequireNoActiveRequests")
    private Boolean requireNoActiveRequests;
}