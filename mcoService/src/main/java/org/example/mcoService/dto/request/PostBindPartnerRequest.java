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

    @XmlElement(name = "RequestId", required = true)
    private String RequestId;

    @XmlElement(name = "UserIdentifier", required = true)
    private String UserIdentifier;

    @XmlElement(name = "PermissionGroups")
    private List<String> PermissionGroups;
}