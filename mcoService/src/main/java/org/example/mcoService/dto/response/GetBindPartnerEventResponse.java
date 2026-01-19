package org.example.mcoService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetBindPartnerEventResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetBindPartnerEventResponse {

    @JsonProperty("Events")
    @XmlElement(name = "Events", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<BindPartnerEvent> events;

    @JsonProperty("Marker")
    @XmlElement(name = "Marker", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String marker;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BindPartnerEvent {

        @JsonProperty("RequestId")
        @XmlElement(name = "RequestId", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String requestId;

        @JsonProperty("Result")
        @XmlElement(name = "Result", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String result;

        @JsonProperty("UserIdentifier")
        @XmlElement(name = "UserIdentifier", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String userIdentifier;

        @JsonProperty("ResponseTime")
        @XmlElement(name = "ResponseTime", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String responseTime;
    }
}