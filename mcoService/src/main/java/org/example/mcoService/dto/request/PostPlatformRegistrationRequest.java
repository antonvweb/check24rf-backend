package org.example.mcoService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "PostPlatformRegistrationRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostPlatformRegistrationRequest {

    @XmlElement(name = "Name", required = true)
    private String name;

    @XmlElement(name = "Type", required = true)
    private String type;

    @XmlElement(name = "Description")
    private String description;

    @XmlElement(name = "TransitionLink")
    private String transitionLink;

    @XmlElement(name = "Text")
    private String text;

    @XmlElement(name = "Image")
    private String image;

    @XmlElement(name = "ImageFullScreen")  // ← С большой буквы!
    private String imageFullScreen;

    @XmlElement(name = "Inn")  // ← Единственное число, НЕ INNs!
    private String inn;

    @XmlElement(name = "Phone")
    private String phone;
}