package org.example.mcoService.dto.response;

import lombok.Data;
import jakarta.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "DrPlatformError",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class DrPlatformError {

    @XmlElement(name = "Code")
    private String code;

    @XmlElement(name = "Message")
    private String message;

    @XmlElementWrapper(name = "Args")
    @XmlElement(name = "Arg")
    private List<Arg> args;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Arg {
        @XmlElement(name = "Key")
        private String key;

        @XmlElement(name = "Value")
        private String value;
    }
}