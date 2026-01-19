package org.example.mcoService.dto.request;

import jakarta.xml.bind.annotation.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "PostBindPartnerBatchRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostBindPartnerBatchRequest {

    @XmlElement(name = "RequestId", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String requestId;

    @XmlElement(name = "UserIdentifiers", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<String> userIdentifiers;

    @XmlElement(name = "PermissionGroups",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<String> permissionGroups;

    @XmlElement(name = "IsUnverifiedIdentifier",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private Boolean isUnverifiedIdentifier;

    @XmlElement(name = "RequireNoActiveRequests",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private Boolean requireNoActiveRequests;
}