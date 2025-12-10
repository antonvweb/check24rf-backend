package org.example.mcoService.dto.response;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Data;
import jakarta.xml.bind.annotation.*;
import org.example.mcoService.dto.LocalDateTimeAdapter;

import java.time.LocalDateTime;

@Data
@XmlRootElement(name = "PostPlatformRegistrationResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
public class PostPlatformRegistrationResponse {

    @XmlElement(name = "Id")
    private String id;

    @XmlElement(name = "RegistrationDate")
    private LocalDateTime registrationDate;
}