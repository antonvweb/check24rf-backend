package org.example.mcoService.dto.response;

import lombok.Data;
import jakarta.xml.bind.annotation.*;
import java.time.LocalDateTime;

@Data
@XmlRootElement(name = "PostPlatformRegistrationResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/kkt/DrPartnersIntegrationService/v0.1")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostPlatformRegistrationResponse {

    @XmlElement(name = "Id")
    private String id;

    @XmlElement(name = "RegistrationDate")
    private LocalDateTime registrationDate;

    public String getId() {
        return id;
    }
}