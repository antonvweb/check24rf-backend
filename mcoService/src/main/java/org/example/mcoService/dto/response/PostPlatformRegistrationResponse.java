package org.example.mcoService.dto.response;

import lombok.Data;
import jakarta.xml.bind.annotation.*;
import java.time.LocalDateTime;

@Data
@XmlRootElement(name = "PostPlatformRegistrationResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostPlatformRegistrationResponse {

    @XmlElement(name = "Id",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String id;

    @XmlElement(name = "RegistrationDate",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private LocalDateTime registrationDate;
}