package org.example.mcoService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "PostPlatformRegistrationRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/kkt/DrPartnersIntegrationService/v0.1")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostPlatformRegistrationRequest {

    @XmlElement(name = "Name", required = true)
    private String name;

    @XmlElement(name = "Type", required = true)
    private String type; // PARTNER

    @XmlElement(name = "Description")
    private String description;

    @XmlElement(name = "TransitionLink")
    private String transitionLink;

    @XmlElement(name = "Text")
    private String text;

    @XmlElement(name = "Image", required = true)
    private byte[] image; // Base64 encoded image

    @XmlElementWrapper(name = "INNs")
    @XmlElement(name = "INN")
    private List<String> inns;

    @XmlElement(name = "Phone")
    private String phone;

    @XmlElement(name = "ImageFullscreen")
    private byte[] imageFullscreen;

    @XmlElement(name = "Hidden")
    private Boolean hidden;
}